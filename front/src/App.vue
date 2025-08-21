<template>
    <div class="app-container">
        <Selecteur></Selecteur>
        <router-view />
    </div>
</template>

<script setup lang="ts">
import Selecteur from "@/components/Selecteur.vue";
</script>

<style scoped>
.app-container {
    background-color: #2b2926;
    z-index: 1;
}
</style>
