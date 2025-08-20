<template>
    <div class="game-container">
        <div class="board">
            <div
                v-for="index in boardCells.length"
                :key="index"
                class="board-cell empty"
            ></div>

            <div
                v-for="tile in tiles"
                :key="tile.id"
                class="tile"
                :class="[`tile-${tile.value}`, { merged: tile.merged }]"
                :style="{
                    top: tile.row * (cellSize + gap) + 'px',
                    left: tile.col * (cellSize + gap) + 'px',
                }"
            >
                {{ tile.value }}
            </div>
        </div>

        <div class="button-container">
            <button @click="restartGame">Reset</button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue";
import Tile from "@/models/tile.ts";

const boardSize = 4;
const cellSize = 100;
const gap = 5;
const MOVE_DURATION = 200;

const tiles = ref<Tile[]>([]);
let idCounter = 0;

const boardCells = Array.from({ length: boardSize * boardSize });

function createTile(value: number, row: number, col: number): Tile {
    return { id: idCounter++, value, row, col, merged: false };
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
        tiles.value.push(createTile(2, cell.row, cell.col));
    }
}

function initializeBoard() {
    tiles.value = [];
    placeRandomTiles(2);
}

function restartGame() {
    initializeBoard();
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
        }, MOVE_DURATION);
    }
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

onMounted(() => {
    initializeBoard();
    window.addEventListener("keydown", handleKeyPress);
});

onBeforeUnmount(() => {
    window.removeEventListener("keydown", handleKeyPress);
});
</script>

<style scoped>
.game-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100vh;
}

.board {
    position: relative;
    width: calc(4 * 100px + 3 * 5px);
    height: calc(4 * 100px + 3 * 5px);
    background: #bbada0;
    border-radius: 8px;
    display: grid;
    grid-template-rows: repeat(4, 100px);
    grid-template-columns: repeat(4, 100px);
    gap: 5px;
    /* padding supprimé */
}

.board-cell.empty {
    width: 100%;
    height: 100%;
    background: #cdc1b4;
    border-radius: 6px;
}

.tile {
    position: absolute;
    width: 100px;
    height: 100px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    font-weight: bold;
    color: #333;
    border-radius: 6px;
    transition: all 0.2s ease-in-out;
    z-index: 10;
}

.tile.merged {
    animation: pop 0.2s ease;
}

@keyframes pop {
    0% {
        transform: scale(1);
    }
    50% {
        transform: scale(1.2);
    }
    100% {
        transform: scale(1);
    }
}

/* Couleurs */
.tile-2 {
    background: #eee4da;
}
.tile-4 {
    background: #ede0c8;
}
.tile-8 {
    background: #f2b179;
    color: #fff;
}
.tile-16 {
    background: #f59563;
    color: #fff;
}
.tile-32 {
    background: #f67c5f;
    color: #fff;
}
.tile-64 {
    background: #f65e3b;
    color: #fff;
}
.tile-128 {
    background: #edcf72;
    color: #fff;
}
.tile-256 {
    background: #edcc61;
    color: #fff;
}
.tile-512 {
    background: #edc850;
    color: #fff;
}
.tile-1024 {
    background: #edc53f;
    color: #fff;
}
.tile-2048 {
    background: #edc22e;
    color: #fff;
}

.button-container {
    margin-top: 20px;
}
</style>
