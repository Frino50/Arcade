package perso.arcade.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import perso.arcade.model.ClassementDto;
import perso.arcade.model.Record;
import perso.arcade.model.SaveRecordDto;
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
    public ResponseEntity<Record> saveRecord(@RequestBody SaveRecordDto saveRecordDto) {
        Record savedRecord = recordService.saveRecord(saveRecordDto);
        return new ResponseEntity<>(savedRecord, HttpStatus.CREATED);
    }

    @GetMapping("/leaderboard/{gameName}")
    public ResponseEntity<List<ClassementDto>> getLeaderboard(@PathVariable String gameName) {
        List<ClassementDto> leaderboard = recordService.getLeaderboard(gameName);
        return new ResponseEntity<>(leaderboard, HttpStatus.OK);
    }

    @GetMapping("/bestScore/{gameName}")
    public ResponseEntity<Long> getBestScore(@PathVariable String gameName) {
        Long bestScore = recordService.getBestScore(gameName);
        return new ResponseEntity<>(bestScore, HttpStatus.OK);
    }

}
