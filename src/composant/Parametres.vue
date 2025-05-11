<!-- Correction du fichier Parametres.vue -->
<template>
    <div class="controls-group">
        <div class="control-item">
            <div class="control-label">
                <span>Vitesse de déplacement</span>
                <span>{{ vitesseDeplacement }}</span>
            </div>
            <div class="slider-container">
                <input
                    type="range"
                    min="1"
                    max="100"
                    v-model="vitesseDeplacement"
                    @change="
                        emit('vitesseDeplacement', Number(vitesseDeplacement))
                    "
                />
            </div>
        </div>

        <div class="control-item">
            <div class="control-label">
                <span>Vitesse de propagation</span>
                <span>{{ vitessePropagation }}</span>
            </div>
            <div class="slider-container">
                <input
                    type="range"
                    min="1"
                    max="10000"
                    v-model="vitessePropagation"
                    @change="
                        emit('vitessePropagation', Number(vitessePropagation))
                    "
                />
            </div>
        </div>

        <div class="control-item">
            <div class="control-label">
                <span>Nombre initial de bactéries</span>
                <span>{{ bacterieStart }}</span>
            </div>
            <div class="slider-container">
                <input
                    type="range"
                    min="1"
                    max="20"
                    v-model="bacterieStart"
                    @change="emit('bacterieStart', Number(bacterieStart))"
                />
            </div>
        </div>

        <div class="stats-info">
            <p>Population: {{ listBacterie.length }} / {{ nombreMax }}</p>
            <div class="progress-bar">
                <div
                    class="progress"
                    :style="{
                        width: `${(listBacterie.length / nombreMax) * 100}%`,
                    }"
                ></div>
            </div>
        </div>
        <div class="button-container">
            <button @click="emit('start')" class="start-button">
                Démarrer
            </button>
            <button
                @click="emit('dialogGenerale', true)"
                :disabled="listBacterie.length < 1"
                class="stats-button"
            >
                Statistiques
            </button>
        </div>
    </div>
</template>

<script setup lang="ts">
import Bacterie from "@/model/bacterie.ts";
import { onMounted, ref } from "vue";

const emit = defineEmits([
    "dialogGenerale",
    "nombreMax",
    "bacterieStart",
    "start",
    "vitesseDeplacement",
    "vitessePropagation",
]);

const listBacterie = defineModel<Bacterie[]>({
    default: () => [],
});

const vitesseDeplacement = ref(100);
const vitessePropagation = ref(200);
const bacterieStart = ref(10);
const nombreMax = ref(1000);

onMounted(() => {
    emit("vitesseDeplacement", vitesseDeplacement.value);
    emit("vitessePropagation", vitessePropagation.value);
    emit("bacterieStart", bacterieStart.value);
    emit("nombreMax", nombreMax.value);
});
</script>

<style scoped>
.controls-group {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 10px;
    background-color: rgba(60, 60, 60, 0.5);
    color: white;
    border-radius: 5px;
}

.control-item {
    display: flex;
    flex-direction: column;
    gap: 5px;
}

.control-label {
    display: flex;
    justify-content: space-between;
    font-size: 14px;
}

.slider-container {
    position: relative;
    width: 100%;
    height: 20px;
}

input[type="range"] {
    width: 100%;
    height: 8px;
    background: #444;
    border-radius: 4px;
    outline: none;
}

input[type="range"]::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: #fff;
    cursor: pointer;
    box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);
}

input[type="range"]::-moz-range-thumb {
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: #fff;
    cursor: pointer;
    box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);
}

.stats-info {
    display: flex;
    flex-direction: column;
    padding: 10px;
    border-radius: 5px;
}

.button-container {
    display: flex;
    gap: 12px;
    margin-top: 5px;
}

button {
    flex: 1;
    padding: 8px 15px;
    background-color: #444;
    color: white;
    border: none;
    border-radius: 5px;
    cursor: pointer;
    transition: background-color 0.2s;
    font-size: 14px;
}

button:disabled {
    background-color: #333;
    color: #666;
    cursor: not-allowed;
}

.start-button {
    background-color: #4caf50;
}

.start-button:hover {
    background-color: #3dc042;
}

.stats-button {
    background-color: #4a5d7e;
}

.stats-button:hover {
    background-color: #5a6d8e;
}

.progress-bar {
    width: 100%;
    height: 10px;
    background-color: #444;
    border-radius: 5px;
    overflow: hidden;
    margin-top: 5px;
}

.progress {
    height: 100%;
    transition: width 0.3s ease;
    background-color: blue;
}
</style>
