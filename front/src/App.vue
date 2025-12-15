<template>
    <div class="app-container">
        <Toast />
        <Selecteur v-if="localStore.pseudo" />
        <ThemeSwitcher />
        <Bubble />
        <router-view />
    </div>
</template>

<script setup lang="ts">
import Selecteur from "@/components/Selecteur.vue";
import { useLocalStore } from "./store/local.ts";
import Toast from "@/components/Toast.vue";
import ThemeSwitcher from "@/components/ThemeSwitcher.vue";
import Bubble from "@/components/Bubble.vue";

const localStore = useLocalStore();
</script>
