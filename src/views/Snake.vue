<template>
    <div class="body-container">
        <div class="points">{{ point }}</div>
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
        <button @click="startGame">Commencer</button>
        <Message v-if="loose" message="Perdu" :isGreen="false"></Message>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from "vue";
import Message from "@/components/Message.vue";

const loose = ref<boolean>(false);
const animationId = ref<number | null>(null);
const gridSize = 40;
const cellSize = 20;
const direction = ref<string>("right");
const snake = ref<Position[]>([{ x: 10, y: 10 }]);
const food = ref<Position>({ x: 1, y: 1 });
const point = ref<number>(0);
const speed = ref<number>(10);
const moveInterval = ref<number>(0);

interface Position {
    x: number;
    y: number;
}

function moveSnake() {
    const head = { ...snake.value[0] };
    switch (direction.value) {
        case "right":
            head.x++;
            if (head.x >= gridSize) head.x = 0; // Réapparition à gauche quand sort à droite
            break;
        case "left":
            head.x--;
            if (head.x < 0) head.x = gridSize - 1; // Réapparition à droite quand sort à gauche
            break;
        case "up":
            head.y--;
            if (head.y < 0) head.y = gridSize - 1; // Réapparition en bas quand sort en haut
            break;
        case "down":
            head.y++;
            if (head.y >= gridSize) head.y = 0; // Réapparition en haut quand sort en bas
            break;
    }

    if (head.x < 0 || head.x >= gridSize || head.y < 0 || head.y >= gridSize) {
        loose.value = true;
        clearInterval(moveInterval.value);
        return;
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
        point.value++;
    } else {
        snake.value.pop();
    }
}

function placeFood() {
    food.value.x = Math.floor(Math.random() * gridSize);
    food.value.y = Math.floor(Math.random() * gridSize);
}

function startGame() {
    point.value = 0;
    loose.value = false;
    direction.value = "right";
    placeFood();

    snake.value.splice(0, snake.value.length, { x: 10, y: 10 });

    if (moveInterval.value !== 0) {
        clearInterval(moveInterval.value);
    }

    moveInterval.value = setInterval(() => {
        moveSnake();
        if (!loose.value) {
            animationId.value = requestAnimationFrame(() => {
                drawSnake();
            });
        }
    }, 1000 / speed.value);

    animationId.value = requestAnimationFrame(() => {
        drawSnake();
    });
}

function drawSnake() {
    if (!loose.value) {
        animationId.value = requestAnimationFrame(() => {
            drawSnake();
        });
    }
}

onMounted(() => {
    window.addEventListener("keydown", handleKeyDown);
});

onUnmounted(() => {
    window.removeEventListener("keydown", handleKeyDown);
    if (moveInterval.value !== 0) {
        clearInterval(moveInterval.value);
    }
});

function handleKeyDown(event: KeyboardEvent) {
    if (event.key === "ArrowRight" && direction.value !== "left")
        direction.value = "right";
    if (event.key === "ArrowLeft" && direction.value !== "right")
        direction.value = "left";
    if (event.key === "ArrowUp" && direction.value !== "down")
        direction.value = "up";
    if (event.key === "ArrowDown" && direction.value !== "up")
        direction.value = "down";
}

const gameBoardStyle = computed(() => ({
    width: `${gridSize * cellSize}px`,
    height: `${gridSize * cellSize}px`,
}));
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
    font-size: 8rem;
}
.game-board {
    position: relative;
    border: 1px solid #ccc;
}
.cell {
    position: absolute;
    width: 20px;
    height: 20px;
    transition:
        left 0.1s linear,
        top 0.1s linear;
}
.cell.snake {
    background-color: green;
    border-radius: 50%;
}
.cell.food {
    background-color: red;
    border-radius: 50%;
}
button {
    margin-top: 1rem;
}
input[type="range"] {
    margin-top: 0.5rem;
}
</style>
