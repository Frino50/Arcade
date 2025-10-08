<template>
    <div class="button">
        <button @click="generer()">Générer</button>
    </div>
    <div v-for="(bonhomme, key) in bonhommes" :key="key">
        <div
            class="ball fluorescent-effect"
            :style="{
                width: bonhomme.diametre + 'px',
                height: bonhomme.diametre + 'px',
                left: bonhomme.x + 'px',
                top: bonhomme.y + 'px',
                'background-color': bonhomme.color,
                '--glow-color': bonhomme.color,
            }"
            @click="handleDivClick(bonhomme)"
        ></div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watchEffect } from "vue";
import { Color } from "@/models/enums/color";
import { Direction, directionState } from "@/models/enums/direction";
import bonhomme from "../models/bonhomme.ts";

const bon1 = new bonhomme(0, 0, Color.Red);
const bon2 = new bonhomme(500, 500, Color.Blue);
const bonhommes = ref<bonhomme[]>([bon1, bon2]);
const activeDiv = ref<bonhomme>(bon1);
const speed = <number>1;
const cercle = ref<number>(100);

let intervalId: number | null = null;

function handleKeyDown(event: KeyboardEvent) {
    const key = event.code as Direction;

    if (Object.values(Direction).includes(key)) {
        directionState[key] = true;
        if (intervalId === null) {
            intervalId = setInterval(
                handleDirectionChange,
                1
            ) as unknown as number;
        }
    }
}

function handleKeyUp(event: KeyboardEvent) {
    const key = event.code as Direction;

    if (Object.values(Direction).includes(key)) {
        directionState[key] = false;
        if (
            Object.values(Direction).every((dir) => !directionState[dir]) &&
            intervalId !== null
        ) {
            clearInterval(intervalId);
            intervalId = null;
        }
    }
}

function handleDirectionChange() {
    if (directionState.ArrowUp && directionState.ArrowRight) {
        move(activeDiv.value.x + speed, activeDiv.value.y - speed);
    } else if (directionState.ArrowUp && directionState.ArrowLeft) {
        move(activeDiv.value.x - speed, activeDiv.value.y - speed);
    } else if (directionState.ArrowDown && directionState.ArrowRight) {
        move(activeDiv.value.x + speed, activeDiv.value.y + speed);
    } else if (directionState.ArrowDown && directionState.ArrowLeft) {
        move(activeDiv.value.x - speed, activeDiv.value.y + speed);
    } else if (directionState.ArrowUp) {
        move(activeDiv.value.x, activeDiv.value.y - speed);
    } else if (directionState.ArrowDown) {
        move(activeDiv.value.x, activeDiv.value.y + speed);
    } else if (directionState.ArrowLeft) {
        move(activeDiv.value.x - speed, activeDiv.value.y);
    } else if (directionState.ArrowRight) {
        move(activeDiv.value.x + speed, activeDiv.value.y);
    }
}

function move(x: number, y: number) {
    const diametre = activeDiv.value.diametre; // Récupère le diamètre de l'élément actif
    const innerWidth = window.innerWidth; // Récupère la largeur de la fenêtre du navigateur
    const innerHeight = window.innerHeight; // Récupère la hauteur de la fenêtre du navigateur

    const newX = Math.max(0, Math.min(x, innerWidth - diametre)); // Limite la nouvelle coordonnée x
    const newY = Math.max(0, Math.min(y, innerHeight - diametre)); // Limite la nouvelle coordonnée y

    const collidesWithOther = checkCollisions(newX, newY); // Vérifie s'il y a des collisions à la nouvelle position

    if (!collidesWithOther) {
        activeDiv.value.x = newX; // Met à jour la coordonnée x de l'élément actif
        activeDiv.value.y = newY; // Met à jour la coordonnée y de l'élément actif
    } else {
        const verticalMove = Math.max(0, Math.min(y, innerHeight - diametre)); // Limite le mouvement en y
        const horizontalMove = Math.max(0, Math.min(x, innerWidth - diametre)); // Limite le mouvement en x

        const collidesVertically = checkCollisions(
            activeDiv.value.x,
            verticalMove
        ); // Vérifie les collisions après le déplacement en y
        const collidesHorizontally = checkCollisions(
            horizontalMove,
            activeDiv.value.y
        ); // Vérifie les collisions après le déplacement en x

        if (!collidesVertically) {
            activeDiv.value.y = verticalMove; // Met à jour la coordonnée y si le déplacement en y est possible
        }

        if (!collidesHorizontally) {
            activeDiv.value.x = horizontalMove; // Met à jour la coordonnée x si le déplacement en x est possible
        }

        const decalage = 3;
        const horizontaleBas = checkCollisions(
            horizontalMove,
            activeDiv.value.y + decalage
        );
        const horizontaleHaut = checkCollisions(
            horizontalMove,
            activeDiv.value.y - decalage
        );

        const verticalHaut = checkCollisions(
            activeDiv.value.x - decalage,
            verticalMove
        );
        const verticalBas = checkCollisions(
            activeDiv.value.x + decalage,
            verticalMove
        );

        if (!verticalBas) {
            activeDiv.value.x = horizontalMove + decalage;
        }
        if (!verticalHaut) {
            activeDiv.value.x = horizontalMove - decalage;
        }
        if (!horizontaleBas) {
            activeDiv.value.y = verticalMove + decalage;
        }
        if (!horizontaleHaut) {
            activeDiv.value.y = verticalMove - decalage;
        }
    }
}

function checkCollisions(x: number, y: number): boolean {
    return bonhommes.value.some((bonhomme) => {
        if (bonhomme !== activeDiv.value) {
            const distance = getDistance(
                x + activeDiv.value.diametre / 2,
                y + activeDiv.value.diametre / 2,
                bonhomme.x + bonhomme.diametre / 2,
                bonhomme.y + bonhomme.diametre / 2
            ); // Calcule la distance entre les centres des éléments
            return (
                distance < (activeDiv.value.diametre + bonhomme.diametre) / 2
            ); // Vérifie s'il y a collision
        }
        return false;
    });
}

function handleDivClick(clickedDiv: any) {
    activeDiv.value = clickedDiv;
}

onMounted(() => {
    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);
});

onUnmounted(() => {
    window.removeEventListener("keydown", handleKeyDown);
    window.removeEventListener("keyup", handleKeyUp);
});

function getDistance(x1: number, y1: number, x2: number, y2: number) {
    return Math.sqrt((x1 - x2) ** 2 + (y1 - y2) ** 2);
}

function generer() {
    const newBonhomme = new bonhomme(500, 500);
    bonhommes.value.push(newBonhomme);
}

watchEffect(() => {
    const bonhomme1 = bonhommes.value[0];
    const bonhomme2 = bonhommes.value[1];
    const distance = getDistance(
        bonhomme1.x,
        bonhomme1.y,
        bonhomme2.x,
        bonhomme2.y
    );

    if (distance === cercle.value) {
        const newBonhomme = new bonhomme(500, 500);
        bonhommes.value.push(newBonhomme);
    }
});
</script>

<style scoped>
.button {
    display: flex;
    justify-content: center;
    margin-top: 2rem;
    background-color: transparent;
}
.ball {
    position: absolute;
    border-radius: 100%;
    --glow-color: transparent;
}

.fluorescent-effect {
    box-shadow:
        0 0 40px var(--glow-color),
        0 0 0 var(--glow-color),
        0 0 40px var(--glow-color);
}
</style>
