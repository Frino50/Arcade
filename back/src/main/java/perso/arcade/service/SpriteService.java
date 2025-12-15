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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
            // Nettoyage préventif si le sprite existe déjà en base pour éviter les doublons orphelins
            if (spriteRepository.findByName(spriteName).isPresent()) {
                throw new SpriteNameAlreadyExist("Un sprite avec le nom '" + spriteName + "' existe déjà.");
            }

            Sprite sprite = new Sprite(spriteName);

            // 2. Analyse des images (Frames, Dimensions)
            processAnimationsMetaData(tempSpriteRoot, sprite);

            // 3. Stockage définitif et propre (Renommage 1.png, 2.png...)
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

        // Si le dossier existe déjà (ex: résidu), on le nettoie
        if (Files.exists(targetSpriteDir)) {
            FileSystemUtils.deleteRecursively(targetSpriteDir);
        }
        Files.createDirectories(targetSpriteDir);

        // On ne copie QUE les dossiers d'animations connus (IDLE, WALK, etc.)
        // Cela évite de copier les __MACOSX ou autres fichiers inutiles du ZIP
        for (AnimationType type : ANIMATION_TYPES) {
            File sourceAnimDir = new File(tempSpriteRoot, type.name());

            if (sourceAnimDir.exists() && sourceAnimDir.isDirectory()) {
                Path targetAnimDir = targetSpriteDir.resolve(type.name());
                Files.createDirectories(targetAnimDir);

                File[] images = sourceAnimDir.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".png"));

                if (images != null && images.length > 0) {
                    // Tri alphabétique pour garantir l'ordre des frames
                    Arrays.sort(images, Comparator.comparing(File::getName));

                    AtomicInteger counter = new AtomicInteger(1);
                    for (File img : images) {
                        // Renommage propre : 1.png, 2.png...
                        String cleanName = counter.getAndIncrement() + ".png";
                        Files.copy(img.toPath(), targetAnimDir.resolve(cleanName), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private File findSpriteRoot(Path tempDir) {
        File[] folders = tempDir.toFile().listFiles(File::isDirectory);
        // On ignore __MACOSX s'il est présent à la racine
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
            if (images != null && images.length > 0) {
                Arrays.sort(images, Comparator.comparing(File::getName));

                // Créer une Animation pour chaque image
                for (File imgFile : images) {
                    processSingleImageMetaData(imgFile, type, sprite);
                }
            }
        }
    }


    private void processSingleImageMetaData(File imgFile, AnimationType type, Sprite sprite) {
        try {
            BufferedImage img = ImageIO.read(imgFile);
            if (img == null) return;

            int width = img.getWidth();
            int height = img.getHeight();
            int frames = detectFrames(img, width, height);

            sprite.addAnimation(new Animation(frames, width, height, type));

        } catch (IOException e) {
            log.error("Erreur lecture image pour métadonnées : {}", imgFile.getName(), e);
        }
    }

    /**
     * Algorithme de détection de frames basé sur les colonnes vides.
     */
    private int detectFrames(BufferedImage img, int width, int height) {
        // Optimisation : On ne scanne que si nécessaire.
        // Si l'image est petite, c'est rapide.

        boolean[] columnHasPixels = new boolean[width];

        // 1. Scan vertical pour détecter les colonnes non vides
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xff;
                if (alpha > 10) { // Seuil de tolérance transparence
                    columnHasPixels[x] = true;
                    break; // Pas besoin de vérifier le reste de la colonne
                }
            }
        }

        // 2. Compter les blocs continus
        int frames = 0;
        boolean insideFrame = false;

        for (int x = 0; x < width; x++) {
            if (columnHasPixels[x]) {
                if (!insideFrame) {
                    frames++;
                    insideFrame = true;
                }
            } else {
                insideFrame = false;
            }
        }

        // Sécurité : si aucune frame détectée mais que l'image existe, c'est au moins 1 frame
        return Math.max(1, frames);
    }

    // --- CRUD ---

    public List<SpriteInfos> getAllSpritesInfos() {
        return spriteRepository.getAllSpritesInfos(AnimationType.IDLE);
    }

    @Transactional
    public void deleteSpriteByName(String spriteName) {
        // 1. Suppression DB
        spriteRepository.deleteByName(spriteName);

        // 2. Suppression Fichiers
        // UTILISATION DU CHEMIN CONFIGURÉ, PAS EN DUR
        Path folderPath = staticStorageRoot.resolve(spriteName);
        try {
            if (Files.exists(folderPath)) {
                FileSystemUtils.deleteRecursively(folderPath);
                log.info("Dossier sprite supprimé : {}", folderPath);
            }
        } catch (IOException e) {
            // On log l'erreur mais on ne bloque pas la transaction DB forcément,
            // sinon le sprite réapparaitrait en base alors que le dossier est peut-être à moitié supprimé.
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
                    // ATOMIC_MOVE si possible
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