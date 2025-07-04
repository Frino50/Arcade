<template>
    <div class="body-container">
        <div class="points">{{ points }}</div>
        <div class="game-board" :style="gameBoardStyle">
            <div
                v-for="(cell, index) in displayedBoard"
                :key="index"
                class="cell"
                :class="getCellClass(cell)"
                :style="{
                    left: (index % cols) * cellSize + 'px',
                    top: Math.floor(index / cols) * cellSize + 'px',
                }"
            ></div>
        </div>
        <button @click="startGame">Commencer</button>
        <Message v-if="gameOver" message="Perdu" :isGreen="false"></Message>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import Message from "@/components/Message.vue";

const rows = 20;
const cols = 10;
const cellSize = 30;
const speed = ref<number>(500);

const gameBoard = ref<number[]>(Array(rows * cols).fill(0));
const currentPiece = ref<number[][]>([]);
const currentPosition = ref<{ x: number; y: number }>({ x: 0, y: 0 });
const points = ref<number>(0);
const gameOver = ref<boolean>(false);
const currentPieceType = ref<number>(0);

const pieces = [
    { forme: [[1, 1, 1, 1]], color: 1 }, // I
    {
        forme: [
            [1, 1, 0],
            [0, 1, 1],
        ],
        color: 2,
    }, // Z
    {
        forme: [
            [0, 1, 1],
            [1, 1, 0],
        ],
        color: 3,
    }, // S
    {
        forme: [
            [1, 1],
            [1, 1],
        ],
        color: 4,
    }, // O
    {
        forme: [
            [1, 0, 0],
            [1, 1, 1],
        ],
        color: 5,
    }, // L
    {
        forme: [
            [0, 0, 1],
            [1, 1, 1],
        ],
        color: 6,
    }, // J
    {
        forme: [
            [0, 1, 0],
            [1, 1, 1],
        ],
        color: 7,
    }, // T
];

const displayedBoard = computed(() => {
    const boardCopy = gameBoard.value.slice();
    currentPiece.value.forEach((row, dy) =>
        row.forEach((cell, dx) => {
            if (cell) {
                const x = currentPosition.value.x + dx;
                const y = currentPosition.value.y + dy;
                if (x >= 0 && x < cols && y < rows) {
                    boardCopy[y * cols + x] = currentPieceType.value;
                }
            }
        })
    );
    return boardCopy;
});

function startGame() {
    points.value = 0;
    gameOver.value = false;
    gameBoard.value = Array(rows * cols).fill(0);
    spawnPiece();
    dropPiece();
}

function spawnPiece() {
    const piece = pieces[Math.floor(Math.random() * pieces.length)];
    currentPiece.value = piece.forme;
    currentPieceType.value = piece.color;
    currentPosition.value = { x: Math.floor(cols / 2) - 1, y: 0 };
    if (!isValidMove(currentPiece.value, currentPosition.value)) {
        gameOver.value = true;
    }
}

function dropPiece() {
    if (gameOver.value) return;
    if (!movePiece(0, 1)) {
        lockPiece();
        clearLines();
        spawnPiece();
    }
    setTimeout(dropPiece, speed.value);
}

function movePiece(dx: number, dy: number): boolean {
    const newPosition = {
        x: currentPosition.value.x + dx,
        y: currentPosition.value.y + dy,
    };
    if (isValidMove(currentPiece.value, newPosition)) {
        currentPosition.value = newPosition;
        return true;
    }
    return false;
}

function rotatePiece() {
    // Le _ veut dire qu'on ignore la valeur et que seul l'index nous intéresse
    const rotatedPiece = currentPiece.value[0].map((_, i) =>
        currentPiece.value.map((row) => row[i]).reverse()
    );
    if (isValidMove(rotatedPiece, currentPosition.value)) {
        currentPiece.value = rotatedPiece;
    }
}

function isValidMove(
    piece: number[][],
    position: { x: number; y: number }
): boolean {
    return piece.every((row, dy) =>
        row.every((cell, dx) => {
            const x = position.x + dx;
            const y = position.y + dy;
            return (
                cell === 0 ||
                (x >= 0 &&
                    x < cols &&
                    y < rows &&
                    gameBoard.value[y * cols + x] === 0)
            );
        })
    );
}

function lockPiece() {
    currentPiece.value.forEach((row, dy) =>
        row.forEach((cell, dx) => {
            if (cell) {
                gameBoard.value[
                    (currentPosition.value.y + dy) * cols +
                        currentPosition.value.x +
                        dx
                ] = currentPieceType.value;
            }
        })
    );
}

function clearLines() {
    let newBoard = gameBoard.value.slice();
    let linesCleared = 0;
    for (let y = 0; y < rows; y++) {
        const row = newBoard.slice(y * cols, y * cols + cols);
        if (row.every((cell) => cell !== 0)) {
            newBoard.splice(y * cols, cols);
            newBoard.unshift(...Array(cols).fill(0));
            linesCleared++;
        }
    }
    gameBoard.value = newBoard;
    points.value += linesCleared * 100;
}

function handleKeyDown(event: KeyboardEvent) {
    if (gameOver.value) return;
    if (event.key === "ArrowLeft") movePiece(-1, 0);
    if (event.key === "ArrowRight") movePiece(1, 0);
    if (event.key === "ArrowDown") movePiece(0, 1);
    if (event.key === "ArrowUp") rotatePiece();
}

function getCellClass(cell: number) {
    return {
        1: "color-i",
        2: "color-z",
        3: "color-s",
        4: "color-o",
        5: "color-l",
        6: "color-j",
        7: "color-t",
    }[cell];
}

const gameBoardStyle = computed(() => ({
    width: `${cols * cellSize}px`,
    height: `${rows * cellSize}px`,
}));

onMounted(() => {
    window.addEventListener("keydown", handleKeyDown);
});

onUnmounted(() => {
    window.removeEventListener("keydown", handleKeyDown);
});
</script>

<style scoped>
.body-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
}
.points {
    position: absolute;
    left: 15rem;
    font-size: 2rem;
}
.game-board {
    position: relative;
    border: 1px solid #ccc;
}
.cell {
    position: absolute;
    width: 30px;
    height: 30px;
    border: 1px solid #333;
}
.color-i {
    background-color: cyan;
}
.color-z {
    background-color: red;
}
.color-s {
    background-color: green;
}
.color-o {
    background-color: yellow;
}
.color-l {
    background-color: orange;
}
.color-j {
    background-color: blue;
}
.color-t {
    background-color: purple;
}
button {
    margin-top: 1rem;
}
</style>
