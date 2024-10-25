<template>
    <StatsIndividuelle
        :bacterie-selected="bacterieSelected"
        :open="dialog"
        @close="dialog = $event"
    />
    <StatsGenerale
        :list-bacterie="listBacterie"
        :open="statsGenerale"
        @close="statsGenerale = $event"
        v-if="listBacterie.length > 1"
    />
    <div>
        <div>
            <label>
                Vitesse de déplacement des bactéries :
                {{ vitesseDeplacement }}
            </label>
            <input
                style="margin-left: 20px"
                type="range"
                min="1"
                max="100"
                v-model="vitesseDeplacement"
                @change="changeIntervalSpeed"
            />
        </div>

        <div>
            <label>
                Vitesse de propagation des bactéries :
                {{ vitessePropagation }}
            </label>
            <input
                style="margin-left: 20px"
                type="range"
                min="1"
                max="10000"
                v-model="vitessePropagation"
                @change="changeIntervalPropagation"
            />
        </div>
        <div>
            <label>
                Nombre de bactérie à faire apparaître :
                {{ bacterieStart }}
            </label>
            <input
                style="margin-left: 20px"
                type="range"
                min="1"
                max="20"
                v-model="bacterieStart"
            />
        </div>
        <p>Nombre de bactérie : {{ listBacterie.length }} / {{ nombreMax }}</p>
        <button @click="start()">Start</button>
        <button
            @click="statsGenerale = true"
            :disabled="listBacterie.length < 1"
        >
            Stats
        </button>
    </div>

    <div
        v-for="bacterie in listBacterie"
        :key="bacterie.id"
        class="bacterie fluorescent-effect"
        :style="{
            left: bacterie.position.x + 'px',
            top: bacterie.position.y + 'px',
            backgroundColor: bacterie.color,
            width: bacterie.taille + 'px',
            height: bacterie.taille + 'px',
            background: bacterie.color,
            '--glow-color': bacterie.color,
        }"
        @click="(dialog = true), (bacterieSelected = bacterie)"
    ></div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { Color } from "../model/enum/color.ts";
import Bacterie from "../model/bacterie.ts";
import StatsIndividuelle from "../composant/StatsIndividuelle.vue";
import StatsGenerale from "../composant/StatsGenerale.vue";

const listBacterie = ref<Bacterie[]>([]);
const intervalDeplacement = ref<number>();
const vitesseDeplacement = ref<number>(100);
const intervalPropagation = ref<number>();
const vitessePropagation = ref<number>(200);
const intervalBaisserVie = ref<number>();
const statsGenerale = ref<boolean>(false);
const bacterieStart = ref<number>(10);
const bacterieSelected = ref<Bacterie>();
const dialog = ref<boolean>(false);
const nombreMax = ref<number>(500);
const gridSize = ref<number>(100);
const grid: { [key: string]: Bacterie[] } = {};

function initialisationList() {
    for (let i = 0; i < bacterieStart.value; i++) {
        let colorIndex;

        colorIndex = Math.floor(Math.random() * Object.values(Color).length);

        const taille = Math.random() * 10 + 5;

        listBacterie.value.push(
            new Bacterie(
                Math.random() * (window.innerWidth - taille),
                Math.random() * (window.innerHeight - taille),
                Object.values(Color)[colorIndex],
                1,
                taille,
                15,
                1,
                0
            )
        );
    }
}

function deplacement() {
    for (const key in grid) {
        if (grid.hasOwnProperty(key)) {
            delete grid[key];
        }
    }

    for (const bacterie of listBacterie.value) {
        const newX = bacterie.position.x + (Math.random() * 2 - 1);
        const newY = bacterie.position.y + (Math.random() * 2 - 1);

        bacterie.position.x = Math.max(
            0,
            Math.min(newX, window.innerWidth - bacterie.taille)
        );
        bacterie.position.y = Math.max(
            0,
            Math.min(newY, window.innerHeight - bacterie.taille)
        );

        addToGrid(bacterie);

        const nearbyBacteries = getNearbyBacteries(bacterie);

        for (const otherBacterie of nearbyBacteries) {
            if (detecterChevauchement(bacterie, otherBacterie)) {
                repousserChevauchements(bacterie, otherBacterie);
            }
        }
    }
}

function addToGrid(bacterie: Bacterie) {
    const xIndex = coordToGridIndex(bacterie.position.x);
    const yIndex = coordToGridIndex(bacterie.position.y);

    const key = `${xIndex}-${yIndex}`;

    if (!grid[key]) {
        grid[key] = [];
    }

    grid[key].push(bacterie);
}

function getNearbyBacteries(bacterie: Bacterie): Bacterie[] {
    const xIndex = coordToGridIndex(bacterie.position.x);
    const yIndex = coordToGridIndex(bacterie.position.y);

    const nearbyBacteries: Bacterie[] = [];

    for (let x = xIndex - 1; x <= xIndex + 1; x++) {
        for (let y = yIndex - 1; y <= yIndex + 1; y++) {
            const key = `${x}-${y}`;

            if (grid[key]) {
                nearbyBacteries.push(...grid[key]);
            }
        }
    }

    return nearbyBacteries;
}

function coordToGridIndex(coord: number): number {
    return Math.floor(coord / gridSize.value);
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
}

function checkForReproduction(bacterie: Bacterie) {
    if (
        bacterie.division === false &&
        Math.random() < bacterie.reproduction / 100
    ) {
        bacterie.division = true;
    }

    if (bacterie.division === true) {
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

function propagation() {
    if (listBacterie.value.length < nombreMax.value) {
        listBacterie.value.forEach((bacterie) => {
            if (checkForReproduction(bacterie)) {
                const newBacterie = createNewBacterie(bacterie);
                bacterie.nbrEnfant++;
                if (listBacterie.value.length < nombreMax.value) {
                    listBacterie.value.push(newBacterie);
                }
            }
        });
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
    clearInterval(intervalDeplacement.value);
    intervalDeplacement.value = setInterval(deplacement, event.target.value);
}

function changeIntervalPropagation(event: any) {
    clearInterval(intervalPropagation.value);
    intervalPropagation.value = setInterval(propagation, event.target.value);
}

function baisserVie() {
    listBacterie.value.forEach((bacterie, index) => {
        bacterie.vie -= 0.5;
        if (bacterie.vie <= 0) {
            listBacterie.value.splice(index, 1);
        }
    });
}

function reStart() {
    clearInterval(intervalDeplacement.value);
    clearInterval(intervalPropagation.value);
    clearInterval(intervalBaisserVie.value);
    listBacterie.value = [];
    initialisationList();
}

function start() {
    reStart();

    intervalDeplacement.value = setInterval(
        deplacement,
        vitesseDeplacement.value
    );

    intervalPropagation.value = setInterval(
        propagation,
        vitessePropagation.value
    );

    intervalBaisserVie.value = setInterval(baisserVie, 1000);
}
</script>

<style scoped>
.bacterie {
    position: absolute;
    cursor: pointer;
    border-radius: 50%;
}

.fluorescent-effect {
    box-shadow: 0 0 40px var(--glow-color), 0 0 0px var(--glow-color),
        0 0 40px var(--glow-color);
}
</style>
