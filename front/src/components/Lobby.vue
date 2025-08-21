<template>
    <div class="lobby-container">
        <p v-if="!isConnected">Connexion au serveur...</p>
        <div v-else>
            <div class="header">
                <h3>Parties disponibles</h3>
                <button @click="createGame">Créer une partie</button>
            </div>
            <ul v-if="games.length > 0" class="game-list">
                <li v-for="game in games" :key="game.id">
                    <span>Partie {{ game.id.substring(0, 8) }} </span>
                    <span class="game-info">
                        Joueurs: {{ game.players }} / 2 | Spectateurs:
                        {{ game.spectators }}
                    </span>
                    <div class="actions">
                        <button
                            v-if="!game.isFull"
                            @click="$emit('join-game', game.id)"
                        >
                            Rejoindre
                        </button>
                        <button @click="$emit('spectate-game', game.id)">
                            Spectateur
                        </button>
                    </div>
                </li>
            </ul>
            <p v-else class="no-games">Aucune partie disponible.</p>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

type Game = {
    id: string;
    players: number;
    spectators: number;
    isFull: boolean;
};

const games = ref<Game[]>([]);
const isConnected = ref(false);

const emit = defineEmits(["create-game", "join-game", "spectate-game"]);

function createGame() {
    emit("create-game");
}

function updateLobby(availableGames: Game[]) {
    games.value = availableGames;
    isConnected.value = true;
}

defineExpose({
    updateLobby,
    isConnected,
});
</script>

<style scoped>
.lobby-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    background: #2c3e50;
    color: #ecf0f1;
    padding: 2rem;
    border-radius: 10px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
    width: 90%;
    max-width: 600px;
}
.header {
    display: flex;
    justify-content: space-between;
    width: 100%;
    margin-bottom: 1rem;
    gap: 5rem;
}
.game-list {
    width: 100%;
    list-style: none;
    padding: 0;
}
.game-list li {
    background: #34495e;
    padding: 1rem;
    margin-bottom: 10px;
    border-radius: 8px;
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.actions button {
    margin-left: 10px;
}
.no-games {
    font-style: italic;
    color: #95a5a6;
}
</style>
