<template>
    <div class="body-container">
        <div>
            <div class="header">
                <div>Score {{ score }}</div>
            </div>
        </div>
        <div class="game-container">
            <div :style="gameBoardStyle">
                <div
                    v-for="(cell, index) in displayedBoard"
                    :key="index"
                    class="cell"
                    :class="getCellClass(cell)"
                    :style="{
                        width: cellSize + 'px',
                        height: cellSize + 'px',
                        left: (index % cols) * cellSize + 6 + 'px',
                        top: Math.floor(index / cols) * cellSize + 6 + 'px',
                    }"
                ></div>
            </div>

            <div class="next-container">
                <div class="next-piece-container">
                    <div class="next-piece" :style="nextPieceStyle">
                        <template v-for="(row, y) in nextPiece">
                            <template v-for="(cell, x) in row">
                                <div
                                    v-if="cell"
                                    :key="y + '-' + x"
                                    class="preview-cell"
                                    :class="getCellClass(nextPieceType)"
                                    :style="{
                                        width: previewCellSize + 'px',
                                        height: previewCellSize + 'px',
                                    }"
                                ></div>
                            </template>
                        </template>
                    </div>
                </div>
            </div>
        </div>

        <button class="bouton" @click="startGame">Commencer</button>
        <Message v-if="gameOver" message="Perdu" :isGreen="false"></Message>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import Message from "@/components/Message.vue";

const rows = 20;
const cols = 10;
const cellSize = 35;
const speed = ref<number>(500);
const previewCellSize = computed(() => Math.floor(cellSize * 0.7));
const gameBoard = ref<number[]>(Array(rows * cols).fill(0));
const currentPiece = ref<number[][]>([]);
const currentPosition = ref<{ x: number; y: number }>({ x: 0, y: 0 });
const score = ref<number>(0);
const gameOver = ref<boolean>(false);
const currentPieceType = ref<number>(0);

const nextPiece = ref<number[][]>([]);
const nextPieceType = ref<number>(0);

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
    score.value = 0;
    gameOver.value = false;
    gameBoard.value = Array(rows * cols).fill(0);
    const first = pieces[Math.floor(Math.random() * pieces.length)];
    nextPiece.value = first.forme;
    nextPieceType.value = first.color;
    spawnPiece();
    dropPiece();
}

function spawnPiece() {
    currentPiece.value = nextPiece.value;
    currentPieceType.value = nextPieceType.value;

    const piece = pieces[Math.floor(Math.random() * pieces.length)];
    nextPiece.value = piece.forme;
    nextPieceType.value = piece.color;

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

    if (linesCleared === 1) score.value += 100;
    else if (linesCleared === 2) score.value += 300;
    else if (linesCleared === 3) score.value += 500;
    else if (linesCleared === 4) score.value += 800;
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

const nextPieceStyle = computed(() => {
    if (!nextPiece.value.length) return {};
    const width = Math.max(...nextPiece.value.map((row) => row.length));
    const height = nextPiece.value.length;
    return {
        display: "grid",
        gridTemplateColumns: `repeat(${width}, 20px)`,
        gridTemplateRows: `repeat(${height}, 20px)`,
        gap: "2px",
    };
});

onMounted(() => {
    window.addEventListener("keydown", handleKeyDown);
});

onUnmounted(() => {
    window.removeEventListener("keydown", handleKeyDown);
});
</script>

<style scoped>
.header {
    padding: 0.8rem 1.2rem;
    border-radius: 8px;
    text-align: center;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 3rem;
    font-weight: bold;
    font-size: 1.1rem;
    color: var(--futurist-accent);
    text-shadow: 0 0 8px var(--futurist-shadow-strong);
    position: relative;
}

.header > * {
    background: var(--futurist-blur-bg);
    padding: 0.8rem 1rem;
    border-radius: 10px;
    border: 1px solid var(--futurist-border);
    box-shadow: 0 0 12px var(--futurist-shadow);
    transition:
        transform 0.25s ease,
        box-shadow 0.25s ease;
}

.header > *:hover {
    transform: scale(1.05);
    box-shadow: 0 0 20px var(--futurist-shadow-strong);
}

.game-container {
    position: relative;
    display: flex;
    justify-content: center;
}

.next-container {
    position: absolute;
    left: 100%;
    margin-left: 2rem;
    top: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.5rem;
}

.next-piece-container {
    width: 100px;
    height: 100px;
    box-shadow: 0 4px 8px var(--futurist-shadow);
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--futurist-blur-bg);
    padding: 0.8rem 1rem;
    border-radius: 10px;
    border: 1px solid var(--futurist-border);
}

.cell {
    position: absolute;
    width: 30px;
    height: 30px;
    border-radius: 4px;
    box-shadow: inset 0 0 3px var(--futurist-shadow);
    background: var(--futurist-bg-dark);
}

.preview-cell {
    width: 20px;
    height: 20px;
    border-radius: 3px;
    box-shadow: inset 0 0 2px var(--futurist-shadow);
}

.color-s {
    background: linear-gradient(
        145deg,
        var(--piece-s-light),
        var(--piece-s-dark)
    );
    box-shadow:
        inset 0 0 5px var(--piece-s-light),
        0 0 10px var(--piece-s-dark);
}

.color-l {
    background: linear-gradient(
        145deg,
        var(--piece-l-light),
        var(--piece-l-dark)
    );
    box-shadow:
        inset 0 0 5px var(--piece-l-light),
        0 0 10px var(--piece-l-dark);
}

.color-j {
    background: linear-gradient(
        145deg,
        var(--piece-j-light),
        var(--piece-j-dark)
    );
    box-shadow:
        inset 0 0 5px var(--piece-j-light),
        0 0 10px var(--piece-j-dark);
}

.color-t {
    background: linear-gradient(
        145deg,
        var(--piece-t-light),
        var(--piece-t-dark)
    );
    box-shadow:
        inset 0 0 5px var(--piece-t-light),
        0 0 10px var(--piece-t-dark);
}
.color-o {
    background: linear-gradient(
        145deg,
        var(--piece-o-light),
        var(--piece-o-dark)
    );
    box-shadow:
        inset 0 0 5px var(--piece-o-light),
        0 0 10px var(--piece-o-dark);
}
</style>
