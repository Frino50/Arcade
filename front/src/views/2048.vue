<template>
    <GameLayout :game-name="GAME_NAME" ref="gameLayoutRef">
        <template #header>
            <div>
                <div>Score</div>
                <div>{{ score }}</div>
            </div>
        </template>

        <template #default>
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

                <div v-for="tile in tiles" :key="tile.id">
                    <div
                        class="tile"
                        :class="{ merged: tile.merged, new: tile.isNew }"
                        :style="{
                            top: tile.row * (cellSize + gap) + 'px',
                            left: tile.col * (cellSize + gap) + 'px',
                            width: cellSize + 'px',
                            height: cellSize + 'px',
                            fontSize: cellSize / 3 + 'px',
                            background: getTileGradient(tile.value),
                            transition: `all ${animationSpeed}ms ease`,
                        }"
                    >
                        {{ tile.value }}
                    </div>
                </div>
            </div>
            <div>
                <button @click="restartGame">Reset</button>
            </div>
        </template>
    </GameLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue";
import Tile from "@/models/tile.ts";
import SaveRecordDto from "@/models/dtos/saveRecordDto.ts";
import { GameType } from "@/models/enums/gameType.ts";
import scoreService from "@/services/scoreService.ts";
import GameLayout from "@/components/GameLayout.vue";

const boardSize = 4;
const gap = 5;
const GAME_NAME = GameType.DEUX_MILLE_QUARANTE_HUIT;
const gameLayoutRef = ref<InstanceType<typeof GameLayout>>();
const score = ref(0);
const tiles = ref<Tile[]>([]);
const boardCells = Array.from({ length: boardSize * boardSize });
let idCounter = 0;
let cellSize = 150;

let lastMoveTime = 0;
const MIN_SPEED = 80;
const MAX_SPEED = 250;
const animationSpeed = ref(MAX_SPEED);

function createTile(value: number, row: number, col: number): Tile {
    return { id: idCounter++, value, row, col, merged: false, isNew: true };
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
        setTimeout(() => (newTile.isNew = false), animationSpeed.value);
    }
}

function initializeBoard() {
    tiles.value = [];
    score.value = 0;
    placeRandomTiles(2);
}

async function restartGame() {
    await saveCurrentRecord();
    initializeBoard();
}

async function saveCurrentRecord() {
    if (score.value <= 0) return;
    const recordData = new SaveRecordDto(GAME_NAME, score.value);
    try {
        await scoreService.saveRecord(recordData);
        gameLayoutRef.value?.refresh();
    } catch {}
}

function move(direction: string) {
    const now = Date.now();
    const delta = now - lastMoveTime;

    animationSpeed.value =
        delta < MAX_SPEED ? Math.max(MIN_SPEED, delta) : MAX_SPEED;
    lastMoveTime = now;

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
            let nextRow = row,
                nextCol = col;

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
            } else break;
        }
        tile.row = row;
        tile.col = col;
    }

    if (moved) {
        placeRandomTiles(1);
    }
    checkGameOver();
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
    window.addEventListener("keydown", handleKeyPress);
    initializeBoard();
});

onBeforeUnmount(() => {
    saveCurrentRecord();
    window.removeEventListener("keydown", handleKeyPress);
    window.removeEventListener("resize", updateCellSize);
});

function getTileGradient(value: number) {
    return `radial-gradient(circle at top left, var(--tile-${value}-color), var(--tile-${value}-color))`;
}
</script>

<style scoped>
.board {
    position: relative;
    display: grid;
    border-radius: 1rem;
    overflow: hidden;
}

.board-cell.empty {
    width: 100%;
    height: 100%;
    background: var(--futurist-input-bg);
    border-radius: 10px;
    border: 1px solid rgba(0, 255, 255, 0.1);
    box-shadow: inset 0 0 8px rgba(0, 255, 255, 0.05);
}

.tile {
    position: absolute;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    border-radius: 10px;
    z-index: 10;
    text-shadow: 0 0 8px rgba(255, 255, 255, 0.4);
    animation: pulse-glow 4s ease-in-out infinite;
    color: #fff;
}

.tile.merged {
    animation: pop 0.3s ease;
    box-shadow:
        0 0 20px var(--futurist-shadow-strong),
        inset 0 0 12px rgba(255, 255, 255, 0.3);
}

.tile.new {
    animation: appear 0.4s ease;
    box-shadow:
        0 0 18px var(--futurist-shadow-strong),
        inset 0 0 12px rgba(255, 255, 255, 0.2);
}

@keyframes pop {
    0% {
        transform: scale(1);
    }
    50% {
        transform: scale(1.12);
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

@keyframes pulse-glow {
    0%,
    100% {
        box-shadow:
            0 0 25px var(--futurist-shadow),
            inset 0 0 30px rgba(0, 255, 255, 0.08);
    }
    50% {
        box-shadow:
            0 0 45px var(--futurist-shadow-strong),
            inset 0 0 40px rgba(0, 255, 255, 0.15);
    }
}
</style>
