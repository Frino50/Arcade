package perso.arcade.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import perso.arcade.model.dto.SpriteInfos;
import perso.arcade.model.entities.Sprite;
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
    public Sprite uploadSprite(@RequestParam("file") MultipartFile zipFile) {
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

    @PutMapping("/rename/{idSprite}/{spriteName}")
    public Sprite renameSprite(@PathVariable Long idSprite, @PathVariable String spriteName) {
        return spriteService.renameSprite(idSprite, spriteName);
    }
}
