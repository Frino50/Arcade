package perso.arcade.service;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import perso.arcade.exception.SpriteNameAlreadyExist;
import perso.arcade.model.dto.ModifSpriteDto;
import perso.arcade.model.dto.SpriteInfos;
import perso.arcade.model.entities.Animation;
import perso.arcade.model.entities.Sprite;
import perso.arcade.model.enumeration.AnimationType;
import perso.arcade.repository.SpriteRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class SpriteService {

    private static final Logger log = LoggerFactory.getLogger(SpriteService.class);

    private static final List<AnimationType> ANIMATION_TYPES = List.of(
            AnimationType.IDLE,
            AnimationType.WALK,
            AnimationType.ATTACK
    );

    private final Path staticStorageRoot;
    private final SpriteRepository spriteRepository;

    public SpriteService(
            SpriteRepository spriteRepository,
            @Value("${sprite.storage.root}") String storageRoot) {
        this.spriteRepository = spriteRepository;
        this.staticStorageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        initStorage();
    }

    private void initStorage() {
        try {
            if (!Files.exists(staticStorageRoot)) {
                Files.createDirectories(staticStorageRoot);
                log.info("Répertoire de stockage créé : {}", staticStorageRoot);
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'initialiser le stockage des sprites", e);
        }
    }

    /**
     * Traite un fichier ZIP uploadé.
     */
    @Transactional
    public SpriteInfos processSpriteZip(MultipartFile zipFile) {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new IllegalArgumentException("Le fichier ZIP est vide.");
        }

        Path tempDir = null;

        try {
            // 1. Dézippage temporaire
            tempDir = unzipToTempDirectory(zipFile);

            File tempSpriteRoot = findSpriteRoot(tempDir);

            String spriteName = tempSpriteRoot.getName();
            if (spriteRepository.findByName(spriteName).isPresent()) {
                throw new SpriteNameAlreadyExist("Un sprite avec le nom '" + spriteName + "' existe déjà.");
            }

            Sprite sprite = new Sprite(spriteName);

            // 2. Analyse des images (Frames, Dimensions) et Rognage Vertical Global
            processAnimationsMetaData(tempSpriteRoot, sprite);

            // 3. Stockage définitif et propre (utilise les images temporaires rognées)
            storeSpriteFilesCleanly(tempSpriteRoot, spriteName);
            sprite.setScale(1);
            // 4. Sauvegarde DB
            log.info("Sprite '{}' importé avec succès.", spriteName);
            spriteRepository.save(sprite);

            return spriteRepository.getSpritesInfosById(AnimationType.IDLE, sprite.getId());
        } catch (SpriteNameAlreadyExist e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors du traitement du ZIP", e);
            throw new RuntimeException("Erreur import sprite : " + e.getMessage(), e);
        } finally {
            cleanupTempDirectory(tempDir);
        }
    }

    /**
     * Calcule le cadre englobant (bounding box) des pixels non transparents.
     *
     * @return Un tableau d'entiers [xMin, yMin, xMax, yMax].
     */
    private int[] calculateBoundingBox(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        int xMin = width, yMin = height;
        int xMax = 0, yMax = 0;
        final int ALPHA_THRESHOLD = 10;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xff;
                if (alpha > ALPHA_THRESHOLD) {
                    xMin = Math.min(xMin, x);
                    yMin = Math.min(yMin, y);
                    xMax = Math.max(xMax, x);
                    yMax = Math.max(yMax, y);
                }
            }
        }

        if (xMin > xMax || yMin > yMax) {
            return null;
        }
        return new int[]{xMin, yMin, xMax, yMax};
    }

    /**
     * Calcule la limite verticale la plus extrême (YMin et YMax) pour toutes les images d'une animation.
     *
     * @return Un tableau d'entiers [YMin global, YMax global, Largeur de l'image].
     */
    private int[] calculateGlobalVerticalBoundingBox(File[] images) {
        int globalYMin = Integer.MAX_VALUE;
        int globalYMax = Integer.MIN_VALUE;
        int width = 0;
        boolean foundPixels = false;

        for (File imgFile : images) {
            try {
                BufferedImage img = ImageIO.read(imgFile);
                if (img == null) continue;

                if (width == 0) {
                    width = img.getWidth();
                }

                int[] localBox = calculateBoundingBox(img);

                if (localBox != null) {
                    globalYMin = Math.min(globalYMin, localBox[1]);
                    globalYMax = Math.max(globalYMax, localBox[3]);
                    foundPixels = true;
                }
            } catch (IOException e) {
                log.error("Erreur lecture image pour global box verticale : {}", imgFile.getName(), e);
            }
        }

        if (!foundPixels) return null;

        // [YMin global, YMax global, Largeur originale]
        return new int[]{globalYMin, globalYMax, width};
    }


    private BufferedImage cropImage(BufferedImage img, int x, int y, int w, int h) {
        if (x < 0 || y < 0 || w <= 0 || h <= 0 || x + w > img.getWidth() || y + h > img.getHeight()) {
            log.warn("Tentative de rognage invalide. Retour de l'image originale.");
            return img;
        }
        return img.getSubimage(x, y, w, h);
    }

    /**
     * Applique le rognage vertical (coupe le vide au-dessus de YMin et en dessous de YMax)
     * et remplace les fichiers temporaires.
     */
    private void cropAndReplaceAnimationFramesVertical(File[] images, int[] globalVerticalBox) {
        // globalVerticalBox contient [YMin, YMax, Largeur]
        int yStart = globalVerticalBox[0]; // Point de départ en haut (coupe le vide au-dessus)
        int yEnd = globalVerticalBox[1];   // Point de fin en bas (coupe le vide en dessous)

        // La hauteur à conserver, incluant les pixels YMin et YMax
        int heightCropped = yEnd - yStart + 1;

        int widthOriginal = globalVerticalBox[2]; // Largeur inchangée
        int xStart = 0; // Point de départ horizontal inchangé

        for (File file : images) {
            try {
                BufferedImage originalImg = ImageIO.read(file);
                if (originalImg == null) continue;

                // Rogner en utilisant la largeur originale, mais la nouvelle hauteur calculée.
                BufferedImage croppedImg = cropImage(originalImg, xStart, yStart, widthOriginal, heightCropped);

                // Écraser le fichier temporaire avec l'image rognée
                ImageIO.write(croppedImg, "png", file);
                log.debug("Image rognée verticalement : {} (Nouvelle hauteur : {})", file.getName(), heightCropped);
            } catch (IOException e) {
                log.error("Erreur de rognage verticale pour le fichier temporaire : {}", file.getName(), e);
            }
        }
    }
    // --- GESTION FICHIERS ---

    private Path unzipToTempDirectory(MultipartFile zipFile) throws IOException {
        Path destDir = Files.createTempDirectory("sprite_upload_");
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Protection contre le Zip Slip vulnerability
                Path newPath = destDir.resolve(entry.getName()).normalize();
                if (!newPath.startsWith(destDir)) {
                    throw new IOException("Entrée ZIP invalide : " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        return destDir;
    }

    private void storeSpriteFilesCleanly(File tempSpriteRoot, String spriteName) throws IOException {
        Path targetSpriteDir = staticStorageRoot.resolve(spriteName);

        if (Files.exists(targetSpriteDir)) {
            FileSystemUtils.deleteRecursively(targetSpriteDir);
        }
        Files.createDirectories(targetSpriteDir);

        for (AnimationType type : ANIMATION_TYPES) {
            File sourceAnimDir = new File(tempSpriteRoot, type.name());

            if (sourceAnimDir.exists() && sourceAnimDir.isDirectory()) {
                Path targetAnimDir = targetSpriteDir.resolve(type.name());
                Files.createDirectories(targetAnimDir);

                File[] images = sourceAnimDir.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".png"));

                if (images != null && images.length > 0) {
                    Arrays.sort(images, Comparator.comparing(File::getName));

                    AtomicInteger counter = new AtomicInteger(1);
                    for (File img : images) {
                        String cleanName = counter.getAndIncrement() + ".png";
                        Files.copy(img.toPath(), targetAnimDir.resolve(cleanName), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private File findSpriteRoot(Path tempDir) {
        File[] folders = tempDir.toFile().listFiles(File::isDirectory);
        List<File> validFolders = Arrays.stream(folders != null ? folders : new File[0])
                .filter(f -> !f.getName().startsWith("__"))
                .toList();

        if (validFolders.size() != 1) {
            throw new RuntimeException("La structure du ZIP est incorrecte. Il doit contenir un seul dossier racine (ex: 'Monstre1/IDLE/...')");
        }
        return validFolders.getFirst();
    }

    private void cleanupTempDirectory(Path tempDir) {
        if (tempDir != null) {
            try {
                FileSystemUtils.deleteRecursively(tempDir);
            } catch (IOException e) {
                log.warn("Impossible de supprimer le dossier temporaire : {}", tempDir, e);
            }
        }
    }

    // --- ANALYSE D'IMAGES ---

    private void processAnimationsMetaData(File spriteRoot, Sprite sprite) {
        for (AnimationType type : ANIMATION_TYPES) {
            File typeDir = new File(spriteRoot, type.name());
            if (!typeDir.exists()) continue;

            File[] images = typeDir.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".png"));
            if (images == null || images.length == 0) continue;

            Arrays.sort(images, Comparator.comparing(File::getName));

            // ÉTAPE CLÉ : 1. Calcule de la Bounding Box Verticale Globale
            int[] globalVerticalBox = calculateGlobalVerticalBoundingBox(images);

            if (globalVerticalBox != null) {
                // Étape 2 : Rogner et remplacer les fichiers temporaires
                cropAndReplaceAnimationFramesVertical(images, globalVerticalBox);
            }

            // Créer une Animation pour chaque image avec indice commençant à 1
            for (int i = 0; i < images.length; i++) {
                File imgFile = images[i];
                int indice = i + 1; // commence à 1
                processSingleImageMetaData(imgFile, type, sprite, indice);
            }
        }
    }


    private void processSingleImageMetaData(File imgFile, AnimationType type, Sprite sprite, int indice) {
        try {
            BufferedImage img = ImageIO.read(imgFile);
            if (img == null) return;

            int width = img.getWidth();
            int height = img.getHeight();
            int frames = detectFrames(img, width, height);

            sprite.addAnimation(new Animation(frames, width, height, type, indice));

        } catch (IOException e) {
            log.error("Erreur lecture image pour métadonnées : {}", imgFile.getName(), e);
        }
    }


    /**
     * Algorithme de détection de frames basé sur les colonnes vides.
     */
    private int detectFrames(BufferedImage img, int width, int height) {
        final int ALPHA_THRESHOLD = 10;       // tolérance transparence
        final int MIN_ABSOLUTE_WIDTH = 5;    // résidus trop petits
        final double RESIDUAL_THRESHOLD = 0.3; // résidus relatifs
        final double LARGE_BLOCK_FACTOR = 1.9; // seuil pour blocs larges

        boolean[] columnHasPixels = new boolean[width];

        // 1. Scan vertical pour détecter les colonnes non vides
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int alpha = (img.getRGB(x, y) >>> 24) & 0xff;
                if (alpha > ALPHA_THRESHOLD) {
                    columnHasPixels[x] = true;
                    break;
                }
            }
        }

        // 2. Identifier les blocs continus
        List<Integer> frameWidths = new ArrayList<>();
        int currentWidth = 0;
        boolean insideFrame = false;
        for (boolean hasPixel : columnHasPixels) {
            if (hasPixel) {
                currentWidth++;
                insideFrame = true;
            } else if (insideFrame) {
                frameWidths.add(currentWidth);
                currentWidth = 0;
                insideFrame = false;
            }
        }
        if (insideFrame) frameWidths.add(currentWidth);

        if (frameWidths.isEmpty()) return 1;

        // 3. Calcul largeur moyenne robuste (exclure blocs trop petits)
        List<Integer> filteredWidths = frameWidths.stream()
                .filter(w -> w >= MIN_ABSOLUTE_WIDTH)
                .toList();

        double mean = filteredWidths.stream().mapToInt(Integer::intValue).average().orElse(0);
        double stdDev = Math.sqrt(filteredWidths.stream()
                .mapToDouble(w -> (w - mean) * (w - mean))
                .average()
                .orElse(0));
        double averageWidth = filteredWidths.stream()
                .filter(w -> Math.abs(w - mean) <= 2 * stdDev)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(mean);

        if (averageWidth == 0) return 1;

        // 4. Compter les frames
        int totalFrames = 0;
        for (int w : frameWidths) {
            if (w < MIN_ABSOLUTE_WIDTH || w < averageWidth * RESIDUAL_THRESHOLD) continue;

            if (w > averageWidth * LARGE_BLOCK_FACTOR) {
                int estimatedFrames = (int) Math.floor(w / averageWidth);
                totalFrames += Math.max(1, estimatedFrames);
            } else {
                totalFrames++;
            }
        }

        // Debug
        System.out.println("Blocs: " + frameWidths + " | Moyenne robuste: " + averageWidth + " | Frames: " + totalFrames);

        return Math.max(1, totalFrames);
    }

    // --- CRUD ---

    public List<SpriteInfos> getAllSpritesInfos() {
        return spriteRepository.getAllSpritesInfos(AnimationType.IDLE);
    }

    @Transactional
    public void deleteSpriteByName(String spriteName) {
        spriteRepository.deleteByName(spriteName);

        Path folderPath = staticStorageRoot.resolve(spriteName);
        try {
            if (Files.exists(folderPath)) {
                FileSystemUtils.deleteRecursively(folderPath);
                log.info("Dossier sprite supprimé : {}", folderPath);
            }
        } catch (IOException e) {
            log.error("Erreur critique lors de la suppression du dossier physique {}", folderPath, e);
        }
    }

    @Transactional
    public SpriteInfos renameSprite(ModifSpriteDto modifSpriteDto) {
        Sprite sprite = spriteRepository.findById(modifSpriteDto.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sprite introuvable ID: " + modifSpriteDto.getId()));

        boolean nameChanged = !sprite.getName().equals(modifSpriteDto.getNewName());
        boolean scaleChanged = !Objects.equals(sprite.getScale(), modifSpriteDto.getScale());

        if (scaleChanged) {
            sprite.setScale(modifSpriteDto.getScale());
        }
        if (nameChanged) {
            String oldName = sprite.getName();
            sprite.setName(modifSpriteDto.getNewName());
            spriteRepository.save(sprite);

            Path oldPath = staticStorageRoot.resolve(oldName);
            Path newPath = staticStorageRoot.resolve(modifSpriteDto.getNewName());

            try {
                if (Files.exists(oldPath)) {
                    Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    log.info("Dossier renommé de '{}' vers '{}'", oldName, modifSpriteDto.getNewName());
                } else {
                    log.warn("Tentative de renommage d'un dossier inexistant : {}", oldPath);
                }
            } catch (IOException e) {
                log.error("Erreur lors du renommage du dossier '{}' vers '{}'", oldPath, newPath, e);
                throw new RuntimeException("Erreur I/O lors du renommage du dossier sprite", e);
            }
        } else if (scaleChanged) {
            spriteRepository.save(sprite);
        }

        return spriteRepository.getSpritesInfosById(AnimationType.IDLE, sprite.getId());
    }

    public List<SpriteInfos> getAllSprites(int spriteId) {
        return spriteRepository.getAllSprites(spriteId);
    }
}