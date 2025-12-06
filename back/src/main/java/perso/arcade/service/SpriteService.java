package perso.arcade.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import perso.arcade.model.dto.SpriteSummaryDTO;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service de traitement des fichiers ZIP de sprites.
 * Gère l'upload, le dézippage, l'analyse des frames dans les images et la persistance
 * des entités {@link Sprite} et {@link Animation}.
 */
@Service
public class SpriteService {

    private static final List<AnimationType> ANIMATION_TYPES = List.of(
            AnimationType.IDLE,
            AnimationType.WALK,
            AnimationType.ATTACK
    );

    /**
     * Racine où les sprites seront stockés de manière permanente (e.g., dans /static/sprites).
     * Injectée depuis application.properties ou variables d'environnement.
     */
    private final Path staticStorageRoot;

    private final SpriteRepository spriteRepository;

    /**
     * Constructeur pour l'injection des dépendances et de la valeur de configuration.
     *
     * @param spriteRepository Repository pour la persistance des sprites.
     * @param storageRoot      Chemin absolu ou relatif où les fichiers doivent être stockés.
     */
    public SpriteService(
            SpriteRepository spriteRepository,
            @Value("${sprite.storage.root:/tmp/sprites}") String storageRoot) {
        this.spriteRepository = spriteRepository;
        this.staticStorageRoot = Path.of(storageRoot);
    }

    /**
     * Point d'entrée pour le traitement d'un fichier ZIP de sprites.
     * Gère le flux complet : dézippage, analyse, persistance, et nettoyage.
     *
     * @param zipFile Le fichier ZIP ({@link MultipartFile}) contenant les dossiers de sprites.
     * @throws IllegalArgumentException Si le fichier ZIP est nul ou vide.
     * @throws RuntimeException         Si une erreur survient lors du traitement, du dézippage ou du stockage.
     */
    public void processSpriteZip(MultipartFile zipFile) {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new IllegalArgumentException("Le fichier ZIP ne peut pas être vide.");
        }

        Path tempDir = null;

        try {
            tempDir = unzipToTempDirectory(zipFile);

            File spriteRoot = findSpriteRoot(tempDir);
            String spriteName = spriteRoot.getName();
            Sprite sprite = new Sprite(spriteName);

            // 1. Analyse des animations pour obtenir frames, width, height
            processAnimations(spriteRoot, sprite);

            // 2. Stockage des fichiers dans le dossier statique configuré avec renommage
            storeSpriteFiles(spriteRoot);

            // 3. Sauvegarde de l'entité en base
            spriteRepository.save(sprite);

        } catch (Exception e) {
            System.err.println("Erreur fatale lors du traitement du ZIP : " + e.getMessage());
            throw new RuntimeException("Erreur lors du traitement du fichier ZIP : " + e.getMessage(), e);
        } finally {
            cleanupTempDirectory(tempDir);
        }
    }

    /**
     * Crée un répertoire temporaire, y dézippe le contenu du fichier ZIP, et supprime le fichier ZIP temporaire.
     *
     * @param zipFile Le fichier ZIP à traiter.
     * @return Le chemin du répertoire temporaire où le ZIP a été extrait.
     * @throws IOException Si une erreur d'entrée/sortie se produit lors de la copie ou du dézippage.
     */
    private Path unzipToTempDirectory(MultipartFile zipFile) throws IOException {
        Path tempZip = null;
        Path destDir;

        try {
            tempZip = Files.createTempFile("sprite_upload_", Objects.requireNonNull(zipFile.getOriginalFilename()));
            Files.copy(zipFile.getInputStream(), tempZip, StandardCopyOption.REPLACE_EXISTING);

            destDir = Files.createTempDirectory("sprite_upload_");

            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(tempZip))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path newPath = destDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(newPath);
                    } else {
                        Files.createDirectories(newPath.getParent());
                        Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            return destDir;
        } finally {
            if (tempZip != null) Files.deleteIfExists(tempZip);
        }
    }

    /**
     * Supprime de manière récursive le répertoire temporaire spécifié.
     *
     * @param tempDir Le chemin du répertoire temporaire à nettoyer.
     */
    private void cleanupTempDirectory(Path tempDir) {
        if (tempDir == null || !Files.exists(tempDir)) return;

        try (Stream<Path> paths = Files.walk(tempDir)) {
            paths
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            System.err.println("Impossible de supprimer " + path + ": " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            System.err.println("Erreur lors du nettoyage du dossier temporaire : " + e.getMessage());
        }
    }

    /**
     * Copie récursivement les fichiers du dossier temporaire vers le dossier statique permanent
     * et les renomme avec un indice incrémental si ce sont des images.
     *
     * @param spriteRoot Le dossier racine du sprite dans le répertoire temporaire.
     * @throws IOException Si la copie des fichiers échoue.
     */
    private void storeSpriteFiles(File spriteRoot) throws IOException {
        String spriteName = spriteRoot.getName();

        Path targetDir = staticStorageRoot.resolve(spriteName);

        if (!Files.exists(staticStorageRoot)) {
            Files.createDirectories(staticStorageRoot);
        }

        // 1. Supprimer l'ancien dossier s'il existe
        if (Files.exists(targetDir)) {
            try (Stream<Path> paths = Files.walk(targetDir)) {
                paths
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                System.err.println("Impossible de supprimer " + path + ": " + e.getMessage());
                            }
                        });
            }
        }

        // 2. Initialiser le compteur d'index pour le renommage
        // Le renommage nécessite de connaître si on est dans un sous-dossier d'animation
        // La copie récursive simple ne permet pas un renommage par sous-dossier facilement.
        // On va copier d'abord les dossiers, puis copier/renommer les fichiers image un par un.

        // Créer d'abord la structure de dossiers cible
        try (Stream<Path> paths = Files.walk(spriteRoot.toPath())) {
            paths
                    .forEach(source -> {
                        try {
                            Path relativePath = spriteRoot.toPath().relativize(source);
                            Path destination = targetDir.resolve(relativePath);

                            if (Files.isDirectory(source)) {
                                Files.createDirectories(destination);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Erreur lors de la création des répertoires: " + e.getMessage(), e);
                        }
                    });
        }

        // Copier et renommer les fichiers image
        for (AnimationType type : ANIMATION_TYPES) {
            File sourceDir = new File(spriteRoot, type.name());
            File destinationDir = new File(targetDir.toFile(), type.name());

            if (sourceDir.exists() && destinationDir.exists()) {
                File[] imageFiles = sourceDir.listFiles(
                        f -> f.isFile() && f.getName().toLowerCase().endsWith(".png")
                );

                if (imageFiles != null) {
                    // Trier par nom original pour assurer un ordre (ex: "idle_01.png" avant "idle_02.png")
                    java.util.Arrays.sort(imageFiles, Comparator.comparing(File::getName));

                    AtomicInteger index = new AtomicInteger(1);
                    for (File imgFile : imageFiles) {
                        String newFileName = index.getAndIncrement() + ".png";
                        Path destinationPath = destinationDir.toPath().resolve(newFileName);
                        Files.copy(imgFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    // --- Fonctions Privées d'Analyse des Sprites (inchangées) ---

    /**
     * Valide et retourne le dossier racine unique du sprite à partir du répertoire dézippé.
     *
     * @param tempDir Le répertoire temporaire contenant les fichiers extraits du ZIP.
     * @return Le {@link File} représentant le dossier racine du sprite.
     * @throws RuntimeException Si aucun ou plusieurs dossiers racines sont trouvés.
     */
    private File findSpriteRoot(Path tempDir) {
        File[] spriteFolders = tempDir.toFile().listFiles(File::isDirectory);
        if (spriteFolders == null || spriteFolders.length != 1) {
            throw new RuntimeException("Le ZIP doit contenir exactement un dossier racine.");
        }
        return spriteFolders[0];
    }

    /**
     * Parcourt les dossiers d'animation (IDLE, WALK, ATTACK) dans le répertoire racine et traite les images PNG.
     *
     * @param spriteRoot Le dossier racine du sprite.
     * @param sprite     L'entité {@link Sprite} à laquelle ajouter les animations.
     */
    private void processAnimations(File spriteRoot, Sprite sprite) {
        for (AnimationType type : ANIMATION_TYPES) {
            File typeDir = new File(spriteRoot, type.name());
            if (!typeDir.exists() || !typeDir.isDirectory()) continue;

            File[] imageFiles = typeDir.listFiles(
                    f -> f.isFile() && f.getName().toLowerCase().endsWith(".png")
            );

            if (imageFiles != null) {
                for (File imgFile : imageFiles) {
                    // On garde le principe d'une seule image (sprite strip) par dossier d'animation
                    // mais si plusieurs existent, on les analyse toutes.
                    processImageFile(imgFile, type, sprite);
                }
            }
        }
    }

    /**
     * Lit une image PNG, détecte le nombre de frames qu'elle contient,
     * et crée une entité {@link Animation} associée au {@link Sprite} donné.
     *
     * @param imgFile Le fichier image PNG à analyser.
     * @param type    Le type d'animation (IDLE, WALK, ATTACK).
     * @param sprite  Le {@link Sprite} parent pour la nouvelle animation.
     */
    private void processImageFile(File imgFile, AnimationType type, Sprite sprite) {
        BufferedImage img;
        try {
            img = ImageIO.read(imgFile);
        } catch (IOException e) {
            System.err.println("Erreur lecture image " + imgFile.getName() + " : " + e.getMessage());
            return;
        }
        if (img == null) {
            System.err.println("Le fichier " + imgFile.getName() + " n'est pas une image PNG valide.");
            return;
        }

        int width = img.getWidth();
        int height = img.getHeight();
        int frames = detectFrames(img, width, height);

        // NOTE: Votre logique actuelle est optimisée pour des *sprite strips* (une seule image = une animation).
        // Si vous passez à des *frames individuelles* (plusieurs images = une animation), la détection de 'frames'
        // dans cette méthode devrait toujours retourner 1, et le 'frames' de l'Animation devrait être le *compte* total des fichiers.
        // Pour l'instant, je garde la logique d'analyse existante.
        Animation anim = new Animation(frames, width, height, type);
        sprite.addAnimation(anim);
    }

    /**
     * Détecte le nombre de frames horizontales dans une image de sprite strip (bande de frames).
     * La détection se fait en identifiant les zones opaques séparées par des zones de colonnes transparentes.
     *
     * @param img    L'objet {@link BufferedImage} de l'image de sprite strip.
     * @param width  La largeur de l'image en pixels.
     * @param height La hauteur de l'image en pixels.
     * @return Le nombre de frames détectées.
     */
    private int detectFrames(BufferedImage img, int width, int height) {
        boolean[] columnOpaque = new boolean[width];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = img.getRGB(x, y);
                if (((pixel >> 24) & 0xff) > 10) {
                    columnOpaque[x] = true;
                    break;
                }
            }
        }

        int frames = 0;
        boolean inFrame = false;
        for (int x = 0; x < width; x++) {
            if (columnOpaque[x]) {
                if (!inFrame) inFrame = true;
            } else {
                if (inFrame) {
                    frames++;
                    inFrame = false;
                }
            }
        }
        if (inFrame) frames++;

        return frames;
    }

    public List<SpriteSummaryDTO> getAllSpriteSummaries() {
        return spriteRepository.findAllSpriteSummariesWithIdleAnimation(
                AnimationType.IDLE,
                AnimationType.IDLE.name()
        );
    }
}