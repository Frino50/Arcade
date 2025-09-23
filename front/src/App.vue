<template>
    <div class="app-container">
        <Toast />
        <Selecteur v-if="localStore.pseudo" />
        <router-view />
    </div>
</template>

<script setup lang="ts">
import Selecteur from "@/components/Selecteur.vue";
import { useLocalStore } from "./store/local.ts";
import Toast from "@/components/Toast.vue";

const localStore = useLocalStore();
</script>

<style scoped>
.app-container {
    background-color: #2b2926;
    z-index: 1;
}
</style>
