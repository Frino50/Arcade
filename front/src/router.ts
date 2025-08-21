import { createRouter, createWebHistory, RouteRecordRaw } from "vue-router";
import Accueil from "@/views/Accueil.vue";
import Demineur from "@/views/Demineur.vue";
import Morpion from "@/views/Morpion.vue";
import DeuxMilleQuaranteHuit from "@/views/2048.vue";
import Boids from "@/views/Boids.vue";
import Bacterie from "@/views/Bacterie.vue";
import Ball from "@/views/Ball.vue";
import Snake from "@/views/Snake.vue";
import Tetris from "@/views/Tetris.vue";

const routes: Array<RouteRecordRaw> = [
    {
        path: "/",
        name: "Accueil",
        component: Accueil,
    },
    {
        path: "/Demineur",
        name: "Demineur",
        component: Demineur,
    },
    {
        path: "/Morpion",
        name: "Morpion",
        component: Morpion,
    },
    {
        path: "/2048",
        name: "2048",
        component: DeuxMilleQuaranteHuit,
    },
    {
        path: "/Boids",
        name: "Boids",
        component: Boids,
    },
    {
        path: "/Bacterie",
        name: "Bacterie",
        component: Bacterie,
    },
    {
        path: "/Ball",
        name: "Ball",
        component: Ball,
    },
    {
        path: "/Snake",
        name: "Snake",
        component: Snake,
    },
    {
        path: "/Tetris",
        name: "Tetris",
        component: Tetris,
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;
