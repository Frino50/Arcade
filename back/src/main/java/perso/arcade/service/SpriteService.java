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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class SpriteService {

    public static final String SEPARATEUR = "═══════════════════════════════════════════════════════════";

    private static final Logger log = LoggerFactory.getLogger(SpriteService.class);
    private static final int ALPHA_THRESHOLD = 10;
    private static final int MIN_ABSOLUTE_WIDTH = 5;
    private static final double RESIDUAL_THRESHOLD = 0.3;
    private static final double LARGE_BLOCK_FACTOR = 1.9;
    private static final double STDDEV_MULTIPLIER = 2.0;
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

    // ==================== INITIALISATION ====================

    private void initStorage() {
        try {
            if (!Files.exists(staticStorageRoot)) {
                Files.createDirectories(staticStorageRoot);
                log.info("📁 Répertoire de stockage créé: {}", staticStorageRoot);
            } else {
                log.debug("📁 Répertoire de stockage existant: {}", staticStorageRoot);
            }
        } catch (IOException e) {
            log.error("❌ Impossible d'initialiser le stockage des sprites", e);
            throw new RuntimeException("Impossible d'initialiser le stockage des sprites", e);
        }
    }

    // ==================== IMPORT SPRITE ====================

    @Transactional
    public SpriteInfos processSpriteZip(MultipartFile zipFile) {
        log.info("🚀 Début du traitement du fichier ZIP: {}", zipFile.getOriginalFilename());

        validateZipFile(zipFile);

        Path tempDir = null;
        try {
            tempDir = unzipToTempDirectory(zipFile);
            File tempSpriteRoot = findSpriteRoot(tempDir);
            String spriteName = tempSpriteRoot.getName();

            log.info("📦 Sprite détecté: '{}'", spriteName);
            validateSpriteNotExists(spriteName);

            Sprite sprite = createSpriteEntity(spriteName);
            processAnimationsMetaData(tempSpriteRoot, sprite);
            storeSpriteFilesCleanly(tempSpriteRoot, spriteName);

            spriteRepository.save(sprite);

            log.info("✅ Sprite '{}' importé avec succès", spriteName);
            log.info(SEPARATEUR);

            return spriteRepository.getSpritesInfosByTypeAndName(AnimationType.IDLE, sprite.getName());

        } catch (SpriteNameAlreadyExist e) {
            log.warn("⚠️  Sprite déjà existant: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement du ZIP: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur import sprite: " + e.getMessage(), e);
        } finally {
            cleanupTempDirectory(tempDir);
        }
    }

    private Sprite createSpriteEntity(String spriteName) {
        Sprite sprite = new Sprite(spriteName);
        sprite.setScale(1);
        return sprite;
    }

    // ==================== VALIDATION ====================

    private void validateZipFile(MultipartFile zipFile) {
        if (zipFile == null || zipFile.isEmpty()) {
            log.error("❌ Fichier ZIP vide ou null");
            throw new IllegalArgumentException("Le fichier ZIP est vide");
        }
        log.debug("✓ Validation ZIP OK: {} ({} bytes)",
                zipFile.getOriginalFilename(), zipFile.getSize());
    }

    private void validateSpriteNotExists(String spriteName) {
        if (spriteRepository.findByName(spriteName).isPresent()) {
            log.warn("⚠️  Sprite '{}' existe déjà en base", spriteName);
            throw new SpriteNameAlreadyExist("Un sprite avec le nom '" + spriteName + "' existe déjà");
        }
        log.debug("✓ Nom de sprite disponible: '{}'", spriteName);
    }

    // ==================== ANALYSE DES ANIMATIONS ====================

    private void processAnimationsMetaData(File spriteRoot, Sprite sprite) {
        log.info("🔍 Analyse des animations du sprite...");

        int totalAnimations = 0;
        for (AnimationType type : ANIMATION_TYPES) {
            File typeDir = new File(spriteRoot, type.name());
            if (!typeDir.exists()) {
                log.debug("  ⊘ Type {} non présent", type);
                continue;
            }

            File[] images = getImageFiles(typeDir);
            if (images.length == 0) {
                log.debug("  ⊘ Type {} vide", type);
                continue;
            }

            log.info("  📂 Traitement {} ({} images)", type, images.length);
            Map<File, Integer> frameCountByImage = analyzeAnimationFrames(images);
            createAnimations(images, frameCountByImage, type, sprite);
            totalAnimations += images.length;
        }

        log.info("✓ {} animations créées au total", totalAnimations);
    }

    private File[] getImageFiles(File directory) {
        File[] images = directory.listFiles(f ->
                f.isFile() && f.getName().toLowerCase().endsWith(".png"));

        if (images == null || images.length == 0) {
            return new File[0];
        }

        Arrays.sort(images, Comparator.comparing(File::getName));
        return images;
    }

    private Map<File, Integer> analyzeAnimationFrames(File[] images) {
        Map<File, Integer> frameCountByImage = new HashMap<>();

        for (File file : images) {
            try {
                BufferedImage img = ImageIO.read(file);
                if (img == null) {
                    log.warn("    ⚠️  Image illisible: {} (1 frame par défaut)", file.getName());
                    frameCountByImage.put(file, 1);
                    continue;
                }

                int frameCount = detectFrames(img, file.getName());
                frameCountByImage.put(file, frameCount);

            } catch (IOException e) {
                log.error("    ❌ Erreur lecture image {}: {}", file.getName(), e.getMessage());
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
                    log.debug("    ✓ Animation {}.{}: {}x{}px, {} frames",
                            type, indice, img.getWidth(), img.getHeight(), frameCount);
                }
            } catch (IOException e) {
                log.error("    ❌ Erreur création animation {}.{}: {}",
                        type, indice, e.getMessage());
            }
        }
    }

    // ==================== DÉTECTION DE FRAMES ====================

    private int detectFrames(BufferedImage img, String filename) {
        log.debug("    🔎 Analyse frames: {} ({}x{})", filename, img.getWidth(), img.getHeight());

        boolean[] columnHasPixels = scanColumns(img);
        List<Integer> frameWidths = extractFrameWidths(columnHasPixels);

        if (frameWidths.isEmpty()) {
            log.debug("       → Aucun pixel détecté, 1 frame par défaut");
            return 1;
        }

        double averageWidth = calculateRobustAverageWidth(frameWidths);
        if (averageWidth == 0) {
            log.debug("       → Largeur moyenne nulle, 1 frame par défaut");
            return 1;
        }

        int totalFrames = countFrames(frameWidths, averageWidth);
        log.debug("       → {} frames détectées (largeur moy: {}px)", totalFrames, averageWidth);

        return totalFrames;
    }

    private boolean[] scanColumns(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
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
                .filter(w -> Math.abs(w - mean) <= STDDEV_MULTIPLIER * stdDev)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(mean);
    }

    private int countFrames(List<Integer> frameWidths, double averageWidth) {
        int totalFrames = 0;

        for (int w : frameWidths) {
            if (isResidualBlock(w, averageWidth)) {
                log.trace("          • Bloc ignoré (résidu): {}px", w);
                continue;
            }

            if (isLargeBlock(w, averageWidth)) {
                int estimatedFrames = estimateFramesInLargeBlock(w, averageWidth);
                totalFrames += estimatedFrames;
                log.trace("          • Bloc large divisé: {}px → {} frames", w, estimatedFrames);
            } else {
                totalFrames++;
                log.trace("          • Bloc standard: {}px → 1 frame", w);
            }
        }

        return Math.max(1, totalFrames);
    }

    private boolean isResidualBlock(int width, double averageWidth) {
        return width < MIN_ABSOLUTE_WIDTH || width < averageWidth * RESIDUAL_THRESHOLD;
    }

    private boolean isLargeBlock(int width, double averageWidth) {
        return width > averageWidth * LARGE_BLOCK_FACTOR;
    }

    private int estimateFramesInLargeBlock(int width, double averageWidth) {
        return Math.max(1, (int) Math.floor(width / averageWidth));
    }

    // ==================== RECONSTRUCTION DE SPRITE ====================

    private BufferedImage rebuildFinalSprite(BufferedImage original, int frameCount) {
        log.debug("🔧 Reconstruction sprite: {}x{}px, {} frames",
                original.getWidth(), original.getHeight(), frameCount);

        List<BufferedImage> rawFrames = splitByFrameCount(original, frameCount);
        int[] globalBounds = calculateGlobalBounds(rawFrames);

        if (globalBounds == null) {
            log.debug("   → Image vide, conservation de l'original");
            return original;
        }

        Bounds bounds = new Bounds(globalBounds);
        int newFrameWidth = bounds.width();
        int newFrameHeight = bounds.height();
        int totalWidth = frameCount * newFrameWidth;

        BufferedImage output = new BufferedImage(totalWidth, newFrameHeight, BufferedImage.TYPE_INT_ARGB);

        log.debug("   → Optimisation: crop X[{}-{}], Y[{}-{}]",
                bounds.minX, bounds.maxX, bounds.minY, bounds.maxY);
        log.debug("   → Nouvelles dimensions: {}x{}px par frame", newFrameWidth, newFrameHeight);

        composeFramesGlobal(rawFrames, output, bounds);

        return output;
    }

    private List<BufferedImage> splitByFrameCount(BufferedImage img, int frameCount) {
        int frameWidth = img.getWidth() / frameCount;
        List<BufferedImage> frames = new ArrayList<>();

        for (int i = 0; i < frameCount; i++) {
            int x = i * frameWidth;
            frames.add(img.getSubimage(x, 0, frameWidth, img.getHeight()));
        }

        return frames;
    }

    private int[] calculateGlobalBounds(List<BufferedImage> frames) {
        int globalMinX = Integer.MAX_VALUE;
        int globalMaxX = Integer.MIN_VALUE;
        int globalMinY = Integer.MAX_VALUE;
        int globalMaxY = Integer.MIN_VALUE;

        boolean hasContent = false;

        for (BufferedImage frame : frames) {
            int[] bounds = computeContentBounds(frame);
            if (bounds != null) {
                hasContent = true;
                globalMinX = Math.min(globalMinX, bounds[0]);
                globalMaxX = Math.max(globalMaxX, bounds[1]);
                globalMinY = Math.min(globalMinY, bounds[2]);
                globalMaxY = Math.max(globalMaxY, bounds[3]);
            }
        }

        return hasContent ? new int[]{globalMinX, globalMaxX, globalMinY, globalMaxY} : null;
    }

    private int[] computeContentBounds(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        int minX = width, maxX = 0;
        int minY = height, maxY = 0;
        boolean hasPixels = false;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int alpha = (img.getRGB(x, y) >>> 24) & 0xff;
                if (alpha > ALPHA_THRESHOLD) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                    hasPixels = true;
                }
            }
        }

        return hasPixels ? new int[]{minX, maxX, minY, maxY} : null;
    }

    private void composeFramesGlobal(List<BufferedImage> frames, BufferedImage output, Bounds bounds) {
        int xCursor = 0;
        int newFrameWidth = bounds.width();
        int newFrameHeight = bounds.height();

        for (BufferedImage frame : frames) {
            for (int x = 0; x < newFrameWidth; x++) {
                for (int y = 0; y < newFrameHeight; y++) {
                    int sourceX = bounds.minX + x;
                    int sourceY = bounds.minY + y;

                    if (sourceX < frame.getWidth() && sourceY < frame.getHeight()) {
                        output.setRGB(xCursor + x, y, frame.getRGB(sourceX, sourceY));
                    }
                }
            }
            xCursor += newFrameWidth;
        }
    }

    // ==================== CLASSE INTERNE POUR LES BOUNDS ====================

    private Path unzipToTempDirectory(MultipartFile zipFile) throws IOException {
        Path destDir = Files.createTempDirectory("sprite_upload_");
        log.debug("📂 Extraction ZIP vers: {}", destDir);

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            int fileCount = 0;

            while ((entry = zis.getNextEntry()) != null) {
                Path newPath = destDir.resolve(entry.getName()).normalize();

                if (!newPath.startsWith(destDir)) {
                    throw new IOException("Entrée ZIP invalide (path traversal): " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                    fileCount++;
                }
            }

            log.debug("✓ {} fichiers extraits", fileCount);
        }

        return destDir;
    }

    // ==================== GESTION ZIP ====================

    private File findSpriteRoot(Path tempDir) {
        File[] folders = tempDir.toFile().listFiles(File::isDirectory);
        List<File> validFolders = Arrays.stream(folders != null ? folders : new File[0])
                .filter(f -> !f.getName().startsWith("__"))
                .toList();

        if (validFolders.size() != 1) {
            log.error("❌ Structure ZIP incorrecte: {} dossiers trouvés (1 attendu)", validFolders.size());
            throw new RuntimeException("La structure du ZIP est incorrecte. Il doit contenir un seul dossier racine");
        }

        File root = validFolders.getFirst();
        log.debug("✓ Dossier racine identifié: {}", root.getName());
        return root;
    }

    private void cleanupTempDirectory(Path tempDir) {
        if (tempDir != null) {
            try {
                FileSystemUtils.deleteRecursively(tempDir);
                log.debug("🗑️  Dossier temporaire supprimé: {}", tempDir);
            } catch (IOException e) {
                log.warn("⚠️  Impossible de supprimer le dossier temporaire: {}", tempDir);
            }
        }
    }

    private void storeSpriteFilesCleanly(File tempSpriteRoot, String spriteName) throws IOException {
        Path targetSpriteDir = staticStorageRoot.resolve(spriteName);

        if (Files.exists(targetSpriteDir)) {
            log.debug("🗑️  Suppression du dossier existant: {}", targetSpriteDir);
            FileSystemUtils.deleteRecursively(targetSpriteDir);
        }

        Files.createDirectories(targetSpriteDir);
        log.debug("📁 Création du dossier cible: {}", targetSpriteDir);

        int totalFilesCopied = 0;
        for (AnimationType type : ANIMATION_TYPES) {
            totalFilesCopied += copyAnimationFiles(tempSpriteRoot, targetSpriteDir, type);
        }

        log.info("✓ {} fichiers copiés vers le stockage permanent", totalFilesCopied);
    }

    // ==================== STOCKAGE FICHIERS ====================

    private int copyAnimationFiles(File sourceRoot, Path targetRoot, AnimationType type) throws IOException {
        File sourceAnimDir = new File(sourceRoot, type.name());
        if (!sourceAnimDir.exists() || !sourceAnimDir.isDirectory()) {
            return 0;
        }

        Path targetAnimDir = targetRoot.resolve(type.name());
        Files.createDirectories(targetAnimDir);

        File[] images = getImageFiles(sourceAnimDir);
        if (images.length == 0) {
            return 0;
        }

        AtomicInteger counter = new AtomicInteger(1);
        for (File img : images) {
            String cleanName = counter.getAndIncrement() + ".png";
            Files.copy(img.toPath(), targetAnimDir.resolve(cleanName), StandardCopyOption.REPLACE_EXISTING);
        }

        log.debug("  ✓ {} fichiers copiés pour {}", images.length, type);
        return images.length;
    }

    @Transactional
    public SpriteInfos reBuildImage(Long animationId) throws IOException {
        log.info("🔧 Début reconstruction animation ID: {}", animationId);

        SpriteInfos spriteInfos = spriteRepository.getSpriteInfosByAnimationId(animationId);
        if (spriteInfos == null) {
            log.error("❌ Animation introuvable ID: {}", animationId);
            throw new IllegalArgumentException("Animation introuvable ID: " + animationId);
        }

        Path filePath = Paths.get(spriteStorage, spriteInfos.getImageUrl());

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            log.error("❌ Fichier sprite introuvable: {}", filePath);
            throw new IOException("Fichier sprite introuvable: " + filePath);
        }

        try {
            BufferedImage originalImg = ImageIO.read(filePath.toFile());
            if (originalImg == null) {
                throw new IOException("Impossible de lire l'image: " + filePath);
            }

            log.info("   Original: {}x{}px, {} frames",
                    originalImg.getWidth(), originalImg.getHeight(), spriteInfos.getFrames());

            BufferedImage normalizedImg = rebuildFinalSprite(originalImg, spriteInfos.getFrames());

            ImageIO.write(normalizedImg, "png", filePath.toFile());
            log.debug("💾 Image sauvegardée: {}", filePath.getFileName());

            Animation animation = animationRepository.findById(animationId)
                    .orElseThrow(() -> new IllegalArgumentException("Animation introuvable ID: " + animationId));

            animation.setWidth(normalizedImg.getWidth());
            animation.setHeight(normalizedImg.getHeight());
            animationRepository.save(animation);

            log.info("✅ Reconstruction terminée: {}x{}px, {} frames",
                    normalizedImg.getWidth(), normalizedImg.getHeight(), spriteInfos.getFrames());
            log.info(SEPARATEUR);

            return spriteRepository.getSpriteInfosByAnimationId(animationId);

        } catch (IOException e) {
            log.error("❌ Erreur reconstruction image: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la reconstruction de l'image: " + e.getMessage(), e);
        }
    }

    // ==================== RECONSTRUCTION À LA DEMANDE ====================

    public ResponseEntity<Resource> getSprite(HttpServletRequest request) throws IOException {
        String relativePath = request.getRequestURI().replace("/api/sprite/sprite-storage/", "");
        Path filePath = Paths.get(spriteStorage, relativePath);

        log.debug("🔍 Requête sprite: {}", relativePath);

        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            Resource resource = new UrlResource(filePath.toUri());

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            log.debug("✓ Sprite trouvé: {} ({})", filePath.getFileName(), contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"")
                    .body(resource);
        }

        log.warn("⚠️  Sprite introuvable: {}", relativePath);
        return ResponseEntity.notFound().build();
    }

    public List<SpriteInfos> getAllSpritesInfos() {
        return spriteRepository.getAllSpritesInfos(AnimationType.IDLE);
    }

    public List<SpriteInfos> getAllAnimationsBySpriteName(String spriteName) {
        return spriteRepository.getAllAnimationsBySpriteName(spriteName);
    }

    @Transactional
    public void deleteSpriteByName(String spriteName) {
        log.info("🗑️  Suppression du sprite: '{}'", spriteName);

        spriteRepository.deleteByName(spriteName);
        log.debug("✓ Sprite supprimé de la base de données");

        Path folderPath = staticStorageRoot.resolve(spriteName);
        try {
            if (Files.exists(folderPath)) {
                FileSystemUtils.deleteRecursively(folderPath);
                log.info("✓ Dossier physique supprimé: {}", folderPath);
            } else {
                log.warn("⚠️  Dossier physique introuvable: {}", folderPath);
            }
        } catch (IOException e) {
            log.error("❌ Erreur suppression du dossier: {}", folderPath, e);
        }

        log.info(SEPARATEUR);
    }

    @Transactional
    public SpriteInfos renameSprite(ModifSpriteDto modifSpriteDto) {
        log.info("✏️  Modification du sprite: '{}'", modifSpriteDto.getOldName());

        Sprite sprite = spriteRepository.findByName(modifSpriteDto.getOldName())
                .orElseThrow(() -> {
                    log.error("❌ Sprite introuvable: '{}'", modifSpriteDto.getOldName());
                    return new IllegalArgumentException("Sprite introuvable NOM: " + modifSpriteDto.getOldName());
                });

        boolean nameChanged = !sprite.getName().equals(modifSpriteDto.getNewName());
        boolean scaleChanged = !Objects.equals(sprite.getScale(), modifSpriteDto.getScale());

        if (scaleChanged) {
            log.info("   → Modification de l'échelle: {} → {}", sprite.getScale(), modifSpriteDto.getScale());
            sprite.setScale(modifSpriteDto.getScale());
        }

        if (nameChanged) {
            log.info("   → Renommage: '{}' → '{}'", sprite.getName(), modifSpriteDto.getNewName());
            renameSpriteFolderAndEntity(sprite, modifSpriteDto.getNewName());
        }

        if (nameChanged || scaleChanged) {
            sprite = spriteRepository.save(sprite);
            log.info("✓ Sprite mis à jour en base de données");
        } else {
            log.info("ℹ️  Aucune modification à appliquer");
        }

        log.info(SEPARATEUR);

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
                log.info("✓ Dossier renommé: '{}' → '{}'", oldName, newName);
            } else {
                log.warn("⚠️  Dossier source inexistant: {}", oldPath);
            }
        } catch (IOException e) {
            log.error("❌ Erreur renommage du dossier: '{}' → '{}'", oldPath, newPath, e);
            throw new RuntimeException("Erreur I/O lors du renommage du dossier sprite", e);
        }
    }

    @Transactional
    public SpriteInfos flipHorizontal(Long animationId) {
        log.info("🔄 Retournement horizontal de l'animation ID: {}", animationId);

        SpriteInfos spriteInfos = spriteRepository.getSpriteInfosByAnimationId(animationId);
        if (spriteInfos == null) {
            throw new IllegalArgumentException("Animation introuvable ID: " + animationId);
        }

        Path filePath = Paths.get(spriteStorage, spriteInfos.getImageUrl());

        try {
            BufferedImage originalImg = ImageIO.read(filePath.toFile());
            if (originalImg == null) {
                throw new IOException("Impossible de lire l'image : " + filePath);
            }

            int frameCount = spriteInfos.getFrames();
            int frameWidth = originalImg.getWidth() / frameCount;
            int height = originalImg.getHeight();

            // Créer une image de destination de la même taille
            BufferedImage flippedImg = new BufferedImage(originalImg.getWidth(), height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = flippedImg.createGraphics();

            for (int i = 0; i < frameCount; i++) {
                int xSourceStart = i * frameWidth;
                int xSourceEnd = xSourceStart + frameWidth;

                // On dessine la frame i à la position i (ordre conservé)
                // Mais on inverse les coordonnées X de destination pour créer le miroir
                int xDestStart = i * frameWidth;
                int xDestEnd = xDestStart + frameWidth;

                // drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer)
                // En inversant dx1 et dx2, on opère le miroir horizontal
                g2d.drawImage(originalImg,
                        xDestEnd, 0, xDestStart, height, // Destination (inversée pour le flip)
                        xSourceStart, 0, xSourceEnd, height, // Source
                        null);
            }

            g2d.dispose();

            // Sauvegarde de l'image modifiée
            ImageIO.write(flippedImg, "png", filePath.toFile());
            log.info("✅ Animation retournée avec succès.");

            return spriteRepository.getSpriteInfosByAnimationId(animationId);

        } catch (IOException e) {
            log.error("❌ Erreur lors du retournement de l'image: {}", e.getMessage());
            throw new RuntimeException("Erreur I/O lors du retournement du sprite", e);
        }
    }

    private record Bounds(int minX, int maxX, int minY, int maxY) {
        Bounds(int[] bounds) {
            this(bounds[0], bounds[1], bounds[2], bounds[3]);
        }

        int width() {
            return maxX - minX + 1;
        }

        int height() {
            return maxY - minY + 1;
        }
    }
}