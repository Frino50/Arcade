<template>
    <svg
        v-if="boidSelect"
        v-for="(boid, index) in boids"
        :key="index"
        class="line"
    >
        <line
            v-if="distance(boidSelect, boid) < 50"
            :x1="boidSelect.x + 5"
            :y1="boidSelect.y + 8"
            :x2="boid.x + 5"
            :y2="boid.y + 8"
            style="stroke: yellow; stroke-width: 2"
        />
    </svg>

    <div
        v-for="(boid, index) in boids"
        :key="index"
        :class="[
            'boid',
            `boid-${index % 3}`,
            { 'selected-boid': index === boidSelect?.id },
        ]"
        @click="boidSelect = boid"
        :style="{
            left: boid.x + 'px',
            top: boid.y + 'px',
            transform: `rotate(${getRotation(boid)}rad)`,
        }"
    ></div>

    <div v-if="boidSelect" class="caracteristique">
        <div>Id: {{ boidSelect.id }}</div>
        <div>Vx: {{ boidSelect.vx.toFixed(2) }}</div>
        <div>Vy: {{ boidSelect.vy.toFixed(2) }}</div>
        <div>x: {{ boidSelect.x.toFixed(2) }}</div>
        <div>y: {{ boidSelect.y.toFixed(2) }}</div>
        <div>Direction: {{ getDirection(boidSelect.vx, boidSelect.vy) }}</div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import Boid from "../model/boid";

const SPAWN_NUMBER = 100;
const BOID_SIZE = 17;
const SEPARATION_RADIUS = 25;
const SEPARATION_FORCE = 0.1;
const ALIGNMENT_RADIUS = 50;
const ALIGNMENT_FORCE = 0.05;
const COHESION_RADIUS = 50;
const COHESION_FORCE = 0.05;
const boids = ref<Boid[]>([]);
const boidSelect = ref<Boid>();

function getDirection(vx: number, vy: number): string {
    const angle = Math.atan2(vy, vx) * (180 / Math.PI);
    const directions = [
        { min: -22.5, max: 22.5, direction: "Droite" },
        { min: 22.5, max: 67.5, direction: "Bas-droit" },
        { min: 67.5, max: 112.5, direction: "Bas" },
        { min: 112.5, max: 157.5, direction: "Bas-gauche" },
        { min: 157.5, max: 180, direction: "Gauche" },
        { min: -180, max: -157.5, direction: "Gauche" },
        { min: -157.5, max: -112.5, direction: "Haut-gauche" },
        { min: -112.5, max: -67.5, direction: "Haut" },
        { min: -67.5, max: -22.5, direction: "Haut-droit" },
    ];

    for (const dir of directions) {
        if (angle >= dir.min && angle < dir.max) {
            return dir.direction;
        }
    }

    return "Inconnue";
}

function distance(boid1: Boid, boid2: Boid): number {
    return Math.sqrt((boid1.x - boid2.x) ** 2 + (boid1.y - boid2.y) ** 2);
}

function updateBoids() {
    for (const boid of boids.value) {
        const otherBoids = boids.value.filter(
            (otherBoid) => otherBoid !== boid
        );

        separation(boid, otherBoids);
        alignement(boid, otherBoids);
        cohesion(boid, otherBoids);

        boid.x += boid.vx;
        boid.y += boid.vy;

        deplacement(boid);
    }
}

function separation(boid: Boid, otherBoids: Boid[]) {
    for (const otherBoid of otherBoids) {
        const dist = distance(boid, otherBoid);

        if (dist < SEPARATION_RADIUS) {
            const diffX = boid.x - otherBoid.x;
            const diffY = boid.y - otherBoid.y;

            boid.vx += (diffX / dist) * SEPARATION_FORCE;
            boid.vy += (diffY / dist) * SEPARATION_FORCE;
        }
    }
}

function alignement(boid: Boid, otherBoids: Boid[]) {
    let averageDirection = 0;
    let count = 0;

    for (const otherBoid of otherBoids) {
        const dist = distance(boid, otherBoid);

        if (dist < ALIGNMENT_RADIUS) {
            averageDirection += Math.atan2(otherBoid.vy, otherBoid.vx);
            count++;
        }
    }

    if (count > 0) {
        averageDirection /= count;

        const averageVx = Math.cos(averageDirection);
        const averageVy = Math.sin(averageDirection);

        boid.vx += (averageVx - boid.vx) * ALIGNMENT_FORCE;
        boid.vy += (averageVy - boid.vy) * ALIGNMENT_FORCE;
    }
}

function cohesion(boid: Boid, otherBoids: Boid[]) {
    let averagePositionX = 0;
    let averagePositionY = 0;
    let count = 0;

    for (const otherBoid of otherBoids) {
        const dist = distance(boid, otherBoid);

        if (dist < COHESION_RADIUS) {
            averagePositionX += otherBoid.x;
            averagePositionY += otherBoid.y;
            count++;
        }
    }

    if (count > 0) {
        averagePositionX /= count;
        averagePositionY /= count;

        const directionX = averagePositionX - boid.x;
        const directionY = averagePositionY - boid.y;

        const length = Math.hypot(directionX, directionY);
        const normalizedDirectionX = directionX / length;
        const normalizedDirectionY = directionY / length;

        boid.vx += normalizedDirectionX * COHESION_FORCE;
        boid.vy += normalizedDirectionY * COHESION_FORCE;
    }
}

function deplacement(boid: Boid) {
    if (boid.x < 0) {
        boid.x = window.innerWidth - BOID_SIZE;
    } else if (boid.x > window.innerWidth - BOID_SIZE) {
        boid.x = 0;
    }

    if (boid.y < 0) {
        boid.y = window.innerHeight - BOID_SIZE;
    } else if (boid.y > window.innerHeight - BOID_SIZE) {
        boid.y = 0;
    }
}

function getRotation(boid: Boid): number {
    return Math.atan2(boid.vy, boid.vx) + Math.PI / 2;
}

onMounted(() => {
    function animate() {
        updateBoids();
        requestAnimationFrame(animate);
    }

    for (let i = 0; i < SPAWN_NUMBER; i++) {
        boids.value.push(new Boid());
    }

    animate();

    window.addEventListener("keydown", touchePresse);
});

onUnmounted(() => {
    window.removeEventListener("keydown", touchePresse);
});

function touchePresse(event: KeyboardEvent): void {
    if (event.key === "ArrowRight" && boidSelect.value) {
        const nextId = (boidSelect.value.id + 1) % SPAWN_NUMBER;
        boidSelect.value = boids.value.find((boid) => boid.id === nextId);
    } else if (event.key === "ArrowLeft" && boidSelect.value) {
        const prevId = (boidSelect.value.id - 1 + SPAWN_NUMBER) % SPAWN_NUMBER;
        boidSelect.value = boids.value.find((boid) => boid.id === prevId);
    }
}
</script>

<style scoped>
.boid {
    cursor: pointer;
    width: 0;
    height: 0;
    border-left: 0.3rem solid transparent;
    border-right: 0.3rem solid transparent;
    border-bottom: 0.9rem solid white;
    position: absolute;
    transition: transform 0.3s ease;
}

.boid-0 {
    border-bottom: 0.9rem solid #3498db;
}

.boid-1 {
    border-bottom: 0.9rem solid #2a6d99;
}

.boid-2 {
    border-bottom: 0.9rem solid #dde1e5;
}
.selected-boid {
    border-bottom: 0.9rem solid red;
}
.caracteristique {
    height: 3rem;
    display: flex;
    flex-direction: column;
    margin: 0;
}

.line {
    position: absolute;
    width: 95%;
    height: 100%;
}
</style>
