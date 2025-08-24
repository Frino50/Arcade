<template>
    <div class="body-container">
        <div class="header">
            <div class="scores">
                <div class="score-box">
                    <div>Score</div>
                    <div class="score">{{ score }}</div>
                </div>
                <div class="score-box">
                    <div>Best</div>
                    <div class="score">{{ bestScore }}</div>
                </div>
            </div>
        </div>

        <div
            class="board"
            :style="{
                width: boardSize * cellSize + (boardSize - 1) * gap + 'px',
                height: boardSize * cellSize + (boardSize - 1) * gap + 'px',
                gridTemplateRows: `repeat(${boardSize}, ${cellSize}px)`,
                gridTemplateColumns: `repeat(${boardSize}, ${cellSize}px)`,
                gap: gap + 'px',
            }"
        >
            <div
                v-for="index in boardCells.length"
                :key="index"
                class="board-cell empty"
            ></div>

            <div
                v-for="tile in tiles"
                :key="tile.id"
                class="tile"
                :class="{ merged: tile.merged, new: tile.isNew }"
                :style="{
                    top: tile.row * (cellSize + gap) + 'px',
                    left: tile.col * (cellSize + gap) + 'px',
                    width: cellSize + 'px',
                    height: cellSize + 'px',
                    fontSize: cellSize / 3 + 'px',
                    background: getTileGradient(tile.value),
                    color: getTileTextColor(tile.value),
                }"
            >
                {{ tile.value }}
            </div>
        </div>

        <div>
            <button @click="restartGame">Reset</button>
        </div>
    </div>

    <Leaderboard
        style="position: absolute; left: 10rem; top: 10rem"
        :key="keyLeaderBord"
        :gameName="GAME_NAME"
    />
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue";
import Tile from "@/models/tile.ts";
import SaveRecordDto from "@/models/saveRecordDto.ts";
import { GameType } from "@/models/enums/gameType.ts";
import scoreService from "@/services/scoreService.ts";
import Leaderboard from "../components/LeaderBord.vue";

const boardSize = <number>4;
const gap = <number>5;
const GAME_NAME = GameType.DEUX_MILLE_QUARANTE_HUIT;
let keyLeaderBord = ref<number>(0);
let cellSize = <number>150;

const MOVE_DURATION = 200;

const tiles = ref<Tile[]>([]);
let idCounter = <number>0;
const boardCells = Array.from({ length: boardSize * boardSize });

const score = ref<number>(0);
const bestScore = ref<number>(0);

function createTile(value: number, row: number, col: number): Tile {
    return { id: idCounter++, value, row, col, merged: false, isNew: true };
}
async function getBestScore() {
    try {
        const response = await scoreService.getBestScore(GAME_NAME);
        bestScore.value = response.data;
    } catch (error) {
        console.error("Failed to get record:", error);
    }
}

function getEmptyCells() {
    const filled = tiles.value.map((t) => `${t.row},${t.col}`);
    const empty: { row: number; col: number }[] = [];
    for (let r = 0; r < boardSize; r++) {
        for (let c = 0; c < boardSize; c++) {
            if (!filled.includes(`${r},${c}`)) empty.push({ row: r, col: c });
        }
    }
    return empty;
}

function placeRandomTiles(count: number) {
    const emptyCells = getEmptyCells();
    for (let k = 0; k < count && emptyCells.length > 0; k++) {
        const idx = Math.floor(Math.random() * emptyCells.length);
        const cell = emptyCells.splice(idx, 1)[0];
        const newTile = createTile(2, cell.row, cell.col);
        tiles.value.push(newTile);
        setTimeout(() => {
            newTile.isNew = false;
        }, MOVE_DURATION);
    }
}

function initializeBoard() {
    tiles.value = [];
    score.value = 0;
    placeRandomTiles(2);
}

function restartGame() {
    saveCurrentRecord();
    initializeBoard();
}

async function saveCurrentRecord() {
    if (score.value <= 0) {
        return;
    }
    const recordData = new SaveRecordDto(GAME_NAME, score.value);
    try {
        const response = await scoreService.saveRecord(recordData);
        bestScore.value = response.data.score;
        keyLeaderBord.value++;
    } catch (error) {
        console.error("Failed to save record:", error);
    }
}

function move(direction: string) {
    let moved = false;
    tiles.value.forEach((t) => (t.merged = false));

    const sorted = [...tiles.value].sort((a, b) => {
        if (direction === "up") return a.row - b.row;
        if (direction === "down") return b.row - a.row;
        if (direction === "left") return a.col - b.col;
        if (direction === "right") return b.col - a.col;
        return 0;
    });

    for (const tile of sorted) {
        let { row, col } = tile;
        while (true) {
            let nextRow = row;
            let nextCol = col;

            if (direction === "up") nextRow--;
            if (direction === "down") nextRow++;
            if (direction === "left") nextCol--;
            if (direction === "right") nextCol++;

            if (
                nextRow < 0 ||
                nextRow >= boardSize ||
                nextCol < 0 ||
                nextCol >= boardSize
            )
                break;

            const other = tiles.value.find(
                (t) => t.row === nextRow && t.col === nextCol
            );

            if (!other) {
                row = nextRow;
                col = nextCol;
                moved = true;
            } else if (
                other.value === tile.value &&
                !other.merged &&
                !tile.merged
            ) {
                other.value *= 2;
                score.value += other.value;

                other.merged = true;
                tiles.value = tiles.value.filter((t) => t.id !== tile.id);
                moved = true;
                break;
            } else {
                break;
            }
        }

        tile.row = row;
        tile.col = col;
    }

    if (moved) {
        setTimeout(() => {
            placeRandomTiles(1);
            checkGameOver();
        }, MOVE_DURATION);
    }
}

function checkGameOver() {
    if (getEmptyCells().length > 0) return;

    for (let r = 0; r < boardSize; r++) {
        for (let c = 0; c < boardSize; c++) {
            const tile = tiles.value.find((t) => t.row === r && t.col === c);
            if (!tile) continue;
            const neighbors = [
                tiles.value.find((t) => t.row === r + 1 && t.col === c),
                tiles.value.find((t) => t.row === r && t.col === c + 1),
            ];
            if (neighbors.some((n) => n && n.value === tile.value)) return;
        }
    }
    saveCurrentRecord();
    // alert("Game over!");
}

function handleKeyPress(event: KeyboardEvent) {
    switch (event.key) {
        case "ArrowUp":
            move("up");
            break;
        case "ArrowDown":
            move("down");
            break;
        case "ArrowLeft":
            move("left");
            break;
        case "ArrowRight":
            move("right");
            break;
    }
}

function updateCellSize() {
    const maxBoardWidth = Math.min(window.innerWidth * 0.9, 600);
    cellSize = Math.floor((maxBoardWidth - gap * (boardSize - 1)) / boardSize);
}

onMounted(() => {
    updateCellSize();
    window.addEventListener("resize", updateCellSize);
    initializeBoard();
    window.addEventListener("keydown", handleKeyPress);
    updateCellSize();
    getBestScore();
});

onBeforeUnmount(() => {
    saveCurrentRecord();
    window.removeEventListener("keydown", handleKeyPress);
    window.removeEventListener("resize", updateCellSize);
});

function getTileGradient(value: number): string {
    switch (value) {
        case 2:
            return "linear-gradient(145deg, #eee4da, #f3e1c4)";
        case 4:
            return "linear-gradient(145deg, #ede0c8, #f5e8c0)";
        case 8:
            return "linear-gradient(145deg, #f2b179, #f2a679)";
        case 16:
            return "linear-gradient(145deg, #f59563, #f58653)";
        case 32:
            return "linear-gradient(145deg, #f67c5f, #f56b50)";
        case 64:
            return "linear-gradient(145deg, #f65e3b, #f5512e)";
        case 128:
            return "linear-gradient(145deg, #edcf72, #edc75a)";
        case 256:
            return "linear-gradient(145deg, #edcc61, #edc54a)";
        case 512:
            return "linear-gradient(145deg, #edc850, #edc639)";
        case 1024:
            return "linear-gradient(145deg, #edc53f, #edc22e)";
        case 2048:
            return "linear-gradient(145deg, #edc22e, #edbf1e)";
        default:
            return "linear-gradient(145deg, #3c3a32, #4c4a42)";
    }
}

function getTileTextColor(value: number): string {
    return value <= 4 ? "#776e65" : "#fff";
}
</script>

<style scoped>
/* Conteneur principal qui aligne le jeu et le classement */
.page-container {
    display: flex;
    justify-content: center;
    align-items: flex-start;
    gap: 30px;
    padding: 20px;
}

.game-container {
    display: flex;
    flex-direction: column;
    align-items: center;
}

.header {
    display: flex;
    justify-content: center;
}

.header h1 {
    font-size: 2.5rem;
    margin: 0;
}

.scores {
    display: flex;
    gap: 10px;
}

.score-box {
    background: var(--marron-fonce);
    padding: 10px;
    border-radius: 6px;
    text-align: center;
}

.score {
    font-weight: bold;
    font-size: 1.2rem;
}

.board {
    position: relative;
    background: var(--marron);
    border-radius: 8px;
    display: grid;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.board-cell.empty {
    width: 100%;
    height: 100%;
    background: var(--marron-clair);
    border-radius: 6px;
}

.tile {
    position: absolute;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    border-radius: 6px;
    transition: all 0.25s ease;
    z-index: 10;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.tile.merged {
    animation: pop 0.3s ease;
}

.tile.new {
    animation: appear 0.35s ease;
}

@keyframes pop {
    0% {
        transform: scale(1);
    }
    50% {
        transform: scale(1.1);
    }
    100% {
        transform: scale(1);
    }
}

@keyframes appear {
    0% {
        transform: scale(0.5);
        opacity: 0;
    }
    100% {
        transform: scale(1);
        opacity: 1;
    }
}
</style>
