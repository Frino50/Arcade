package perso.arcade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class MorpionWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, GameSession> games = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionToGame = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Set<WebSocketSession> allSessions = ConcurrentHashMap.newKeySet();

    /**
     * Gère la connexion initiale d'une nouvelle session WebSocket.
     * Envoie la liste des parties disponibles au client qui vient de se connecter.
     *
     * @param session La session WebSocket qui vient d'être établie.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Nouvelle connexion : " + session.getId());
        allSessions.add(session); // Ajoutez la session à la liste de toutes les sessions
        sendLobbyUpdateToSingleSession(session);
    }

    /**
     * Gère les messages textuels entrants depuis les clients WebSocket.
     * Dispatche les actions en fonction du type de message (e.g., "create", "join", "move").
     *
     * @param session La session WebSocket d'où provient le message.
     * @param message Le message textuel reçu.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> data = mapper.readValue(message.getPayload(), Map.class);
        String type = (String) data.get("type");

        switch (type) {
            case "create":
                createGame(session);
                break;
            case "join":
                joinGame(session, (String) data.get("gameId"));
                break;
            case "spectate":
                spectateGame(session, (String) data.get("gameId"));
                break;
            case "move":
                handleMove(session, data);
                break;
            case "reset":
                handleReset(session);
                break;
            case "leave":
                handleLeaveGame(session);
                break;
        }
    }

    /**
     * Crée une nouvelle partie de Morpion et y assigne le créateur.
     * Met à jour et diffuse la liste des parties du lobby.
     *
     * @param session La session du joueur qui crée la partie.
     */
    private void createGame(WebSocketSession session) throws IOException {
        String newGameId = UUID.randomUUID().toString();
        GameSession newGame = new GameSession(newGameId, session);
        games.put(newGameId, newGame);
        sessionToGame.put(session, newGameId);

        Map<String, Object> assignMessage = new HashMap<>();
        assignMessage.put("type", "assign");
        assignMessage.put("gameId", newGameId);
        assignMessage.put("value", "X");
        session.sendMessage(new TextMessage(mapper.writeValueAsString(assignMessage)));

        System.out.println("Partie créée avec l'ID : " + newGameId);
        broadcastLobbyUpdate();
    }

    /**
     * Ajoute un joueur à une partie existante.
     * Démarre la partie si un deuxième joueur la rejoint.
     *
     * @param session La session du joueur qui rejoint la partie.
     * @param gameId  L'ID de la partie à rejoindre.
     */
    private void joinGame(WebSocketSession session, String gameId) throws IOException {
        GameSession game = games.get(gameId);
        if (game == null) return;

        if (game.addPlayer(session)) {
            sessionToGame.put(session, gameId);

            Map<String, Object> assignMessage = new HashMap<>();
            assignMessage.put("type", "assign");
            assignMessage.put("gameId", gameId);
            assignMessage.put("value", "O");
            session.sendMessage(new TextMessage(mapper.writeValueAsString(assignMessage)));

            broadcast(game, new HashMap<>() {{
                put("type", "game_started");
                put("gameId", gameId);
            }});

            System.out.println("Un joueur a rejoint la partie " + gameId);
            broadcastLobbyUpdate();
        }
    }

    /**
     * Ajoute une session en tant que spectateur à une partie existante.
     * Envoie l'état initial de la partie au spectateur.
     *
     * @param session La session du spectateur.
     * @param gameId  L'ID de la partie à observer.
     */
    private void spectateGame(WebSocketSession session, String gameId) throws IOException {
        GameSession game = games.get(gameId);
        if (game == null) return;

        game.addSpectator(session);
        sessionToGame.put(session, gameId);

        System.out.println("Un spectateur a rejoint la partie " + gameId);

        broadcast(game, new HashMap<>() {{
            put("type", "game_state");
            put("board", game.getBoard());
            put("players", game.getPlayers().entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().getId(), e -> e.getValue() == 1 ? "X" : "O")));
        }});
    }

    /**
     * Gère un coup joué par un joueur.
     * Vérifie la validité du coup, met à jour l'état du jeu et diffuse le changement.
     *
     * @param session La session du joueur qui a joué.
     * @param data    Les données du coup (ligne, colonne).
     */
    private void handleMove(WebSocketSession session, Map<String, Object> data) throws IOException {
        String gameId = sessionToGame.get(session);
        GameSession game = games.get(gameId);
        if (game == null) return;

        int row = (int) data.get("row");
        int col = (int) data.get("col");

        if (game.isValidMove(row, col, session)) {
            game.makeMove(row, col);

            broadcast(game, new HashMap<>() {{
                put("type", "move");
                put("row", row);
                put("col", col);
                put("value", game.getBoard()[row][col]);
                put("currentPlayer", game.getCurrentPlayer());
            }});

            if (game.checkWin()) {
                broadcast(game, new HashMap<>() {{
                    put("type", "game_over");
                    put("winner", game.getBoard()[row][col]);
                }});
            } else if (game.isBoardFull()) {
                broadcast(game, new HashMap<>() {{
                    put("type", "game_over");
                    put("winner", 0);
                }});
            }
        }
    }

    /**
     * Gère la réinitialisation d'une partie.
     *
     * @param session La session du joueur demandant la réinitialisation.
     */
    private void handleReset(WebSocketSession session) throws IOException {
        String gameId = sessionToGame.get(session);
        GameSession game = games.get(gameId);
        if (game == null) return;

        game.resetBoard();
        broadcast(game, new HashMap<>() {{
            put("type", "game_reset");
        }});
    }

    /**
     * Gère le départ volontaire d'une session de jeu.
     * Met à jour les états, informe les autres joueurs et supprime la partie si elle est vide.
     *
     * @param session La session qui quitte la partie.
     */
    private void handleLeaveGame(WebSocketSession session) throws IOException {
        String gameId = sessionToGame.remove(session);
        if (gameId != null) {
            GameSession game = games.get(gameId);
            if (game != null) {
                game.removeSession(session);
                System.out.println("Session " + session.getId() + " a quitté la partie " + gameId);

                if (game.getPlayers().size() < 2 && game.isStarted()) {
                    broadcast(game, new HashMap<>() {{
                        put("type", "player_disconnected");
                    }});
                }

                removeGameIfEmpty(gameId);
            }
        }
        broadcastLobbyUpdate();
        sendLobbyUpdateToSingleSession(session);
    }

    /**
     * Supprime une partie si elle ne contient plus de joueurs ou de spectateurs.
     *
     * @param gameId L'ID de la partie à vérifier.
     */
    private void removeGameIfEmpty(String gameId) throws IOException {
        GameSession game = games.get(gameId);
        if (game != null) {
            if (game.getPlayers().isEmpty() && game.getSpectators().isEmpty()) {
                games.remove(gameId);
                System.out.println("Partie " + gameId + " supprimée car elle est vide.");
            }
        }
    }

    /**
     * Gère la fermeture de la connexion d'une session.
     * Nettoie les sessions et les parties en cours.
     *
     * @param session La session dont la connexion a été fermée.
     * @param status  Le statut de la fermeture.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        allSessions.remove(session);
        String gameId = sessionToGame.remove(session);
        if (gameId != null) {
            GameSession game = games.get(gameId);
            if (game != null) {
                game.removeSession(session);
                System.out.println("Session " + session.getId() + " déconnectée de la partie " + gameId);

                if (game.getPlayers().size() < 2 && game.isStarted()) {
                    broadcast(game, new HashMap<>() {{
                        put("type", "player_disconnected");
                    }});
                }
                removeGameIfEmpty(gameId);
            }
        }
        broadcastLobbyUpdate();
    }

    /**
     * Diffuse un message à tous les participants (joueurs et spectateurs) d'une partie.
     *
     * @param game    La session de jeu concernée.
     * @param message Le message à diffuser.
     */
    private void broadcast(GameSession game, Map<String, Object> message) throws IOException {
        String json = mapper.writeValueAsString(message);

        for (WebSocketSession playerSession : game.getPlayers().keySet()) {
            if (playerSession.isOpen()) {
                playerSession.sendMessage(new TextMessage(json));
            }
        }

        for (WebSocketSession spectatorSession : game.getSpectators()) {
            if (spectatorSession.isOpen()) {
                spectatorSession.sendMessage(new TextMessage(json));
            }
        }
    }

    /**
     * Envoie la liste des parties disponibles à une seule session.
     *
     * @param session La session à mettre à jour.
     */
    private void sendLobbyUpdateToSingleSession(WebSocketSession session) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(lobbyMessage()));
        }
    }

    private String lobbyMessage() throws JsonProcessingException {
        List<Map<String, Object>> lobbyGames = games.values().stream()
                .map(game -> new HashMap<String, Object>() {{
                    put("id", game.getId());
                    put("players", game.getPlayers().size());
                    put("spectators", game.getSpectators().size());
                    put("isFull", game.isFull());
                }})
                .collect(Collectors.toList());

        Map<String, Object> lobbyMessage = new HashMap<>();
        lobbyMessage.put("type", "lobby_update");
        lobbyMessage.put("games", lobbyGames);

        return mapper.writeValueAsString(lobbyMessage);
    }

    /**
     * Diffuse la liste des parties disponibles à toutes les sessions qui se trouvent dans le lobby (c'est-à-dire qui ne sont pas en partie).
     */
    private void broadcastLobbyUpdate() throws IOException {

        for (WebSocketSession session : allSessions) {
            if (!sessionToGame.containsKey(session) && session.isOpen()) {
                session.sendMessage(new TextMessage(lobbyMessage()));
            }
        }
    }
}