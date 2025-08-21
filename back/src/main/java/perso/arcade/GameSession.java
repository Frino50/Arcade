package perso.arcade;

import org.springframework.web.socket.WebSocketSession;

import java.util.*;

public class GameSession {
    private final String id;
    private final Map<WebSocketSession, Integer> players = new HashMap<>();
    private final List<WebSocketSession> spectators = new ArrayList<>();
    private final int[][] board = new int[3][3];
    private int currentPlayer = 1;
    private boolean isStarted = false;

    public GameSession(String id, WebSocketSession creator) {
        this.id = id;
        this.players.put(creator, 1); // The creator is player 1 (X)
        Arrays.stream(board).forEach(row -> Arrays.fill(row, 0));
    }

    public String getId() {
        return id;
    }

    public boolean addPlayer(WebSocketSession session) {
        if (players.size() < 2) {
            players.put(session, 2);
            isStarted = true;
            return true;
        }
        return false;
    }

    public void addSpectator(WebSocketSession session) {
        spectators.add(session);
    }

    public void removeSession(WebSocketSession session) {
        players.remove(session);
        spectators.remove(session);
        if (players.isEmpty()) {
            isStarted = false;
        }
    }

    public Map<WebSocketSession, Integer> getPlayers() {
        return players;
    }

    public List<WebSocketSession> getSpectators() {
        return spectators;
    }

    public boolean isFull() {
        return players.size() == 2;
    }

    public int[][] getBoard() {
        return board;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isValidMove(int row, int col, WebSocketSession session) {
        return players.containsKey(session) &&
                players.get(session) == currentPlayer &&
                board[row][col] == 0;
    }

    public void makeMove(int row, int col) {
        board[row][col] = currentPlayer;
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
    }

    public boolean checkWin() {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] != 0 && board[i][0] == board[i][1] && board[i][1] == board[i][2]) return true;
            if (board[0][i] != 0 && board[0][i] == board[1][i] && board[1][i] == board[2][i]) return true;
        }
        if (board[0][0] != 0 && board[0][0] == board[1][1] && board[1][1] == board[2][2]) return true;
        return board[0][2] != 0 && board[0][2] == board[1][1] && board[1][1] == board[2][0];
    }

    public boolean isBoardFull() {
        for (int[] row : board) {
            for (int cell : row) {
                if (cell == 0) return false;
            }
        }
        return true;
    }

    public void resetBoard() {
        Arrays.stream(board).forEach(row -> Arrays.fill(row, 0));
        currentPlayer = 1;
    }
}