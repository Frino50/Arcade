package perso.arcade.controller;

import org.springframework.web.bind.annotation.*;
import perso.arcade.model.dto.ClassementDto;
import perso.arcade.model.dto.SaveRecordDto;
import perso.arcade.model.entities.Record;
import perso.arcade.service.RecordService;

import java.util.List;

@RestController
@RequestMapping("/api/record")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping
    public Record saveRecord(@RequestBody SaveRecordDto saveRecordDto) {
        return recordService.saveRecord(saveRecordDto);
    }

    @GetMapping("/leaderboard/{gameName}")
    public List<ClassementDto> getLeaderboard(@PathVariable String gameName) {
        return recordService.getLeaderboard(gameName);
    }

    @GetMapping("/bestScore/{gameName}")
    public Long getBestScore(@PathVariable String gameName) {
        return recordService.getBestScore(gameName);
    }
}