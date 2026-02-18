package com.example.jee.examen.service;

import com.example.jee.examen.dto.GameResponse;
import com.example.jee.examen.dto.RollResponse;
import com.example.jee.examen.dto.ScoreSheetDto;
import com.example.jee.examen.entity.ColonneScore;
import com.example.jee.examen.entity.Joueur;
import com.example.jee.examen.entity.Parties;
import com.example.jee.examen.enums.Category;
import com.example.jee.examen.enums.PartieStatus;
import com.example.jee.examen.enums.RuntimeGameStatus;
import com.example.jee.examen.repository.ColonneScoreRepository;
import com.example.jee.examen.repository.JoueurRepository;
import com.example.jee.examen.repository.PartiesRepository;
import com.example.jee.examen.runtime.GameState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final long TURN_DURATION_MS = 30_000L;

    private final PartiesRepository partiesRepository;
    private final ColonneScoreRepository colonneScoreRepository;
    private final JoueurRepository joueurRepository;
    private final GameStateManager gameStateManager;
    private final DiceService diceService;
    private final ScoreService scoreService;

    @Transactional
    public Long createGame(Long playerA, Long playerB) {
        if (Objects.equals(playerA, playerB)) {
            throw new ResponseStatusException(BAD_REQUEST, "Deux joueurs différents sont requis");
        }

        Parties partie = Parties.builder()
                .status(PartieStatus.EN_COURS)
                .build();
        partie = partiesRepository.save(partie);

        colonneScoreRepository.save(ColonneScore.builder().idPartie(partie.getId()).idJoueur(playerA).build());
        colonneScoreRepository.save(ColonneScore.builder().idPartie(partie.getId()).idJoueur(playerB).build());

        long now = System.currentTimeMillis();
        List<Long> playerIds = List.of(playerA, playerB);
        Map<Long, Integer> extraYam = new HashMap<>();
        extraYam.put(playerA, 0);
        extraYam.put(playerB, 0);

        GameState state = GameState.builder()
                .partieId(partie.getId())
                .playerIds(playerIds)
                .currentPlayerId(playerA)
                .dice(new int[]{0, 0, 0, 0, 0})
                .locked(new boolean[]{false, false, false, false, false})
                .rollCount(0)
                .turnStartedAt(now)
                .turnDeadlineAt(now + TURN_DURATION_MS)
                .extraYamCount(extraYam)
                .status(RuntimeGameStatus.IN_PROGRESS)
                .build();

        gameStateManager.put(partie.getId(), state);
        return partie.getId();
    }

    @Transactional(readOnly = true)
    public GameResponse getGame(Long gameId, Long requesterId) {
        assertPlayerInGame(gameId, requesterId);
        Parties partie = getPartie(gameId);
        List<ColonneScore> sheets = colonneScoreRepository.findByIdPartieOrderByIdJoueurAsc(gameId);
        GameState state = gameStateManager.get(gameId).orElse(null);
        return buildGameResponse(partie, state, sheets);
    }

    @Transactional
    public RollResponse roll(Long gameId, Long playerId) {
        GameState state = getStateOrThrow(gameId);
        assertPlayerInGame(gameId, playerId);
        applyTimeoutPenaltyIfNeeded(gameId, state);
        assertActivePlayer(state, playerId);

        if (state.getRollCount() >= 3) {
            throw new ResponseStatusException(BAD_REQUEST, "Maximum 3 lancers par tour");
        }

        int[] nextDice = state.getRollCount() == 0
                ? diceService.rollAll()
                : diceService.rerollUnlocked(state.getDice(), state.getLocked());

        state.setDice(nextDice);
        state.setRollCount(state.getRollCount() + 1);

        ColonneScore sheet = getSheet(gameId, playerId);
        return buildRollResponse(state, sheet, gameId);
    }

    @Transactional
    public RollResponse lockAndRoll(Long gameId, Long playerId, List<Integer> lockedIndexes) {
        GameState state = getStateOrThrow(gameId);
        assertPlayerInGame(gameId, playerId);
        applyTimeoutPenaltyIfNeeded(gameId, state);
        assertActivePlayer(state, playerId);

        if (state.getRollCount() >= 3) {
            throw new ResponseStatusException(BAD_REQUEST, "Maximum 3 lancers par tour");
        }

        boolean[] locks = new boolean[]{false, false, false, false, false};
        for (Integer idx : lockedIndexes) {
            if (idx == null || idx < 0 || idx > 4) {
                throw new ResponseStatusException(BAD_REQUEST, "Index de lock invalide: " + idx);
            }
            locks[idx] = true;
        }
        state.setLocked(locks);

        int[] nextDice = state.getRollCount() == 0
                ? diceService.rollAll()
                : diceService.rerollUnlocked(state.getDice(), state.getLocked());

        state.setDice(nextDice);
        state.setRollCount(state.getRollCount() + 1);

        ColonneScore sheet = getSheet(gameId, playerId);
        return buildRollResponse(state, sheet, gameId);
    }

    @Transactional
    public GameResponse score(Long gameId, Long playerId, Category category) {
        GameState state = getStateOrThrow(gameId);
        assertPlayerInGame(gameId, playerId);
        applyTimeoutPenaltyIfNeeded(gameId, state);
        assertActivePlayer(state, playerId);

        ColonneScore sheet = getSheet(gameId, playerId);
        if (scoreService.isFilled(sheet, category)) {
            throw new ResponseStatusException(BAD_REQUEST, "Catégorie déjà remplie");
        }

        boolean isYamRoll = scoreService.score(Category.YAM, state.getDice()) == 50;
        if (isYamRoll && sheet.getScoreYam() != null) {
            int extra = state.getExtraYamCount().getOrDefault(playerId, 0) + 1;
            state.getExtraYamCount().put(playerId, extra);
        }

        int value = scoreService.score(category, state.getDice());
        scoreService.setCategoryScore(sheet, category, value);
        scoreService.recomputeTotals(sheet, state.getExtraYamCount().getOrDefault(playerId, 0));
        colonneScoreRepository.save(sheet);

        return completeTurnOrFinish(gameId, state);
    }

    @Transactional
    public void applyTimeoutPenaltyIfNeeded(Long gameId, GameState state) {
        if (System.currentTimeMillis() <= state.getTurnDeadlineAt()) {
            return;
        }

        Long currentPlayerId = state.getCurrentPlayerId();
        ColonneScore sheet = getSheet(gameId, currentPlayerId);
        Category penaltyCategory = scoreService.firstUnfilledCategory(sheet);
        if (penaltyCategory == null) {
            return;
        }

        scoreService.setCategoryScore(sheet, penaltyCategory, 0);
        scoreService.recomputeTotals(sheet, state.getExtraYamCount().getOrDefault(currentPlayerId, 0));
        colonneScoreRepository.save(sheet);

        completeTurnOrFinish(gameId, state);
    }

    private GameResponse completeTurnOrFinish(Long gameId, GameState state) {
        List<ColonneScore> sheets = colonneScoreRepository.findByIdPartieOrderByIdJoueurAsc(gameId);
        boolean finished = sheets.stream().allMatch(scoreService::allCategoriesFilled);

        Parties partie = getPartie(gameId);
        if (finished) {
            partie.setStatus(PartieStatus.TERMINE);
            ColonneScore winnerSheet = sheets.stream()
                    .max(Comparator.comparingInt(ColonneScore::getScoreTotal))
                    .orElseThrow(() -> new ResponseStatusException(INTERNAL_SERVER_ERROR, "Impossible de déterminer le gagnant"));
            partie.setIdVainqueur(winnerSheet.getIdJoueur());
            partiesRepository.save(partie);
            state.setStatus(RuntimeGameStatus.FINISHED);
            return buildGameResponse(partie, state, sheets);
        }

        switchToNextPlayer(state);
        partiesRepository.save(partie);
        return buildGameResponse(partie, state, sheets);
    }

    private void switchToNextPlayer(GameState state) {
        List<Long> ids = state.getPlayerIds();
        int currentIdx = ids.indexOf(state.getCurrentPlayerId());
        int nextIdx = (currentIdx + 1) % ids.size();
        state.setCurrentPlayerId(ids.get(nextIdx));
        state.setRollCount(0);
        state.setDice(new int[]{0, 0, 0, 0, 0});
        state.setLocked(new boolean[]{false, false, false, false, false});
        long now = System.currentTimeMillis();
        state.setTurnStartedAt(now);
        state.setTurnDeadlineAt(now + TURN_DURATION_MS);
    }

    private RollResponse buildRollResponse(GameState state, ColonneScore sheet, Long gameId) {
        List<ColonneScore> allSheets = colonneScoreRepository.findByIdPartieOrderByIdJoueurAsc(gameId);
        return RollResponse.builder()
                .dice(state.getDice().clone())
                .locked(state.getLocked().clone())
                .rollCount(state.getRollCount())
                .rollsLeft(3 - state.getRollCount())
                .turnDeadlineAt(state.getTurnDeadlineAt())
                .possibleScores(scoreService.possibleScores(state.getDice(), sheet))
                .scores(toScoreSheetDtos(allSheets))
                .build();
    }

    private GameResponse buildGameResponse(Parties partie, GameState state, List<ColonneScore> sheets) {
        return GameResponse.builder()
                .gameId(partie.getId())
                .status(partie.getStatus())
                .currentPlayerId(state != null ? state.getCurrentPlayerId() : null)
                .playerIds(state != null ? state.getPlayerIds() : sheets.stream().map(ColonneScore::getIdJoueur).toList())
                .dice(state != null ? state.getDice().clone() : null)
                .locked(state != null ? state.getLocked().clone() : null)
                .rollCount(state != null ? state.getRollCount() : 0)
                .turnDeadlineAt(state != null ? state.getTurnDeadlineAt() : 0)
                .scores(toScoreSheetDtos(sheets))
                .winnerId(partie.getIdVainqueur())
                .build();
    }

    private List<ScoreSheetDto> toScoreSheetDtos(List<ColonneScore> sheets) {
        Map<Long, String> pseudoById = joueurRepository.findAllById(
                sheets.stream().map(ColonneScore::getIdJoueur).toList()
        ).stream().collect(Collectors.toMap(Joueur::getId, Joueur::getPseudo));

        return sheets.stream().map(sheet -> ScoreSheetDto.builder()
                .playerId(sheet.getIdJoueur())
                .pseudo(pseudoById.getOrDefault(sheet.getIdJoueur(), "unknown"))
                .score1(sheet.getScore1())
                .score2(sheet.getScore2())
                .score3(sheet.getScore3())
                .score4(sheet.getScore4())
                .score5(sheet.getScore5())
                .score6(sheet.getScore6())
                .totalNumbers(sheet.getTotalNumbers())
                .totalNumbersBonus(sheet.getTotalNumbersBonus())
                .scoreBrelan(sheet.getScoreBrelan())
                .scoreCarre(sheet.getScoreCarre())
                .scoreFull(sheet.getScoreFull())
                .scorePetiteSuite(sheet.getScorePetiteSuite())
                .scoreGrandeSuite(sheet.getScoreGrandeSuite())
                .scoreYam(sheet.getScoreYam())
                .scoreChance(sheet.getScoreChance())
                .scoreTotal(sheet.getScoreTotal())
                .build()).toList();
    }

    private void assertPlayerInGame(Long gameId, Long playerId) {
        if (!colonneScoreRepository.existsByIdPartieAndIdJoueur(gameId, playerId)) {
            throw new ResponseStatusException(FORBIDDEN, "Le joueur n'appartient pas à cette partie");
        }
    }

    private void assertActivePlayer(GameState state, Long playerId) {
        if (!Objects.equals(state.getCurrentPlayerId(), playerId)) {
            throw new ResponseStatusException(FORBIDDEN, "Ce n'est pas votre tour");
        }
    }

    private ColonneScore getSheet(Long gameId, Long playerId) {
        return colonneScoreRepository.findByIdPartieAndIdJoueur(gameId, playerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Feuille de score introuvable"));
    }

    private Parties getPartie(Long gameId) {
        return partiesRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Partie introuvable"));
    }

    private GameState getStateOrThrow(Long gameId) {
        return gameStateManager.get(gameId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Etat runtime introuvable"));
    }
}
