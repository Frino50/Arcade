package perso.arcade.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import perso.arcade.model.dto.ModifSpriteDto;
import perso.arcade.model.dto.SpriteInfos;
import perso.arcade.service.SpriteService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/sprite")
public class SpriteController {

    private final SpriteService spriteService;


    public SpriteController(SpriteService spriteService) {
        this.spriteService = spriteService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SpriteInfos uploadSprite(@RequestParam("file") MultipartFile zipFile) {
        return spriteService.processSpriteZip(zipFile);
    }

    @GetMapping("/all")
    public ResponseEntity<List<SpriteInfos>> getAllSpritesInfos() {
        List<SpriteInfos> spriteInfos = spriteService.getAllSpritesInfos();
        return ResponseEntity.ok(spriteInfos);
    }

    @DeleteMapping("/delete/{spriteName}")
    public ResponseEntity<Void> deleteSpriteByName(@PathVariable String spriteName) {
        spriteService.deleteSpriteByName(spriteName);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/rename")
    public SpriteInfos renameSprite(@RequestBody ModifSpriteDto modifSpriteDto) {
        return spriteService.renameSprite(modifSpriteDto);
    }

    @GetMapping("/sprite-storage/**")
    public ResponseEntity<Resource> getSprite(HttpServletRequest request) throws IOException {
        return spriteService.getSprite(request);
    }

    @GetMapping("/animations/{spriteName}")
    public List<SpriteInfos> getAllAnimationsBySpriteName(@PathVariable String spriteName) {
        return spriteService.getAllAnimationsBySpriteName(spriteName);
    }

    @GetMapping("/re-build-image/{animationId}")
    public SpriteInfos reBuildImage(@PathVariable Long animationId) throws IOException {
        return spriteService.reBuildImage(animationId);
    }

    @GetMapping("/flip-horizontal/{animationId}")
    public SpriteInfos flipHorizontal(@PathVariable Long animationId) {
        return spriteService.flipHorizontal(animationId);
    }
}