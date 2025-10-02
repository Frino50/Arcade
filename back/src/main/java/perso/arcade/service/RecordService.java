package perso.arcade.service;

import org.springframework.stereotype.Service;
import perso.arcade.model.dto.ClassementDto;
import perso.arcade.model.dto.SaveRecordDto;
import perso.arcade.model.entities.Game;
import perso.arcade.model.entities.Player;
import perso.arcade.model.entities.Record;
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
    private final UtilsService utilsService;

    public RecordService(RecordRepository recordRepository, PlayerRepository playerRepository, GameRepository gameRepository, UtilsService utilsService) {
        this.recordRepository = recordRepository;
        this.playerRepository = playerRepository;
        this.gameRepository = gameRepository;
        this.utilsService = utilsService;
    }

    public Record saveRecord(SaveRecordDto saveRecordDto) {
        String pseudo = utilsService.getPseudo();
        Player player = playerRepository.findByPseudo(pseudo)
                .orElseThrow(() -> new RuntimeException("Player not found with pseudo: " + pseudo));

        Game game = gameRepository.findByName(saveRecordDto.getGameName())
                .orElseThrow(() -> new RuntimeException("Game not found with name: " + saveRecordDto.getGameName()));

        Record existingRecord = recordRepository.findByPlayerAndGame(player, game).orElse(null);

        Long newScore = saveRecordDto.getScore();

        if (existingRecord != null) {
            Long currentScore = existingRecord.getScore();

            boolean shouldUpdate = game.isLowerIsBetter()
                    ? newScore < currentScore   // si lower is better → plus petit est meilleur
                    : newScore > currentScore;  // sinon → plus grand est meilleur

            if (shouldUpdate) {
                existingRecord.setScore(newScore);
                existingRecord.setRecordDate(LocalDateTime.now());
                return recordRepository.save(existingRecord);
            } else {
                return existingRecord;
            }
        } else {
            Record record = new Record();
            record.setScore(saveRecordDto.getScore());
            record.setRecordDate(LocalDateTime.now());
            record.setPlayer(player);
            record.setGame(game);
            return recordRepository.save(record);
        }

    }

    public List<ClassementDto> getLeaderboard(String gameName) {
        Game game = gameRepository.findByName(gameName)
                .orElseThrow(() -> new RuntimeException("Game not found with name: " + gameName));

        List<ClassementDto> classementDtoList = gameRepository.getLeaderboard(gameName, game.isLowerIsBetter());

        for (ClassementDto dto : classementDtoList) {
            if (game.isLowerIsBetter()) {
                dto.setScore(formatTime(dto.getScore()));
            } else {
                dto.setScore(String.valueOf(dto.getScore()));
            }
        }

        return classementDtoList;
    }

    private String formatTime(String millis) {
        long ms = Long.parseLong(millis);
        long minutes = ms / 60000;
        long seconds = (ms % 60000) / 1000;
        return String.format("%dm%02ds", minutes, seconds);
    }

    public Long getBestScore(String gameName) {
        Player player = utilsService.getPlayer();

        Game game = gameRepository.findByName(gameName)
                .orElseThrow(() -> new RuntimeException("Game not found with name: " + gameName));

        return recordRepository.findByPlayerAndGame(player, game)
                .map(Record::getScore) // retourne le score
                .orElse(0L); // si aucun record, renvoyer 0
    }
}