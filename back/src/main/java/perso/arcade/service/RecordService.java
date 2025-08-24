package perso.arcade.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import perso.arcade.model.*;
import perso.arcade.model.Record;
import perso.arcade.repository.GameRepository;
import perso.arcade.repository.PlayerRepository;
import perso.arcade.repository.RecordRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecordService {

    private final RecordRepository recordRepository;
    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;

    public RecordService(RecordRepository recordRepository, PlayerRepository playerRepository, GameRepository gameRepository) {
        this.recordRepository = recordRepository;
        this.playerRepository = playerRepository;
        this.gameRepository = gameRepository;
    }

    private String getPseudo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    public Record saveRecord(SaveRecordDto saveRecordDto) {
        String pseudo = getPseudo();
        Player player = playerRepository.findByPseudo(pseudo)
                .orElseThrow(() -> new RuntimeException("Player not found with pseudo: " + pseudo));

        Game game = gameRepository.findByName(saveRecordDto.getGameName())
                .orElseThrow(() -> new RuntimeException("Game not found with name: " + saveRecordDto.getGameName()));

        Record existingRecord = recordRepository.findByPlayerAndGame(player, game).orElse(null);

        if (existingRecord != null) {
            //  Met à jour l’existant
            existingRecord.setScore(saveRecordDto.getScore());
            existingRecord.setRecordDate(LocalDateTime.now());
            return recordRepository.save(existingRecord);
        } else {
            //  Crée un nouveau record si inexistant
            Record record = new Record();
            record.setScore(saveRecordDto.getScore());
            record.setRecordDate(LocalDateTime.now());
            record.setPlayer(player);
            record.setGame(game);
            return recordRepository.save(record);
        }
    }

    public List<ClassementDto> getLeaderboard(String gameName) {
        return gameRepository.getLeaderboard(gameName);
    }

    public Long getBestScore(String gameName) {
        String pseudo = getPseudo();

        Player player = playerRepository.findByPseudo(pseudo)
                .orElseThrow(() -> new RuntimeException("Player not found with pseudo: " + pseudo));

        Game game = gameRepository.findByName(gameName)
                .orElseThrow(() -> new RuntimeException("Game not found with name: " + gameName));

        return recordRepository.findByPlayerAndGame(player, game)
                .map(Record::getScore) // retourne le score
                .orElse(0L); // si aucun record, renvoyer 0
    }

}
