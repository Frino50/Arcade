package perso.arcade.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import perso.arcade.model.dto.SpriteSummaryDTO;
import perso.arcade.service.SpriteService;

import java.util.List;

@RestController
@RequestMapping("/api/sprite")
public class SpriteController {

    private final SpriteService spriteService;

    public SpriteController(SpriteService spriteService) {
        this.spriteService = spriteService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> detectFrames(@RequestParam("file") MultipartFile zipFile) {
        spriteService.processSpriteZip(zipFile);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<List<SpriteSummaryDTO>> getSpritesSummary() {
        List<SpriteSummaryDTO> sprites = spriteService.getAllSpriteSummaries();
        return ResponseEntity.ok(sprites);
    }
}
