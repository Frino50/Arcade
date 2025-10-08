<template>
    <div class="body-container">
        <div>
            <div class="header">
                <div>Score {{ score }}</div>
            </div>
        </div>

        <div class="game-board" :style="gameBoardStyle">
            <div
                v-for="(part, index) in snake"
                :key="index"
                class="cell snake"
                :style="{
                    left: part.x * cellSize + 'px',
                    top: part.y * cellSize + 'px',
                }"
            ></div>

            <div
                class="cell food"
                :style="{
                    left: food.x * cellSize + 'px',
                    top: food.y * cellSize + 'px',
                }"
            ></div>
        </div>

        <button class="bouton" @click="startGame">Commencer</button>

        <Message v-if="loose" message="Perdu" :isGreen="false" />
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from "vue";
import Message from "@/components/Message.vue";

const loose = ref(false);
const animationId = ref<number | null>(null);
const gridSize = 35;
const cellSize = 20;
const direction = ref("right");
const snake = ref<Position[]>([{ x: 10, y: 10 }]);
const food = ref<Position>({ x: 1, y: 1 });
const score = ref(0);
const speed = ref(10);
const moveInterval = ref<number>(0);
const directionQueue = ref<string[]>([]);
interface Position {
    x: number;
    y: number;
}

function moveSnake() {
    if (directionQueue.value.length > 0) {
        direction.value = directionQueue.value.shift()!;
    }

    const head = { ...snake.value[0] };
    switch (direction.value) {
        case "right":
            head.x++;
            if (head.x >= gridSize) head.x = 0;
            break;
        case "left":
            head.x--;
            if (head.x < 0) head.x = gridSize - 1;
            break;
        case "up":
            head.y--;
            if (head.y < 0) head.y = gridSize - 1;
            break;
        case "down":
            head.y++;
            if (head.y >= gridSize) head.y = 0;
            break;
    }

    for (let part of snake.value) {
        if (part.x === head.x && part.y === head.y) {
            loose.value = true;
            clearInterval(moveInterval.value);
            return;
        }
    }

    snake.value.unshift(head);

    if (head.x === food.value.x && head.y === food.value.y) {
        placeFood();
        score.value++;
    } else {
        snake.value.pop();
    }
}

function placeFood() {
    food.value.x = Math.floor(Math.random() * gridSize);
    food.value.y = Math.floor(Math.random() * gridSize);
}

function startGame() {
    score.value = 0;
    loose.value = false;
    direction.value = "right";
    placeFood();

    snake.value.splice(0, snake.value.length, { x: 10, y: 10 });

    if (moveInterval.value !== 0) clearInterval(moveInterval.value);

    moveInterval.value = setInterval(() => {
        moveSnake();
        if (!loose.value) {
            animationId.value = requestAnimationFrame(drawSnake);
        }
    }, 1000 / speed.value) as unknown as number;

    animationId.value = requestAnimationFrame(drawSnake);
}

function drawSnake() {
    if (!loose.value) {
        animationId.value = requestAnimationFrame(drawSnake);
    }
}

onMounted(() => window.addEventListener("keydown", handleKeyDown));
onUnmounted(() => {
    window.removeEventListener("keydown", handleKeyDown);
    if (moveInterval.value !== 0) clearInterval(moveInterval.value);
});

function handleKeyDown(event: KeyboardEvent) {
    let newDir = "";
    if (event.key === "ArrowRight") newDir = "right";
    if (event.key === "ArrowLeft") newDir = "left";
    if (event.key === "ArrowUp") newDir = "up";
    if (event.key === "ArrowDown") newDir = "down";

    if (!newDir) return;

    const lastDir =
        directionQueue.value.length > 0
            ? directionQueue.value[directionQueue.value.length - 1]
            : direction.value;

    if (
        (lastDir === "left" && newDir === "right") ||
        (lastDir === "right" && newDir === "left") ||
        (lastDir === "up" && newDir === "down") ||
        (lastDir === "down" && newDir === "up")
    ) {
        return;
    }

    directionQueue.value.push(newDir);
}

const gameBoardStyle = computed(() => ({
    width: `${gridSize * cellSize}px`,
    height: `${gridSize * cellSize}px`,
}));
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

.game-board {
    position: relative;
    background: radial-gradient(
        circle at center,
        var(--futurist-bg-dark),
        var(--futurist-bg-mid) 80%
    );
    box-shadow: 0 0 20px var(--futurist-shadow);
    overflow: hidden;
    border-radius: 6px;
}

.cell {
    position: absolute;
    width: 20px;
    height: 20px;
    border-radius: 6px;
    transition:
        left 0.1s linear,
        top 0.1s linear,
        transform 0.15s ease;
}

.cell.snake {
    background: linear-gradient(
        145deg,
        var(--futurist-accent-light),
        var(--futurist-accent)
    );
    box-shadow:
        inset 0 0 8px var(--futurist-shadow),
        0 0 12px var(--futurist-shadow-strong);
    border: 1px solid var(--futurist-border-strong);
}

.cell.snake:hover {
    transform: scale(1.1);
    box-shadow:
        inset 0 0 12px var(--futurist-shadow-strong),
        0 0 20px var(--futurist-shadow-strong);
}

.cell.food {
    background: linear-gradient(
        145deg,
        var(--futurist-danger),
        var(--futurist-danger-light)
    );
    border-radius: 50%;
}
</style>
