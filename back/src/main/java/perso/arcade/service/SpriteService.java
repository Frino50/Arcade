package perso.arcade.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import perso.arcade.exception.SpriteNameAlreadyExist;
import perso.arcade.model.dto.ModifSpriteDto;
import perso.arcade.model.dto.SpriteInfos;
import perso.arcade.model.entities.Animation;
import perso.arcade.model.entities.Sprite;
import perso.arcade.model.enumeration.AnimationType;
import perso.arcade.repository.AnimationRepository;
import perso.arcade.repository.SpriteRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class SpriteService {

    private static final Logger log = LoggerFactory.getLogger(SpriteService.class);

    // Constantes de détection de frames
    private static final int ALPHA_THRESHOLD = 10;
    private static final int MIN_ABSOLUTE_WIDTH = 5;
    private static final double RESIDUAL_THRESHOLD = 0.3;
    private static final double LARGE_BLOCK_FACTOR = 1.9;

    private static final List<AnimationType> ANIMATION_TYPES = List.of(
            AnimationType.IDLE,
            AnimationType.WALK,
            AnimationType.ATTACK
    );

    private final Path staticStorageRoot;
    private final SpriteRepository spriteRepository;
    private final AnimationRepository animationRepository;

    @Value("${sprite.storage.root}")
    private String spriteStorage;

    public SpriteService(
            SpriteRepository spriteRepository,
            AnimationRepository animationRepository,
            @Value("${sprite.storage.root}") String storageRoot) {
        this.spriteRepository = spriteRepository;
        this.animationRepository = animationRepository;
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

    // ==================== IMPORT SPRITE ====================

    @Transactional
    public SpriteInfos processSpriteZip(MultipartFile zipFile) {
        validateZipFile(zipFile);

        Path tempDir = null;
        try {
            tempDir = unzipToTempDirectory(zipFile);
            File tempSpriteRoot = findSpriteRoot(tempDir);
            String spriteName = tempSpriteRoot.getName();

            validateSpriteNotExists(spriteName);

            Sprite sprite = new Sprite(spriteName);
            processAnimationsMetaData(tempSpriteRoot, sprite);
            storeSpriteFilesCleanly(tempSpriteRoot, spriteName);
            sprite.setScale(1);

            spriteRepository.save(sprite);
            log.info("Sprite '{}' importé avec succès.", spriteName);
            log.info("--------------------------------------------");

            return spriteRepository.getSpritesInfosByTypeAndName(AnimationType.IDLE, sprite.getName());
        } catch (SpriteNameAlreadyExist e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors du traitement du ZIP", e);
            throw new RuntimeException("Erreur import sprite : " + e.getMessage(), e);
        } finally {
            cleanupTempDirectory(tempDir);
        }
    }

    // ==================== VALIDATION ====================

    private void validateZipFile(MultipartFile zipFile) {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new IllegalArgumentException("Le fichier ZIP est vide.");
        }
    }

    private void validateSpriteNotExists(String spriteName) {
        if (spriteRepository.findByName(spriteName).isPresent()) {
            throw new SpriteNameAlreadyExist("Un sprite avec le nom '" + spriteName + "' existe déjà.");
        }
    }

    // ==================== ANALYSE ET NORMALISATION ====================

    private void processAnimationsMetaData(File spriteRoot, Sprite sprite) {
        for (AnimationType type : ANIMATION_TYPES) {
            File typeDir = new File(spriteRoot, type.name());
            if (!typeDir.exists()) continue;

            File[] images = getImageFiles(typeDir);
            if (images.length == 0) continue;

            Map<File, Integer> frameCountByImage = analyzeAnimationFrames(images);
            createAnimations(images, frameCountByImage, type, sprite);
        }
    }

    private File[] getImageFiles(File directory) {
        File[] images = directory.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".png"));
        if (images == null || images.length == 0) {
            return new File[0];
        }
        Arrays.sort(images, Comparator.comparing(File::getName));
        return images;
    }

    /**
     * Analyse les frames de chaque image sans les modifier
     */
    private Map<File, Integer> analyzeAnimationFrames(File[] images) {
        Map<File, Integer> frameCountByImage = new HashMap<>();

        for (File file : images) {
            try {
                BufferedImage img = ImageIO.read(file);
                if (img == null) {
                    frameCountByImage.put(file, 1);
                    continue;
                }

                int frameCount = detectFrames(img);
                frameCountByImage.put(file, frameCount);
            } catch (IOException e) {
                log.error("Erreur lors de l'analyse de l'image {}", file.getName(), e);
                frameCountByImage.put(file, 1);
            }
        }

        return frameCountByImage;
    }

    private void createAnimations(File[] images, Map<File, Integer> frameCountByImage,
                                  AnimationType type, Sprite sprite) {
        for (int i = 0; i < images.length; i++) {
            File imgFile = images[i];
            int indice = i + 1;
            int frameCount = frameCountByImage.getOrDefault(imgFile, 1);

            try {
                BufferedImage img = ImageIO.read(imgFile);
                if (img != null) {
                    sprite.addAnimation(new Animation(
                            frameCount,
                            img.getWidth(),
                            img.getHeight(),
                            type,
                            indice
                    ));
                }
            } catch (IOException e) {
                log.error("Erreur lecture métadonnées image : {}", imgFile.getName(), e);
            }
        }
    }

    // ==================== DÉTECTION DE FRAMES ====================

    /**
     * Détecte automatiquement le nombre de frames dans une spritesheet
     */
    private int detectFrames(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        boolean[] columnHasPixels = scanColumns(img, width, height);
        List<Integer> frameWidths = extractFrameWidths(columnHasPixels);

        if (frameWidths.isEmpty()) {
            log.info("Aucun pixel détecté, 1 frame par défaut");
            return 1;
        }

        double averageWidth = calculateRobustAverageWidth(frameWidths);
        if (averageWidth == 0) {
            log.info("Largeur moyenne nulle, 1 frame par défaut");
            return 1;
        }

        int totalFrames = countFrames(frameWidths, averageWidth);
        log.info("Détection terminée: {} frames (blocs: {}, largeur moyenne: {}px)",
                totalFrames, frameWidths, averageWidth);
        log.info("--------------------------------------------");

        return totalFrames;
    }

    /**
     * Scanne chaque colonne pour détecter la présence de pixels
     */
    private boolean[] scanColumns(BufferedImage img, int width, int height) {
        boolean[] columnHasPixels = new boolean[width];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int alpha = (img.getRGB(x, y) >>> 24) & 0xff;
                if (alpha > ALPHA_THRESHOLD) {
                    columnHasPixels[x] = true;
                    break;
                }
            }
        }

        return columnHasPixels;
    }

    /**
     * Extrait les largeurs des blocs continus de pixels
     */
    private List<Integer> extractFrameWidths(boolean[] columnHasPixels) {
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

        if (insideFrame) {
            frameWidths.add(currentWidth);
        }

        return frameWidths;
    }

    /**
     * Calcule une largeur moyenne robuste en éliminant les outliers
     */
    private double calculateRobustAverageWidth(List<Integer> frameWidths) {
        List<Integer> filteredWidths = frameWidths.stream()
                .filter(w -> w >= MIN_ABSOLUTE_WIDTH)
                .toList();

        if (filteredWidths.isEmpty()) return 0;

        double mean = filteredWidths.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        double stdDev = Math.sqrt(filteredWidths.stream()
                .mapToDouble(w -> (w - mean) * (w - mean))
                .average()
                .orElse(0));

        return filteredWidths.stream()
                .filter(w -> Math.abs(w - mean) <= 2 * stdDev)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(mean);
    }

    /**
     * Compte le nombre total de frames en fonction des largeurs de blocs
     */
    private int countFrames(List<Integer> frameWidths, double averageWidth) {
        int totalFrames = 0;

        for (int w : frameWidths) {
            // Ignorer les résidus trop petits
            if (w < MIN_ABSOLUTE_WIDTH || w < averageWidth * RESIDUAL_THRESHOLD) {
                log.info("Bloc ignoré (trop petit): {} px", w);
                continue;
            }

            // Diviser les blocs anormalement larges
            if (w > averageWidth * LARGE_BLOCK_FACTOR) {
                int estimatedFrames = (int) Math.floor(w / averageWidth);
                totalFrames += Math.max(1, estimatedFrames);
                log.info("Bloc large divisé: {} px → {} frames", w, estimatedFrames);
            } else {
                totalFrames++;
                log.info("Bloc standard: {} px → 1 frame", w);
            }
        }

        return Math.max(1, totalFrames);
    }

    // ==================== RECONSTRUCTION DE SPRITE ====================

    /**
     * Reconstruit un sprite avec normalisation des frames
     * (alignement à gauche, largeurs égales)
     */
    private BufferedImage rebuildFinalSprite(BufferedImage original, int frameCount) {
        List<BufferedImage> rawFrames = splitByFrameCount(original, frameCount);
        int height = original.getHeight();

        // 1. Calculer les bornes GLOBALES (sur toute l'animation)
        int[] globalBounds = calculateGlobalHorizontalBounds(rawFrames);

        // Sécurité si l'image est vide
        if (globalBounds == null) {
            return original;
        }

        int globalMinX = globalBounds[0];
        int globalMaxX = globalBounds[1];
        int newFrameWidth = globalMaxX - globalMinX + 1;

        // Calcul de la nouvelle largeur totale
        int totalWidth = frameCount * newFrameWidth;
        BufferedImage output = new BufferedImage(totalWidth, height, BufferedImage.TYPE_INT_ARGB);

        log.info("Reconstruction Globale: {} frames × {}px (ogn: {}px) | Crop: x{} -> x{}",
                frameCount, newFrameWidth, totalWidth, globalMinX, globalMaxX);

        // 2. Composer en utilisant le décalage global constant
        composeFramesGlobal(rawFrames, output, globalMinX, newFrameWidth, height);

        return output;
    }

    /**
     * Calcule les bornes horizontales minimales et maximales sur l'ensemble des frames.
     *
     * @return int[]{minX, maxX} ou null si vide
     */
    private int[] calculateGlobalHorizontalBounds(List<BufferedImage> frames) {
        int globalMinX = Integer.MAX_VALUE;
        int globalMaxX = Integer.MIN_VALUE;
        boolean hasContent = false;

        for (BufferedImage frame : frames) {
            int[] bounds = computeHorizontalBounds(frame); // Réutilise votre méthode existante
            if (bounds != null) {
                hasContent = true;
                // On cherche le min le plus petit de TOUTES les frames
                globalMinX = Math.min(globalMinX, bounds[0]);
                // On cherche le max le plus grand de TOUTES les frames
                globalMaxX = Math.max(globalMaxX, bounds[1]);
            }
        }

        return hasContent ? new int[]{globalMinX, globalMaxX} : null;
    }

    /**
     * Compose les frames en appliquant le MÊME décalage (globalMinX) à toutes.
     */
    private void composeFramesGlobal(List<BufferedImage> frames, BufferedImage output,
                                     int globalMinX, int frameWidth, int height) {
        int xCursor = 0;

        for (BufferedImage frame : frames) {
            // On copie la portion définie par le globalMinX pour chaque frame
            // Cela inclut le "vide" relatif nécessaire au mouvement
            for (int x = 0; x < frameWidth; x++) {
                for (int y = 0; y < height; y++) {
                    // Coordonnée source : on décale toujours de globalMinX
                    int sourceX = globalMinX + x;

                    // Sécurité bounds (si la frame source est plus petite que le calcul théorique)
                    if (sourceX < frame.getWidth()) {
                        output.setRGB(xCursor + x, y, frame.getRGB(sourceX, y));
                    }
                }
            }
            xCursor += frameWidth;
        }
    }

    /**
     * Calcule les bornes horizontales du contenu visible d'une image
     */
    private int[] computeHorizontalBounds(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        int minX = width;
        int maxX = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int alpha = (img.getRGB(x, y) >>> 24) & 0xff;
                if (alpha > ALPHA_THRESHOLD) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                }
            }
        }

        return (minX > maxX) ? null : new int[]{minX, maxX};
    }

    /**
     * Divise une image en frames égales
     */
    private List<BufferedImage> splitByFrameCount(BufferedImage img, int frameCount) {
        int width = img.getWidth();
        int height = img.getHeight();
        int frameWidth = width / frameCount;

        List<BufferedImage> frames = new ArrayList<>();
        for (int i = 0; i < frameCount; i++) {
            int x = i * frameWidth;
            frames.add(img.getSubimage(x, 0, frameWidth, height));
        }

        return frames;
    }

    // ==================== GESTION ZIP ====================

    private Path unzipToTempDirectory(MultipartFile zipFile) throws IOException {
        Path destDir = Files.createTempDirectory("sprite_upload_");

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
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

    private File findSpriteRoot(Path tempDir) {
        File[] folders = tempDir.toFile().listFiles(File::isDirectory);
        List<File> validFolders = Arrays.stream(folders != null ? folders : new File[0])
                .filter(f -> !f.getName().startsWith("__"))
                .toList();

        if (validFolders.size() != 1) {
            throw new RuntimeException("La structure du ZIP est incorrecte. Il doit contenir un seul dossier racine.");
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

    // ==================== STOCKAGE FICHIERS ====================

    private void storeSpriteFilesCleanly(File tempSpriteRoot, String spriteName) throws IOException {
        Path targetSpriteDir = staticStorageRoot.resolve(spriteName);

        if (Files.exists(targetSpriteDir)) {
            FileSystemUtils.deleteRecursively(targetSpriteDir);
        }

        Files.createDirectories(targetSpriteDir);

        for (AnimationType type : ANIMATION_TYPES) {
            copyAnimationFiles(tempSpriteRoot, targetSpriteDir, type);
        }
    }

    private void copyAnimationFiles(File sourceRoot, Path targetRoot, AnimationType type) throws IOException {
        File sourceAnimDir = new File(sourceRoot, type.name());
        if (!sourceAnimDir.exists() || !sourceAnimDir.isDirectory()) return;

        Path targetAnimDir = targetRoot.resolve(type.name());
        Files.createDirectories(targetAnimDir);

        File[] images = getImageFiles(sourceAnimDir);
        if (images.length == 0) return;

        AtomicInteger counter = new AtomicInteger(1);
        for (File img : images) {
            String cleanName = counter.getAndIncrement() + ".png";
            Files.copy(img.toPath(), targetAnimDir.resolve(cleanName), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ==================== RECONSTRUCTION À LA DEMANDE ====================

    /**
     * Reconstruit une animation spécifique avec normalisation des frames
     * et met à jour le fichier et la base de données
     */
    @Transactional
    public SpriteInfos reBuildImage(Long animationId) throws IOException {
        SpriteInfos spriteInfos = spriteRepository.getSpriteInfosByAnimationId(animationId);
        if (spriteInfos == null) {
            throw new IllegalArgumentException("Animation introuvable ID: " + animationId);
        }

        Path filePath = Paths.get(spriteStorage, spriteInfos.getImageUrl());

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            throw new IOException("Fichier sprite introuvable: " + filePath);
        }

        try {
            // Charger l'image originale
            BufferedImage originalImg = ImageIO.read(filePath.toFile());
            if (originalImg == null) {
                throw new IOException("Impossible de lire l'image: " + filePath);
            }

            log.info("Reconstruction de l'image {} ({}x{}, {} frames attendues)",
                    filePath.getFileName(), originalImg.getWidth(),
                    originalImg.getHeight(), spriteInfos.getFrames());

            // Reconstruire avec normalisation
            BufferedImage normalizedImg = rebuildFinalSprite(originalImg, spriteInfos.getFrames());

            // Sauvegarder l'image normalisée
            ImageIO.write(normalizedImg, "png", filePath.toFile());

            // Mettre à jour la largeur en base de données
            Animation animation = animationRepository.findById(animationId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Animation introuvable ID: " + animationId));

            animation.setWidth(normalizedImg.getWidth());
            animationRepository.save(animation);

            log.info("✓ Image reconstruite: {} ({}x{}, {} frames)",
                    filePath.getFileName(), normalizedImg.getWidth(),
                    normalizedImg.getHeight(), spriteInfos.getFrames());

            return spriteRepository.getSpriteInfosByAnimationId(animationId);
        } catch (IOException e) {
            log.error("Erreur lors de la reconstruction de l'image {}", filePath, e);
            throw new RuntimeException("Erreur lors de la reconstruction de l'image: " + e.getMessage(), e);
        }
    }

    // ==================== RÉCUPÉRATION DE SPRITES ====================

    public ResponseEntity<Resource> getSprite(HttpServletRequest request) throws IOException {
        String relativePath = request.getRequestURI().replace("/api/sprite/sprite-storage/", "");
        Path filePath = Paths.get(spriteStorage, relativePath);

        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            Resource resource = new UrlResource(filePath.toUri());

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"")
                    .body(resource);
        }

        return ResponseEntity.notFound().build();
    }

    // ==================== CRUD OPERATIONS ====================

    public List<SpriteInfos> getAllSpritesInfos() {
        return spriteRepository.getAllSpritesInfos(AnimationType.IDLE);
    }

    public List<SpriteInfos> getAllAnimationsBySpriteName(String spriteName) {
        return spriteRepository.getAllAnimationsBySpriteName(spriteName);
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
        Sprite sprite = spriteRepository.findByName(modifSpriteDto.getOldName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sprite introuvable NOM: " + modifSpriteDto.getOldName()));

        boolean nameChanged = !sprite.getName().equals(modifSpriteDto.getNewName());
        boolean scaleChanged = !Objects.equals(sprite.getScale(), modifSpriteDto.getScale());

        if (scaleChanged) {
            sprite.setScale(modifSpriteDto.getScale());
        }

        if (nameChanged) {
            renameSpriteFolderAndEntity(sprite, modifSpriteDto.getNewName());
        }

        if (nameChanged || scaleChanged) {
            sprite = spriteRepository.save(sprite);
        }

        return spriteRepository.getSpritesInfosByTypeAndName(AnimationType.IDLE, sprite.getName());
    }

    private void renameSpriteFolderAndEntity(Sprite sprite, String newName) {
        String oldName = sprite.getName();
        sprite.setName(newName);

        Path oldPath = staticStorageRoot.resolve(oldName);
        Path newPath = staticStorageRoot.resolve(newName);

        try {
            if (Files.exists(oldPath)) {
                Files.move(oldPath, newPath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
                log.info("Dossier renommé de '{}' vers '{}'", oldName, newName);
            } else {
                log.warn("Tentative de renommage d'un dossier inexistant : {}", oldPath);
            }
        } catch (IOException e) {
            log.error("Erreur lors du renommage du dossier '{}' vers '{}'", oldPath, newPath, e);
            throw new RuntimeException("Erreur I/O lors du renommage du dossier sprite", e);
        }
    }
}