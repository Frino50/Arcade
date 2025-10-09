<template>
    <div class="app-container">
        <Toast />
        <Selecteur v-if="localStore.pseudo" />
        <ThemeSwitcher class="theme-switcher" />
        <router-view />
    </div>
</template>

<script setup lang="ts">
import Selecteur from "@/components/Selecteur.vue";
import { useLocalStore } from "./store/local.ts";
import Toast from "@/components/Toast.vue";
import ThemeSwitcher from "@/components/ThemeSwitcher.vue";

const localStore = useLocalStore();
</script>

<style scoped>
.theme-switcher {
    position: fixed;
    top: 0.75rem;
    right: 0.75rem;
    z-index: 9999;
    border-radius: 0.5rem;
    backdrop-filter: blur(4px);
    transition: opacity 0.3s ease;
}
</style>
