<template>
    <div class="game-container">
        <div class="board-container">
            <div
                v-for="(row, rowIndex) in board"
                :key="rowIndex"
                class="board-row"
            >
                <div
                    v-for="(value, colIndex) in row"
                    :key="colIndex"
                    class="board-cell"
                    :class="`tile-${value}`"
                >
                    {{ value !== 0 ? value : "" }}
                </div>
            </div>
        </div>
        <div class="button-container">
            <button @click="restartGame">Restart</button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue";

const boardSize = 4;
const board = ref<number[][]>([]);

function initializeBoard() {
    board.value = Array.from({ length: boardSize }, () =>
        Array(boardSize).fill(0)
    );

    placeRandomTiles(2);
}

function placeRandomTiles(count: number) {
    for (let k = 0; k < count; k++) {
        const emptyCells = Array.from(
            { length: boardSize * boardSize },
            (_, index) => ({
                row: Math.floor(index / boardSize),
                col: index % boardSize,
            })
        ).filter((cell) => board.value[cell.row][cell.col] === 0);

        if (emptyCells.length > 0) {
            const randomCell =
                emptyCells[Math.floor(Math.random() * emptyCells.length)];
            board.value[randomCell.row][randomCell.col] = 2;
        }
    }
}

function restartGame() {
    initializeBoard();
}

function move(direction: string) {
    const previousBoard = JSON.parse(JSON.stringify(board.value));

    for (let i = 0; i < boardSize; i++) {
        const row = [];
        const mergedRow = [];

        for (let j = 0; j < boardSize; j++) {
            const cell =
                direction === "up" || direction === "down"
                    ? board.value[j][i]
                    : board.value[i][j];

            if (cell !== 0) {
                row.push(cell);
            }
        }

        if (direction === "down" || direction === "right") {
            row.reverse();
        }

        for (let j = 0; j < row.length; j++) {
            if (j < row.length - 1 && row[j] === row[j + 1]) {
                mergedRow.push(row[j] * 2);
                j++;
            } else {
                mergedRow.push(row[j]);
            }
        }

        while (mergedRow.length < boardSize) {
            mergedRow.push(0);
        }

        if (direction === "down" || direction === "right") {
            mergedRow.reverse();
        }

        for (let j = 0; j < boardSize; j++) {
            if (direction === "up" || direction === "down") {
                board.value[j][i] = mergedRow[j];
            } else {
                board.value[i][j] = mergedRow[j];
            }
        }
    }

    if (!isEqual(previousBoard, board.value)) {
        placeRandomTiles(1);
    }
}

function isEqual(arr1: number[][], arr2: number[][]): boolean {
    return JSON.stringify(arr1) === JSON.stringify(arr2);
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

.board-row {
    display: flex;
}

.board-cell {
    width: 100px;
    height: 100px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    font-weight: bold;
    background-color: rgb(205, 193, 180);
    border: 0.3rem solid rgb(187, 173, 160);
    color: rgb(50, 50, 50);
}
.button-container {
    margin-top: 20px;
}

.tile-2 {
    background-color: #f5f5dc;
}

.tile-4 {
    background-color: #f5f5c1;
}

.tile-8 {
    background-color: #f5f59f;
}

.tile-16 {
    background-color: #f5f57d;
}

.tile-32 {
    background-color: #f5f55a;
}

.tile-64 {
    background-color: #eab105;
}

.tile-128 {
    background-color: #f58515;
}

.tile-256 {
    background-color: #f56200;
}

.tile-512 {
    background-color: #d04200;
}

.tile-1024 {
    background-color: #a05b00;
}

.tile-2048 {
    background-color: #802200;
}
</style>
