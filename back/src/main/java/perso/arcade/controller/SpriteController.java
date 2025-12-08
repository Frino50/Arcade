package perso.arcade.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import perso.arcade.model.dto.ModifSpriteDto;
import perso.arcade.model.dto.SpriteInfos;
import perso.arcade.service.SpriteService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/sprite")
public class SpriteController {

    private final SpriteService spriteService;

    @Value("${sprite.storage.root}")
    private String spriteStorage;

    public SpriteController(SpriteService spriteService) {
        this.spriteService = spriteService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SpriteInfos uploadSprite(@RequestParam("file") MultipartFile zipFile) {
        return spriteService.processSpriteZip(zipFile);
    }

    @GetMapping("/summary")
    public ResponseEntity<List<SpriteInfos>> getAllSpritesInfos() {
        List<SpriteInfos> spriteInfos = spriteService.getAllSpritesInfos();
        return ResponseEntity.ok(spriteInfos);
    }

    @DeleteMapping("/delete/{spriteName}")
    public ResponseEntity<Void> deleteSpriteById(@PathVariable String spriteName) {
        spriteService.deleteSpriteByName(spriteName);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/rename")
    public SpriteInfos renameSprite(@RequestBody ModifSpriteDto modifSpriteDto) {
        return spriteService.renameSprite(modifSpriteDto);
    }

    @GetMapping("/sprite-storage/**")
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
        } else {
            return ResponseEntity.notFound().build();
        }
    }


}
