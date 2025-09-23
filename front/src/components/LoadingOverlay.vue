<template>
    <div v-if="props.loading" class="spinner-overlay">
        <div class="spinner"></div>
    </div>
</template>

<script setup lang="ts">
const props = defineProps<{ loading: boolean }>();
</script>

<style scoped>
.spinner-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(255, 255, 255, 0.6);
    display: flex;
    justify-content: center;
    align-items: center;
    border-radius: inherit;
    z-index: 10;
    pointer-events: auto;
}

.spinner {
    width: 40px;
    height: 40px;
    border: 4px solid rgba(0, 0, 0, 0.2);
    border-top-color: #333;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
    pointer-events: auto;
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}
</style>
