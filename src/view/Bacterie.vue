<template>
    <PopUp
        v-model="dialogStatIndividuelles"
        title="Détails de la bactérie"
        width="500"
    >
        <StatsIndividuelle v-model="bacterieSelected" />
    </PopUp>
    <PopUp v-model="statsGenerale" title="Détails de la bactérie" width="500">
        <StatsGenerale v-model="listBacterie" />
    </PopUp>
    <PopUp
        v-model="dialogParametre"
        title="Paramètres"
        width="300"
        :initial-position="{ x: 15, y: 15 }"
    >
        <h2 class="title">Simulation de bactéries</h2>
        <div class="controls-group">
            <div class="control-item">
                <div class="control-label">
                    <span>Vitesse de déplacement</span>
                    <span>{{ vitesseDeplacement }}</span>
                </div>
                <div class="slider-container">
                    <input
                        type="range"
                        min="1"
                        max="100"
                        v-model="vitesseDeplacement"
                        @change="changeIntervalSpeed"
                    />
                </div>
            </div>

            <div class="control-item">
                <div class="control-label">
                    <span>Vitesse de propagation</span>
                    <span>{{ vitessePropagation }}</span>
                </div>
                <div class="slider-container">
                    <input
                        type="range"
                        min="1"
                        max="10000"
                        v-model="vitessePropagation"
                        @change="changeIntervalPropagation"
                    />
                </div>
            </div>

            <div class="control-item">
                <div class="control-label">
                    <span>Nombre initial de bactéries</span>
                    <span>{{ bacterieStart }}</span>
                </div>
                <div class="slider-container">
                    <input
                        type="range"
                        min="1"
                        max="20"
                        v-model="bacterieStart"
                    />
                </div>
            </div>
        </div>

        <div class="stats-info">
            <p>Population: {{ listBacterie.length }} / {{ nombreMax }}</p>
            <div class="progress-bar">
                <div
                    class="progress"
                    :style="{
                        width: `${(listBacterie.length / nombreMax) * 100}%`,
                    }"
                ></div>
            </div>
        </div>

        <div class="button-container">
            <button @click="start()" class="start-button">Démarrer</button>
            <button
                @click="statsGenerale = true"
                :disabled="listBacterie.length < 1"
                class="stats-button"
            >
                Statistiques
            </button>
        </div>
    </PopUp>

    <!-- Canvas en plein écran -->
    <canvas ref="canvasRef" class="bacterie-canvas"></canvas>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue";
import { Color } from "../model/enum/color.ts";
import Bacterie from "../model/bacterie.ts";
import StatsIndividuelle from "../composant/StatsIndividuelle.vue";
import StatsGenerale from "../composant/StatsGenerale.vue";
import PopUp from "@/composant/PopUp.vue";

const listBacterie = ref<Bacterie[]>([]);
const intervalDeplacement = ref<number>();
const vitesseDeplacement = ref<number>(100);
const intervalPropagation = ref<number>();
const vitessePropagation = ref<number>(200);
const intervalBaisserVie = ref<number>();
const statsGenerale = ref<boolean>(false);
const bacterieStart = ref<number>(10);
const bacterieSelected = ref<Bacterie>();
const dialogStatIndividuelles = ref<boolean>(false);
const nombreMax = ref<number>(1000); // On peut augmenter le maximum maintenant
const gridSize = ref<number>(20); // Grid plus petite pour plus de précision
const canvasRef = ref<HTMLCanvasElement | null>(null);
const animationFrameId = ref<number | null>(null);
const dialogParametre = ref<boolean>(true);
let ctx: CanvasRenderingContext2D | null = null;
let canvasWidth = 0;
let canvasHeight = 0;
let lastDeplacement = 0;
let lastPropagation = 0;
let lastBaisseVie = 0;

// Système de quadrillage spatial optimisé
const spatialGrid: { [key: string]: Set<Bacterie> } = {};

// Optimisation 1: Initialiser le canvas une seule fois
onMounted(() => {
    if (canvasRef.value) {
        ctx = canvasRef.value.getContext("2d");
        updateCanvasSize();
        window.addEventListener("resize", updateCanvasSize);

        // Gestionnaire de clic pour sélectionner une bactérie
        canvasRef.value.addEventListener("click", handleCanvasClick);
    }
});

onBeforeUnmount(() => {
    if (animationFrameId.value !== null) {
        cancelAnimationFrame(animationFrameId.value);
    }
    clearAllIntervals();
    if (canvasRef.value) {
        canvasRef.value.removeEventListener("click", handleCanvasClick);
    }
    window.removeEventListener("resize", updateCanvasSize);
});

function updateCanvasSize() {
    if (!canvasRef.value) return;

    // Ajuster le canvas pour occuper tout l'écran
    canvasWidth = window.innerWidth;
    canvasHeight = window.innerHeight;
    canvasRef.value.width = canvasWidth;
    canvasRef.value.height = canvasHeight;

    // S'assurer que les bactéries existantes restent dans les limites
    if (listBacterie.value.length > 0) {
        for (const bacterie of listBacterie.value) {
            // Repositionner les bactéries qui seraient hors écran après le redimensionnement
            bacterie.position.x = Math.min(
                bacterie.position.x,
                canvasWidth - bacterie.taille
            );
            bacterie.position.y = Math.min(
                bacterie.position.y,
                canvasHeight - bacterie.taille
            );
        }
        // Mettre à jour la grille spatiale après avoir repositionné les bactéries
        updateSpatialGrid();
    }
}

// Optimisation 2: Gestionnaire de clic optimisé
function handleCanvasClick(event: MouseEvent) {
    const rect = canvasRef.value!.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    // Utiliser le grid spatial pour trouver les bactéries proches
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
        dialogStatIndividuelles.value = true;
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

    // Initialisation de la grille spatiale
    updateSpatialGrid();
}

// Optimisation 3: Utilisation d'un système de grille spatiale plus efficace
function updateSpatialGrid() {
    // Vider la grille
    for (const key in spatialGrid) {
        delete spatialGrid[key];
    }

    // Remplir la grille avec les bactéries
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

// Optimisation 4: Utilisation de requestAnimationFrame pour une animation fluide
function gameLoop(timestamp: number) {
    const deltaTime = timestamp - lastDeplacement;
    const deltaPropagation = timestamp - lastPropagation;
    const deltaBaisseVie = timestamp - lastBaisseVie;

    // Déplacer les bactéries selon le timer
    if (deltaTime >= vitesseDeplacement.value) {
        deplacement();
        lastDeplacement = timestamp;
    }

    // Propager les bactéries selon le timer
    if (deltaPropagation >= vitessePropagation.value) {
        propagation();
        lastPropagation = timestamp;
    }

    // Baisser la vie des bactéries toutes les secondes
    if (deltaBaisseVie >= 1000) {
        baisserVie();
        lastBaisseVie = timestamp;
    }

    // Dessiner toutes les bactéries
    renderBacteries();

    // Continuer la boucle
    animationFrameId.value = requestAnimationFrame(gameLoop);
}

// Optimisation 5: Rendu optimisé avec canvas au lieu du DOM
function renderBacteries() {
    if (!ctx) return;

    // Effacer le canvas
    ctx.clearRect(0, 0, canvasWidth, canvasHeight);

    // Dessiner chaque bactérie
    for (const bacterie of listBacterie.value) {
        ctx.beginPath();
        ctx.arc(
            bacterie.position.x + bacterie.taille / 2,
            bacterie.position.y + bacterie.taille / 2,
            bacterie.taille / 2,
            0,
            Math.PI * 2
        );

        // Couleur de remplissage
        ctx.fillStyle = bacterie.color;
        ctx.fill();

        // Effet de lueur
        ctx.shadowBlur = 20;
        ctx.shadowColor = bacterie.color;
        ctx.strokeStyle = bacterie.color;
        ctx.lineWidth = 2;
        ctx.stroke();
        ctx.shadowBlur = 0;
    }
}

// Optimisation 6: Détection de collision optimisée
function deplacement() {
    for (const bacterie of listBacterie.value) {
        // Sauvegarder l'ancienne position pour la grille spatiale
        const oldGridKey = getGridKey(bacterie.position.x, bacterie.position.y);

        // Déplacer la bactérie avec un mouvement aléatoire
        const newX = bacterie.position.x + (Math.random() * 2 - 1);
        const newY = bacterie.position.y + (Math.random() * 2 - 1);

        // Appliquer strictement les limites du canvas
        // S'assurer que la totalité de la bactérie reste visible à l'écran
        bacterie.position.x = Math.max(
            0,
            Math.min(newX, canvasWidth - bacterie.taille)
        );
        bacterie.position.y = Math.max(
            0,
            Math.min(newY, canvasHeight - bacterie.taille)
        );

        // Effet rebond si on atteint les bords
        if (
            bacterie.position.x <= 0 ||
            bacterie.position.x >= canvasWidth - bacterie.taille
        ) {
            // Légère impulsion dans la direction opposée pour éviter de coller au bord
            bacterie.position.x += bacterie.position.x <= 0 ? 0.5 : -0.5;
        }

        if (
            bacterie.position.y <= 0 ||
            bacterie.position.y >= canvasHeight - bacterie.taille
        ) {
            // Légère impulsion dans la direction opposée pour éviter de coller au bord
            bacterie.position.y += bacterie.position.y <= 0 ? 0.5 : -0.5;
        }

        // Mettre à jour la grille spatiale si nécessaire
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

    // Vérifier les collisions après les déplacements
    checkCollisions();
}

// Optimisation 7: Vérification de collision par lots
function checkCollisions() {
    // Pour chaque cellule de la grille
    for (const key in spatialGrid) {
        if (!spatialGrid[key] || spatialGrid[key].size <= 1) continue;

        // Obtenir toutes les bactéries dans cette cellule et les cellules voisines
        const bacteries = Array.from(spatialGrid[key]);

        // Vérifier les collisions entre les bactéries dans la même cellule
        for (let i = 0; i < bacteries.length; i++) {
            for (let j = i + 1; j < bacteries.length; j++) {
                if (detecterChevauchement(bacteries[i], bacteries[j])) {
                    repousserChevauchements(bacteries[i], bacteries[j]);
                }
            }
        }

        // Vérifier les collisions avec les bactéries dans les cellules voisines
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
    // Calcule l'angle entre les deux bactéries
    const angle = Math.atan2(
        bacterieB.position.y - bacterieA.position.y,
        bacterieB.position.x - bacterieA.position.x
    );

    const deltaX = Math.cos(angle);
    const deltaY = Math.sin(angle);

    // Déplace les bactéries pour les éloigner l'une de l'autre
    bacterieA.position.x -= deltaX;
    bacterieA.position.y -= deltaY;
    bacterieB.position.x += deltaX;
    bacterieB.position.y += deltaY;

    // Mettre à jour la grille spatiale après la répulsion n'est pas nécessaire
    // car le déplacement est trop petit pour changer de cellule
}

// Optimisation 8: Gestion de la propagation par lots
function propagation() {
    if (listBacterie.value.length < nombreMax.value) {
        // Créer un tableau pour stocker les nouvelles bactéries
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

        // Ajouter toutes les nouvelles bactéries en une seule fois
        if (newBacteries.length > 0) {
            listBacterie.value.push(...newBacteries);

            // Mettre à jour la grille spatiale pour les nouvelles bactéries
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
        return true; // Indicate that this bacterie can reproduce
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

// Optimisation 9: Gestion optimisée de la durée de vie
function baisserVie() {
    // Utiliser filter au lieu de splice dans une boucle
    const bacteriesVivantes = listBacterie.value.filter((bacterie) => {
        bacterie.vie -= 0.5;
        return bacterie.vie > 0;
    });

    // Si des bactéries sont mortes, mettre à jour la liste et la grille spatiale
    if (bacteriesVivantes.length < listBacterie.value.length) {
        // Mise à jour complète de la grille spatiale (plus simple que de suivre chaque suppression)
        listBacterie.value = bacteriesVivantes;
        updateSpatialGrid();
    }
}

function mutationColor(originalColor: string): string {
    // Convertir la couleur de "rgb(x, y, z)" en un tableau de nombres
    const colors = originalColor
        .substring(4, originalColor.length - 1)
        .split(",")
        .map((color) => parseInt(color.trim()));

    colors[0] = Math.max(0, Math.min(255, randomValue(colors[0]))); // Modification rouge
    colors[1] = Math.max(0, Math.min(255, randomValue(colors[1]))); // Modification verte
    colors[2] = Math.max(0, Math.min(255, randomValue(colors[2]))); // Modification bleue

    // Vérifier si l'une des valeurs est à 0, et si oui, avoir 10% de chance de la mettre à 1
    for (let i = 0; i < colors.length; i++) {
        if (colors[i] === 0 && Math.random() * 101 < 10) {
            colors[i] = 1;
        }
    }

    // Retourner la nouvelle couleur sous forme de chaîne
    return `rgb(${colors[0]}, ${colors[1]}, ${colors[2]})`;
}

function randomValue(number: number): number {
    const modificationPercentage = Math.random() * 20 - 10; //Génère un nombre aléatoire entre 10 et -10
    const modificationFactor = 1 + modificationPercentage / 100; // Permet d'avoir le nombre en pourcentage

    const modifiedNumber = number * modificationFactor;

    return +modifiedNumber.toFixed(2);
}

function changeIntervalSpeed(event: any) {
    // Pas besoin de nettoyer d'intervalle puisqu'on utilise requestAnimationFrame
    vitesseDeplacement.value = event.target.value;
}

function changeIntervalPropagation(event: any) {
    // Pas besoin de nettoyer d'intervalle
    vitessePropagation.value = event.target.value;
}

function clearAllIntervals() {
    if (intervalDeplacement.value) clearInterval(intervalDeplacement.value);
    if (intervalPropagation.value) clearInterval(intervalPropagation.value);
    if (intervalBaisserVie.value) clearInterval(intervalBaisserVie.value);

    if (animationFrameId.value !== null) {
        cancelAnimationFrame(animationFrameId.value);
        animationFrameId.value = null;
    }
}

function reStart() {
    clearAllIntervals();
    listBacterie.value = [];
    initialisationList();
}

function start() {
    reStart();

    // Initialiser les timestamps
    lastDeplacement = performance.now();
    lastPropagation = performance.now();
    lastBaisseVie = performance.now();

    // Démarrer la boucle de jeu
    animationFrameId.value = requestAnimationFrame(gameLoop);
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
    pointer-events: auto; /* Pour s'assurer que les clics fonctionnent */
    display: block;
}
.controls-panel {
    position: fixed;
    top: 20px;
    left: 20px;
    background-color: rgba(40, 40, 40, 0.95);
    color: white;
    padding: 20px;
    border: 2px solid #555;
    border-radius: 8px;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5);
    z-index: 10;
    max-width: 500px;
    display: flex;
    flex-direction: column;
    gap: 15px;
}

.title {
    margin-top: 0;
    border-bottom: 1px solid #555;
    padding-bottom: 8px;
}

.controls-group {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 10px;
    background-color: rgba(60, 60, 60, 0.5);
    border-radius: 5px;
}

.control-item {
    display: flex;
    flex-direction: column;
    gap: 5px;
}

.control-label {
    display: flex;
    justify-content: space-between;
    font-size: 14px;
}

.slider-container {
    position: relative;
    width: 100%;
    height: 20px;
}

input[type="range"] {
    width: 100%;
    height: 8px;
    background: #444;
    border-radius: 4px;
    outline: none;
}

input[type="range"]::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: #fff;
    cursor: pointer;
    box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);
}

input[type="range"]::-moz-range-thumb {
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: #fff;
    cursor: pointer;
    box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);
}

.stats-info {
    display: flex;
    flex-direction: column;
    gap: 5px;
    padding: 10px;
    background-color: rgba(60, 60, 60, 0.5);
    border-radius: 5px;
}

.button-container {
    display: flex;
    gap: 12px;
    margin-top: 5px;
}

button {
    flex: 1;
    padding: 8px 15px;
    background-color: #444;
    color: white;
    border: none;
    border-radius: 5px;
    cursor: pointer;
    transition: background-color 0.2s;
    font-size: 14px;
}

button:hover:not(:disabled) {
    background-color: #555;
}

button:disabled {
    background-color: #333;
    color: #666;
    cursor: not-allowed;
}

.start-button {
    background-color: #3d6a44;
}

.start-button:hover {
    background-color: #4a7e53;
}

.stats-button {
    background-color: #4a5d7e;
}

.stats-button:hover {
    background-color: #5a6d8e;
}

.stats-info {
    display: flex;
    flex-direction: column;
    gap: 5px;
    padding: 10px;
    background-color: rgba(60, 60, 60, 0.5);
    border-radius: 5px;
}

.progress-bar {
    width: 100%;
    height: 10px;
    background-color: #444;
    border-radius: 5px;
    overflow: hidden;
    margin-top: 5px;
}

.progress {
    height: 100%;
    transition: width 0.3s ease;
    background-color: blue;
}
</style>
