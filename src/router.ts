import { createRouter, createWebHistory, RouteRecordRaw } from "vue-router";
import Accueil from "../src/view/Accueil.vue";
import Demineur from "../src/view/Demineur.vue";
import Morpion from "../src/view/Morpion.vue";
import DeuxMilleQuaranteHuit from "../src/view/2048.vue";
import Boids from "../src/view/Boids.vue";
import Bacterie from "../src/view/Bacterie.vue";
import Ball from "../src/view/Ball.vue";
import Snake from "../src/view/Snake.vue";
import Tetris from "../src/view/Tetris.vue";

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
