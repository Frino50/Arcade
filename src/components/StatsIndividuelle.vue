<template>
    <div v-if="bacterieSelected" class="content">
        <div class="left-column">
            <div class="basic-info">
                <div
                    class="color-preview"
                    :style="{ backgroundColor: bacterieSelected.color }"
                ></div>
                <p class="detail">ID: {{ bacterieSelected.id }}</p>
                <p class="detail">
                    Position: ({{ bacterieSelected.position.x.toFixed(2) }},
                    {{ bacterieSelected.position.y.toFixed(2) }})
                </p>
                <p class="detail">
                    Reproduction: {{ bacterieSelected.reproduction }}%
                </p>
                <p class="detail">
                    Taille: {{ bacterieSelected.taille.toFixed(2) }}
                </p>
                <p class="detail">
                    Vie: {{ bacterieSelected.vie.toFixed(2) }} /
                    {{ bacterieSelected.vieInitial }}
                </p>
                <p class="detail">
                    Génération: {{ bacterieSelected.generation }}
                </p>
                <p class="detail">
                    Nombre d'enfants: {{ bacterieSelected.nbrEnfant }}
                </p>
            </div>

            <div class="charts-container">
                <div class="chart">
                    <h4>État de santé</h4>
                    <div class="pie-chart-container">
                        <svg width="100" height="100" viewBox="0 0 100 100">
                            <circle
                                r="45"
                                cx="50"
                                cy="50"
                                fill="transparent"
                                stroke="#ddd"
                                stroke-width="10"
                            />
                            <circle
                                r="45"
                                cx="50"
                                cy="50"
                                fill="transparent"
                                :stroke="bacterieSelected.color"
                                stroke-width="10"
                                :stroke-dasharray="`${healthPercentage * 283} 283`"
                                transform="rotate(-90, 50, 50)"
                            />
                            <text
                                x="50"
                                y="55"
                                text-anchor="middle"
                                font-size="15"
                                font-weight="bold"
                            >
                                {{ Math.round(healthPercentage * 100) }}%
                            </text>
                        </svg>
                    </div>
                </div>

                <div class="chart">
                    <h4>Cycle de division</h4>
                    <div class="progress-bar">
                        <div
                            class="progress"
                            :style="{
                                width: `${divisionProgress}%`,
                                backgroundColor: bacterieSelected.color,
                            }"
                        ></div>
                    </div>
                    <p class="detail">
                        {{ Math.round(divisionProgress) }}% complété
                    </p>
                </div>
            </div>
        </div>

        <div class="right-column">
            <img src="../assets/adn.gif" alt="ADN animation" class="adn-gif" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import Bacterie from "../models/bacterie.ts";

const bacterieSelected = defineModel<Bacterie>();
const healthPercentage = computed(() => {
    if (!bacterieSelected.value) return 0;
    return bacterieSelected.value.vie / bacterieSelected.value.vieInitial;
});

const divisionProgress = computed(() => {
    if (!bacterieSelected.value || !bacterieSelected.value.division) return 0;

    const initialSize = bacterieSelected.value.tailleInitial;
    const currentSize = bacterieSelected.value.taille;
    const maxSize = initialSize * 2;

    return ((currentSize - initialSize) / (maxSize - initialSize)) * 100;
});
</script>

<style scoped>
.content {
    display: flex;
    gap: 2rem;
    color: white;
    width: 30rem;
}

.right-column {
    display: flex;
    justify-content: center;
    align-items: center;
}

.adn-gif {
    transform-origin: center;
    height: 95%;
    width: 100%;
}

.basic-info {
    display: flex;
    flex-direction: column;
    gap: 5px;
    padding: 10px;
    background-color: rgba(60, 60, 60, 0.5);
    border-radius: 5px;
}

.color-preview {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    display: inline-block;
    margin-right: 10px;
    vertical-align: middle;
    box-shadow:
        0 0 10px,
        0 0 5px;
}

.detail {
    margin: 2px 0;
    font-size: 14px;
}

.charts-container {
    display: flex;
    justify-content: space-between;
    gap: 15px;
    margin-top: 10px;
}

.chart {
    flex: 1;
    text-align: center;
    padding: 10px;
    background-color: rgba(60, 60, 60, 0.5);
    border-radius: 5px;
}

.chart h4 {
    margin-top: 0;
    margin-bottom: 10px;
    font-size: 14px;
}

.pie-chart-container {
    display: flex;
    justify-content: center;
}

.progress-bar {
    width: 100%;
    height: 20px;
    background-color: #444;
    border-radius: 10px;
    overflow: hidden;
    margin-bottom: 5px;
}

.progress {
    height: 100%;
    transition: width 0.3s ease;
}
</style>
