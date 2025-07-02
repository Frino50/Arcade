<template>
    <PopUp
        v-model="dialogParametre"
        title="Paramètres"
        width="300"
        :initial-position="{ x: 15, y: 15 }"
    >
        <Parametres
            v-model="listBacterie"
            @nombreMax="nombreMax = $event"
            @bacterieStart="bacterieStart = $event"
            @vitessePropagation="vitessePropagation = $event"
            @vitesseDeplacement="vitesseDeplacement = $event"
            @dialogGenerale="dialogGenerale = $event"
            @start="start()"
        ></Parametres>
    </PopUp>

    <PopUp
        v-model="dialogGenerale"
        title="Détails de la bactérie"
        width="500"
        :initial-position="{ x: 300, y: 15 }"
    >
        <StatsGenerale v-model="listBacterie" />
    </PopUp>

    <PopUp
        v-model="dialogIndividuel"
        title="Détails de la bactérie"
        width="500"
        :initial-position="{ x: 890, y: 15 }"
    >
        <StatsIndividuelle v-model="bacterieSelected" />
    </PopUp>

    <canvas ref="canvasRef" class="bacterie-canvas"></canvas>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue";
import { Color } from "@/models/enums/color.ts";
import Bacterie from "../models/bacterie.ts";
import StatsIndividuelle from "@/components/StatsIndividuelle.vue";
import StatsGenerale from "@/components/StatsGenerale.vue";
import PopUp from "@/components/PopUp.vue";
import Parametres from "@/components/Parametres.vue";

const listBacterie = ref<Bacterie[]>([]);
const bacterieSelected = ref<Bacterie>();
const vitesseDeplacement = ref<number>(0);
const vitessePropagation = ref<number>(0);
const bacterieStart = ref<number>(0);
const nombreMax = ref<number>(0);
const gridSize = ref<number>(20);
const dialogGenerale = ref<boolean>(false);
const dialogIndividuel = ref<boolean>(false);
const dialogParametre = ref<boolean>(true);
const canvasRef = ref<HTMLCanvasElement | null>(null);
let animationFrameId: number | null = null;
let intervalDeplacement: number | undefined;
let intervalPropagation: number | undefined;
let intervalBaisserVie: number | undefined;
let ctx: CanvasRenderingContext2D | null = null;
let canvasWidth = 0;
let canvasHeight = 0;
let lastDeplacement = 0;
let lastPropagation = 0;
let lastBaisseVie = 0;

const spatialGrid: { [key: string]: Set<Bacterie> } = {};

onMounted(() => {
    if (canvasRef.value) {
        ctx = canvasRef.value.getContext("2d");
        updateCanvasSize();
        window.addEventListener("resize", updateCanvasSize);

        canvasRef.value.addEventListener("click", handleCanvasClick);
    }
});

onBeforeUnmount(() => {
    if (animationFrameId !== null) {
        cancelAnimationFrame(animationFrameId);
    }
    clearAllIntervals();
    if (canvasRef.value) {
        canvasRef.value.removeEventListener("click", handleCanvasClick);
    }
    window.removeEventListener("resize", updateCanvasSize);
});

function updateCanvasSize() {
    if (!canvasRef.value) return;

    canvasWidth = window.innerWidth;
    canvasHeight = window.innerHeight;
    canvasRef.value.width = canvasWidth;
    canvasRef.value.height = canvasHeight;

    if (listBacterie.value.length > 0) {
        for (const bacterie of listBacterie.value) {
            bacterie.position.x = Math.min(
                bacterie.position.x,
                canvasWidth - bacterie.taille
            );
            bacterie.position.y = Math.min(
                bacterie.position.y,
                canvasHeight - bacterie.taille
            );
        }
        updateSpatialGrid();
    }
}

function handleCanvasClick(event: MouseEvent) {
    const rect = canvasRef.value!.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    const gridKey = getGridKey(x, y);
    const nearbyGrids = getNearbyGridKeys(gridKey);

    let closestBacterie: Bacterie | null = null;
    let minDistance = Infinity;

    for (const key of nearbyGrids) {
        if (spatialGrid[key]) {
            for (const bacterie of spatialGrid[key]) {
                const distance = getDistance(
                    x,
                    y,
                    bacterie.position.x + bacterie.taille / 2,
                    bacterie.position.y + bacterie.taille / 2
                );

                if (distance <= bacterie.taille / 2 && distance < minDistance) {
                    minDistance = distance;
                    closestBacterie = bacterie;
                }
            }
        }
    }

    if (closestBacterie) {
        bacterieSelected.value = closestBacterie;
        dialogIndividuel.value = true;
    }
}

function getDistance(x1: number, y1: number, x2: number, y2: number): number {
    const dx = x2 - x1;
    const dy = y2 - y1;
    return Math.sqrt(dx * dx + dy * dy);
}

function initialisationList() {
    for (let i = 0; i < bacterieStart.value; i++) {
        let colorIndex;
        colorIndex = Math.floor(Math.random() * Object.values(Color).length);
        const taille = Math.random() * 10 + 5;

        listBacterie.value.push(
            new Bacterie(
                Math.random() * (canvasWidth - taille),
                Math.random() * (canvasHeight - taille),
                Object.values(Color)[colorIndex],
                1,
                taille,
                15,
                1,
                0
            )
        );
    }

    updateSpatialGrid();
}

// Le reste du code reste inchangé
function updateSpatialGrid() {
    for (const key in spatialGrid) {
        delete spatialGrid[key];
    }

    for (const bacterie of listBacterie.value) {
        const key = getGridKey(bacterie.position.x, bacterie.position.y);
        if (!spatialGrid[key]) {
            spatialGrid[key] = new Set();
        }
        spatialGrid[key].add(bacterie);
    }
}

function getGridKey(x: number, y: number): string {
    const gridX = Math.floor(x / gridSize.value);
    const gridY = Math.floor(y / gridSize.value);
    return `${gridX},${gridY}`;
}

function getNearbyGridKeys(key: string): string[] {
    const [gridX, gridY] = key.split(",").map(Number);
    const keys: string[] = [];

    for (let i = -1; i <= 1; i++) {
        for (let j = -1; j <= 1; j++) {
            keys.push(`${gridX + i},${gridY + j}`);
        }
    }

    return keys;
}

function gameLoop(timestamp: number) {
    const deltaTime = timestamp - lastDeplacement;
    const deltaPropagation = timestamp - lastPropagation;
    const deltaBaisseVie = timestamp - lastBaisseVie;

    if (deltaTime >= vitesseDeplacement.value) {
        deplacement();
        lastDeplacement = timestamp;
    }

    if (deltaPropagation >= vitessePropagation.value) {
        propagation();
        lastPropagation = timestamp;
    }

    if (deltaBaisseVie >= 1000) {
        baisserVie();
        lastBaisseVie = timestamp;
    }

    renderBacteries();

    animationFrameId = requestAnimationFrame(gameLoop);
}

function renderBacteries() {
    if (!ctx) return;

    ctx.clearRect(0, 0, canvasWidth, canvasHeight);

    for (const bacterie of listBacterie.value) {
        ctx.beginPath();
        ctx.arc(
            bacterie.position.x + bacterie.taille / 2,
            bacterie.position.y + bacterie.taille / 2,
            bacterie.taille / 2,
            0,
            Math.PI * 2
        );

        ctx.fillStyle = bacterie.color;
        ctx.fill();

        ctx.shadowBlur = 20;
        ctx.shadowColor = bacterie.color;
        ctx.strokeStyle = bacterie.color;
        ctx.lineWidth = 2;
        ctx.stroke();
        ctx.shadowBlur = 0;
    }
}

function deplacement() {
    for (const bacterie of listBacterie.value) {
        const oldGridKey = getGridKey(bacterie.position.x, bacterie.position.y);

        const newX = bacterie.position.x + (Math.random() * 2 - 1);
        const newY = bacterie.position.y + (Math.random() * 2 - 1);

        bacterie.position.x = Math.max(
            0,
            Math.min(newX, canvasWidth - bacterie.taille)
        );
        bacterie.position.y = Math.max(
            0,
            Math.min(newY, canvasHeight - bacterie.taille)
        );

        if (
            bacterie.position.x <= 0 ||
            bacterie.position.x >= canvasWidth - bacterie.taille
        ) {
            bacterie.position.x += bacterie.position.x <= 0 ? 0.5 : -0.5;
        }

        if (
            bacterie.position.y <= 0 ||
            bacterie.position.y >= canvasHeight - bacterie.taille
        ) {
            bacterie.position.y += bacterie.position.y <= 0 ? 0.5 : -0.5;
        }

        const newGridKey = getGridKey(bacterie.position.x, bacterie.position.y);
        if (oldGridKey !== newGridKey) {
            if (spatialGrid[oldGridKey]) {
                spatialGrid[oldGridKey].delete(bacterie);
            }

            if (!spatialGrid[newGridKey]) {
                spatialGrid[newGridKey] = new Set();
            }
            spatialGrid[newGridKey].add(bacterie);
        }
    }

    checkCollisions();
}

function checkCollisions() {
    for (const key in spatialGrid) {
        if (!spatialGrid[key] || spatialGrid[key].size <= 1) continue;

        const bacteries = Array.from(spatialGrid[key]);

        for (let i = 0; i < bacteries.length; i++) {
            for (let j = i + 1; j < bacteries.length; j++) {
                if (detecterChevauchement(bacteries[i], bacteries[j])) {
                    repousserChevauchements(bacteries[i], bacteries[j]);
                }
            }
        }

        const nearbyKeys = getNearbyGridKeys(key);
        for (const nearbyKey of nearbyKeys) {
            if (nearbyKey === key || !spatialGrid[nearbyKey]) continue;

            const nearbyBacteries = Array.from(spatialGrid[nearbyKey]);

            for (const bacterie of bacteries) {
                for (const nearbyBacterie of nearbyBacteries) {
                    if (detecterChevauchement(bacterie, nearbyBacterie)) {
                        repousserChevauchements(bacterie, nearbyBacterie);
                    }
                }
            }
        }
    }
}

function detecterChevauchement(bacterieA: Bacterie, bacterieB: Bacterie) {
    const deltaX = bacterieA.position.x - bacterieB.position.x;
    const deltaY = bacterieA.position.y - bacterieB.position.y;

    const squaredDistance = deltaX * deltaX + deltaY * deltaY;
    const radiiSum = (bacterieA.taille + bacterieB.taille) / 2;

    return squaredDistance < radiiSum * radiiSum;
}

function repousserChevauchements(bacterieA: Bacterie, bacterieB: Bacterie) {
    const angle = Math.atan2(
        bacterieB.position.y - bacterieA.position.y,
        bacterieB.position.x - bacterieA.position.x
    );

    const deltaX = Math.cos(angle);
    const deltaY = Math.sin(angle);

    bacterieA.position.x -= deltaX;
    bacterieA.position.y -= deltaY;
    bacterieB.position.x += deltaX;
    bacterieB.position.y += deltaY;
}

function propagation() {
    if (listBacterie.value.length < nombreMax.value) {
        const newBacteries: Bacterie[] = [];

        for (const bacterie of listBacterie.value) {
            if (checkForReproduction(bacterie)) {
                const newBacterie = createNewBacterie(bacterie);
                bacterie.nbrEnfant++;
                if (
                    listBacterie.value.length + newBacteries.length <
                    nombreMax.value
                ) {
                    newBacteries.push(newBacterie);
                }
            }
        }

        if (newBacteries.length > 0) {
            listBacterie.value.push(...newBacteries);

            for (const bacterie of newBacteries) {
                const key = getGridKey(
                    bacterie.position.x,
                    bacterie.position.y
                );
                if (!spatialGrid[key]) {
                    spatialGrid[key] = new Set();
                }
                spatialGrid[key].add(bacterie);
            }
        }
    }
}

function checkForReproduction(bacterie: Bacterie) {
    if (!bacterie.division && Math.random() < bacterie.reproduction / 100) {
        bacterie.division = true;
    }

    if (bacterie.division) {
        bacterie.taille++;
    }

    if (bacterie.taille > bacterie.tailleInitial * 2) {
        bacterie.taille = bacterie.tailleInitial;
        bacterie.division = false;
        return true;
    }

    return false;
}

function createNewBacterie(bacterie: Bacterie): Bacterie {
    return new Bacterie(
        bacterie.position.x,
        bacterie.position.y,
        mutationColor(bacterie.color),
        randomValue(bacterie.reproduction),
        randomValue(bacterie.taille),
        randomValue(bacterie.vieInitial),
        bacterie.generation + 1,
        0
    );
}

function baisserVie() {
    const bacteriesVivantes = listBacterie.value.filter((bacterie) => {
        bacterie.vie -= 0.5;
        return bacterie.vie > 0;
    });

    if (bacteriesVivantes.length < listBacterie.value.length) {
        listBacterie.value = bacteriesVivantes;
        updateSpatialGrid();
    }
}

function mutationColor(originalColor: string): string {
    const colors = originalColor
        .substring(4, originalColor.length - 1)
        .split(",")
        .map((color) => parseInt(color.trim()));

    colors[0] = Math.max(0, Math.min(255, randomValue(colors[0])));
    colors[1] = Math.max(0, Math.min(255, randomValue(colors[1])));
    colors[2] = Math.max(0, Math.min(255, randomValue(colors[2])));

    for (let i = 0; i < colors.length; i++) {
        if (colors[i] === 0 && Math.random() * 101 < 10) {
            colors[i] = 1;
        }
    }

    return `rgb(${colors[0]}, ${colors[1]}, ${colors[2]})`;
}

function randomValue(number: number): number {
    const modificationPercentage = Math.random() * 20 - 10;
    const modificationFactor = 1 + modificationPercentage / 100;

    const modifiedNumber = number * modificationFactor;

    return +modifiedNumber.toFixed(2);
}

function clearAllIntervals() {
    if (intervalDeplacement) clearInterval(intervalDeplacement);
    if (intervalPropagation) clearInterval(intervalPropagation);
    if (intervalBaisserVie) clearInterval(intervalBaisserVie);

    if (animationFrameId !== null) {
        cancelAnimationFrame(animationFrameId);
        animationFrameId = null;
    }
}

function reStart() {
    clearAllIntervals();
    listBacterie.value = [];
    initialisationList();
}

function start() {
    reStart();

    lastDeplacement = performance.now();
    lastPropagation = performance.now();
    lastBaisseVie = performance.now();

    animationFrameId = requestAnimationFrame(gameLoop);
}
</script>

<style scoped>
.bacterie-canvas {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    z-index: 0;
    pointer-events: auto;
    display: block;
}
</style>
