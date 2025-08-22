import { createRouter, createWebHistory, RouteRecordRaw } from "vue-router";
import { useLocalStore } from "./store/user.ts";

// Vues principales
import Accueil from "@/views/Accueil.vue";
import Demineur from "@/views/Demineur.vue";
import Morpion from "@/views/Morpion.vue";
import DeuxMilleQuaranteHuit from "@/views/2048.vue";
import Boids from "@/views/Boids.vue";
import Bacterie from "@/views/Bacterie.vue";
import Ball from "@/views/Ball.vue";
import Snake from "@/views/Snake.vue";
import Tetris from "@/views/Tetris.vue";

// Auth
import Login from "@/views/Login.vue";
import Register from "@/views/Register.vue";

const routes: Array<RouteRecordRaw> = [
    {
        path: "/",
        name: "Accueil",
        component: Accueil,
        meta: { requiresAuth: true },
    },
    {
        path: "/demineur",
        name: "Demineur",
        component: Demineur,
        meta: { requiresAuth: true },
    },
    {
        path: "/morpion",
        name: "Morpion",
        component: Morpion,
        meta: { requiresAuth: true },
    },
    {
        path: "/2048",
        name: "2048",
        component: DeuxMilleQuaranteHuit,
        meta: { requiresAuth: true },
    },
    {
        path: "/boids",
        name: "Boids",
        component: Boids,
        meta: { requiresAuth: true },
    },
    {
        path: "/bacterie",
        name: "Bacterie",
        component: Bacterie,
        meta: { requiresAuth: true },
    },
    {
        path: "/ball",
        name: "Ball",
        component: Ball,
        meta: { requiresAuth: true },
    },
    {
        path: "/snake",
        name: "Snake",
        component: Snake,
        meta: { requiresAuth: true },
    },
    {
        path: "/tetris",
        name: "Tetris",
        component: Tetris,
        meta: { requiresAuth: true },
    },
    {
        path: "/login",
        name: "Login",
        component: Login,
    },
    {
        path: "/register",
        name: "Register",
        component: Register,
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

// ✅ Guard global
router.beforeEach((to, _from, next) => {
    const localStore = useLocalStore();

    if (to.meta.requiresAuth && !localStore.pseudo) {
        next("/login"); // pas connecté → login
    } else if (
        (to.path === "/login" || to.path === "/register") &&
        localStore.pseudo
    ) {
        next("/"); // déjà connecté → accueil
    } else {
        next();
    }
});

export default router;
