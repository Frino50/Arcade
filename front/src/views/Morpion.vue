<template>
    <div class="body-container">
        <Lobby
            v-if="!inGame"
            ref="lobby"
            @create-game="createGame"
            @join-game="joinGame"
            @spectate-game="spectateGame"
        />
        <div v-else class="body-container-game">
            <p v-if="waitingForPlayer" class="waiting-message">
                En attente d'un autre joueur...
            </p>

            <div class="aquidejouer" :class="turnStatusClass"></div>

            <Message
                v-if="gameMessage"
                :message="gameMessage"
                :isGreen="isGameMessageGreen"
            />

            <div class="tableau-container">
                <div
                    v-for="(value, index) in listMorpion.flat()"
                    :key="index"
                    @click="localClick(Math.floor(index / 3), index % 3)"
                    class="case-container"
                    :data-value="value"
                >
                    {{ value === 1 ? "X" : value === 2 ? "O" : "" }}
                </div>
            </div>

            <div class="game-info">
                <p v-if="!isPlayer">Tu es spectateur</p>
            </div>

            <div class="boutton-container">
                <button v-if="playerSymbol === 1" @click="resetGame">
                    Rejouer
                </button>
                <button @click="leaveGame">Quitter la partie</button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from "vue";
import Message from "@/components/Message.vue";
import Lobby from "@/components/Lobby.vue";

type PlayerValue = 1 | 2;

const listMorpion = ref<number[][]>([]);
const gameMessage = ref<string>("");
const isGameMessageGreen = ref(false);

const inGame = ref(false);
const isPlayer = ref(false);
const isMyTurn = ref(false);
const isGameReady = ref(false);
const waitingForPlayer = ref(false); // Nouvelle variable d'état
const isLeaving = ref(false);

const playerSymbol = ref<PlayerValue | null>(null);
let socket: WebSocket;
const currentGameId = ref<string | null>(null);
const lobby = ref(null);

const turnStatusClass = computed(() => {
    return {
        "red-light": !isMyTurn.value,
        "green-light": isMyTurn.value,
    };
});

onMounted(() => {
    resetBoard();
    connectWebSocket();
});

onBeforeUnmount(() => {
    socket.close();
});

function connectWebSocket() {
    socket = new WebSocket("ws://202.15.200.35:8080/morpion");
    socket.onopen = () => {
        console.log("Connecté au serveur WebSocket");
        isLeaving.value = false;
    };
    socket.onmessage = (event) => {
        const data = JSON.parse(event.data);
        console.log("Message reçu :", data);

        switch (data.type) {
            case "lobby_update":
                if (lobby.value) {
                    lobby.value.updateLobby(data.games);
                }
                break;
            case "assign":
                currentGameId.value = data.gameId;
                playerSymbol.value = data.value === "X" ? 1 : 2;
                isPlayer.value = true;
                inGame.value = true;
                if (playerSymbol.value === 1) {
                    waitingForPlayer.value = true;
                }
                break;
            case "game_started":
                isGameReady.value = true;
                isMyTurn.value = playerSymbol.value === 1;
                isGameMessageGreen.value = true;
                waitingForPlayer.value = false;
                break;
            case "move":
                handleClick(data.row, data.col, data.value);
                isMyTurn.value = data.currentPlayer === playerSymbol.value;
                break;
            case "game_over":
                isGameReady.value = false;
                isMyTurn.value = false;
                if (data.winner === 0) {
                    gameMessage.value = "Match nul !";
                    isGameMessageGreen.value = false;
                } else {
                    if (data.winner === playerSymbol.value) {
                        gameMessage.value = "Gagné !";
                        isGameMessageGreen.value = true;
                    } else {
                        gameMessage.value = "Perdu !";
                        isGameMessageGreen.value = false;
                    }
                }
                break;
            case "game_state":
                inGame.value = true;
                isPlayer.value = false;
                listMorpion.value = data.board;
                isMyTurn.value = false;
                isGameMessageGreen.value = true;
                break;
            case "game_reset":
                gameMessage.value = "";
                listMorpion.value = Array.from({ length: 3 }, () => [0, 0, 0]);
                isGameMessageGreen.value = true;
                isGameReady.value = true;
                isMyTurn.value = playerSymbol.value === 1;
                break;
            case "player_disconnected":
                alert("Un joueur a quitté la partie. Retour au lobby.");
                leaveGame();
                break;
        }
    };
    socket.onclose = () => {
        console.log("Déconnecté du serveur WebSocket");
        if (!isLeaving.value) {
            console.log("Reconnexion en cours...");
            setTimeout(connectWebSocket, 1000);
        }
    };
}

function createGame() {
    socket.send(JSON.stringify({ type: "create" }));
}

function joinGame(gameId: string) {
    socket.send(JSON.stringify({ type: "join", gameId }));
}

function spectateGame(gameId: string) {
    socket.send(JSON.stringify({ type: "spectate", gameId }));
}

function leaveGame() {
    if (socket && socket.readyState === WebSocket.OPEN && currentGameId.value) {
        socket.send(
            JSON.stringify({ type: "leave", gameId: currentGameId.value })
        );
    }

    isLeaving.value = true;
    gameMessage.value = "";
    inGame.value = false;
    isPlayer.value = false;
    isMyTurn.value = false;
    isGameReady.value = false;
    waitingForPlayer.value = false;
    playerSymbol.value = null;
    currentGameId.value = null;
    resetBoard();
}

function localClick(row: number, col: number) {
    if (
        isGameReady.value &&
        isPlayer.value &&
        isMyTurn.value &&
        listMorpion.value[row][col] === 0
    ) {
        socket.send(
            JSON.stringify({
                type: "move",
                gameId: currentGameId.value,
                row: row,
                col: col,
                value: playerSymbol.value,
            })
        );
        isMyTurn.value = false;
    }
}

function handleClick(rowIndex: number, colIndex: number, value: PlayerValue) {
    listMorpion.value[rowIndex].splice(colIndex, 1, value);
}

function resetGame() {
    if (inGame.value && isPlayer.value) {
        socket.send(
            JSON.stringify({ type: "reset", gameId: currentGameId.value })
        );
    }
}

function resetBoard() {
    listMorpion.value = Array.from({ length: 3 }, () => [0, 0, 0]);
}
</script>

<style scoped>
.body-container-game {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    gap: 1.5rem;
    position: relative;
}

.waiting-message {
    font-size: 1.5rem;
    color: #f39c12;
    position: absolute;
    top: 50px;
    left: 50%;
    transform: translateX(-50%);
    text-align: center;
    width: 100%;
}

.aquidejouer {
    width: 2rem;
    height: 2rem;
    border-radius: 100%;
}
.red-light {
    background-color: red;
}
.green-light {
    background-color: green;
}
.tableau-container {
    display: grid;
    grid-template-columns: repeat(3, 6rem);
    grid-template-rows: repeat(3, 6rem);
    gap: 4px;
    background: var(--marron);
    padding: 6px;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
.case-container {
    height: 6rem;
    width: 6rem;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 2.5rem;
    font-weight: bold;
    cursor: pointer;
    border-radius: 6px;
    background: var(--marron);
    box-shadow: inset 0 0 3px rgba(0, 0, 0, 0.2);
    transition:
        background 0.2s ease,
        transform 0.1s ease;
}
.case-container:hover {
    transform: scale(1.05);
    background: var(--marron-clair);
}
.case-container[data-value="1"] {
    color: #ff4444;
}
.case-container[data-value="2"] {
    color: #4d94ff;
}
.boutton-container {
    margin-top: 1rem;
    display: flex;
    gap: 10px;
}
.game-info {
    margin-top: 1rem;
    font-size: 1.2rem;
}
</style>
