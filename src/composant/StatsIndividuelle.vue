<template>
    <div
        class="dialog"
        v-if="props.open"
        ref="dialogRef"
        @mousedown="startDragging"
        @mouseup="stopDragging"
    >
        <div v-if="props.bacterieSelected" class="content">
            <div class="basic-info">
                <h3 class="title">Détails de la bactérie</h3>
                <div
                    class="color-preview"
                    :style="{ backgroundColor: props.bacterieSelected.color }"
                ></div>
                <p class="detail">ID: {{ props.bacterieSelected.id }}</p>
                <p class="detail">
                    Position: ({{
                        props.bacterieSelected.position.x.toFixed(2)
                    }}, {{ props.bacterieSelected.position.y.toFixed(2) }})
                </p>
                <p class="detail">
                    Reproduction: {{ props.bacterieSelected.reproduction }}%
                </p>
                <p class="detail">
                    Taille: {{ props.bacterieSelected.taille.toFixed(2) }}
                </p>
                <p class="detail">
                    Vie: {{ props.bacterieSelected.vie.toFixed(2) }} /
                    {{ props.bacterieSelected.vieInitial }}
                </p>
                <p class="detail">
                    Génération: {{ props.bacterieSelected.generation }}
                </p>
                <p class="detail">
                    Nombre d'enfants: {{ props.bacterieSelected.nbrEnfant }}
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
                                :stroke="props.bacterieSelected.color"
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
                                backgroundColor: props.bacterieSelected.color,
                            }"
                        ></div>
                    </div>
                    <p class="detail">
                        {{ Math.round(divisionProgress) }}% complété
                    </p>
                </div>
            </div>

            <div class="evolution-chart">
                <h4>Historique ({{ historyData.length }} entrées)</h4>
                <button @click="startTracking" v-if="!isTracking">
                    Commencer à suivre
                </button>
                <button @click="stopTracking" v-if="isTracking">
                    Arrêter le suivi
                </button>
                <button @click="clearHistory">Effacer l'historique</button>

                <div class="chart-container" v-if="historyData.length > 0">
                    <svg width="400" height="150" viewBox="0 0 400 150">
                        <!-- Axes -->
                        <line
                            x1="40"
                            y1="130"
                            x2="380"
                            y2="130"
                            stroke="white"
                        />
                        <line x1="40" y1="130" x2="40" y2="20" stroke="white" />

                        <!-- Life history line -->
                        <polyline
                            :points="getLifeHistoryPoints()"
                            fill="none"
                            stroke="red"
                            stroke-width="2"
                        />

                        <!-- Size history line -->
                        <polyline
                            :points="getSizeHistoryPoints()"
                            fill="none"
                            stroke="blue"
                            stroke-width="2"
                        />

                        <!-- Legend -->
                        <circle cx="60" cy="15" r="4" fill="red" />
                        <text x="70" y="20" fill="white" font-size="12">
                            Vie
                        </text>
                        <circle cx="120" cy="15" r="4" fill="blue" />
                        <text x="130" y="20" fill="white" font-size="12">
                            Taille
                        </text>
                    </svg>
                </div>
                <p v-else class="empty-chart-message">
                    Commencez le suivi pour enregistrer les données
                </p>
            </div>

            <div
                class="family-tree"
                v-if="props.bacterieSelected.nbrEnfant > 0"
            >
                <h4>Arbre généalogique simplifié</h4>
                <div class="tree-visualization">
                    <div class="tree-node parent">
                        <div
                            class="node-circle"
                            :style="{
                                backgroundColor: props.bacterieSelected.color,
                            }"
                        ></div>
                        <div class="node-label">
                            {{ props.bacterieSelected.id }}
                        </div>
                    </div>
                    <div class="tree-children">
                        <div class="child-connector"></div>
                        <div class="children-count">
                            {{ props.bacterieSelected.nbrEnfant }} enfants
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="button-container">
            <button @click="emit('close', false)" class="close-button">
                Fermer
            </button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted, watch } from "vue";
import Bacterie from "../model/bacterie";

const mouseStartPosition = ref<{ x: number; y: number }>({ x: 0, y: 0 });
const historyData = ref<Array<{ time: number; vie: number; taille: number }>>(
    []
);
const isTracking = ref<boolean>(false);
const trackingInterval = ref<number>();
const props = defineProps({
    bacterieSelected: Object as () => Bacterie,
    open: { type: Boolean, default: false },
});
const emit = defineEmits<(e: "close", type: boolean) => boolean>();
const dialogRef = ref<HTMLElement>();
const healthPercentage = computed(() => {
    if (!props.bacterieSelected) return 0;
    return props.bacterieSelected.vie / props.bacterieSelected.vieInitial;
});

const divisionProgress = computed(() => {
    if (!props.bacterieSelected || !props.bacterieSelected.division) return 0;

    const initialSize = props.bacterieSelected.tailleInitial;
    const currentSize = props.bacterieSelected.taille;
    const maxSize = initialSize * 2;

    return ((currentSize - initialSize) / (maxSize - initialSize)) * 100;
});

function startTracking() {
    if (!isTracking.value && props.bacterieSelected) {
        isTracking.value = true;

        addHistoryDataPoint();

        trackingInterval.value = setInterval(addHistoryDataPoint, 1000);
    }
}

function stopTracking() {
    isTracking.value = false;
    clearInterval(trackingInterval.value);
}

function clearHistory() {
    historyData.value = [];
    stopTracking();
}

function addHistoryDataPoint() {
    if (props.bacterieSelected) {
        historyData.value.push({
            time: Date.now(),
            vie: props.bacterieSelected.vie,
            taille: props.bacterieSelected.taille,
        });

        if (historyData.value.length > 20) {
            historyData.value.shift();
        }
    }
}

function getLifeHistoryPoints() {
    if (historyData.value.length === 0) return "";

    const maxVie = Math.max(
        ...historyData.value.map((d) => d.vie),
        props.bacterieSelected.vieInitial
    );

    return historyData.value
        .map((d, i) => {
            const x = 40 + (i / (historyData.value.length - 1 || 1)) * 340;
            const y = 130 - (d.vie / maxVie) * 100;
            return `${x},${y}`;
        })
        .join(" ");
}

function getSizeHistoryPoints() {
    if (historyData.value.length === 0) return "";

    const maxTaille = Math.max(
        ...historyData.value.map((d) => d.taille),
        props.bacterieSelected.tailleInitial * 2
    );

    return historyData.value
        .map((d, i) => {
            const x = 40 + (i / (historyData.value.length - 1 || 1)) * 340;
            const y = 130 - (d.taille / maxTaille) * 100;
            return `${x},${y}`;
        })
        .join(" ");
}

function startDragging(event: MouseEvent) {
    if (event.button === 0 && dialogRef.value) {
        mouseStartPosition.value = {
            x: event.pageX - dialogRef.value.getBoundingClientRect().left,
            y: event.pageY - dialogRef.value.getBoundingClientRect().top,
        };

        document.addEventListener("mousemove", handleDrag);
    }
}

function handleDrag(event: MouseEvent) {
    if (dialogRef.value && mouseStartPosition.value) {
        const dialogWidth = dialogRef.value.offsetWidth;
        const dialogHeight = dialogRef.value.offsetHeight;
        const newLeft = event.clientX - mouseStartPosition.value.x;
        const newTop = event.clientY - mouseStartPosition.value.y;

        if (newLeft >= 0 && newLeft + dialogWidth <= window.innerWidth) {
            dialogRef.value.style.left = `${newLeft}px`;
        }

        if (newTop >= 0 && newTop + dialogHeight <= window.innerHeight) {
            dialogRef.value.style.top = `${newTop}px`;
        }
    }
}

function stopDragging() {
    document.removeEventListener("mousemove", handleDrag);
}

onUnmounted(() => {
    stopTracking();
});

watch(
    () => props.open,
    (newValue) => {
        if (!newValue) {
            stopTracking();
        }
    }
);

watch(
    () => props.bacterieSelected,
    () => {
        clearHistory();
    },
    { deep: true }
);
</script>

<style scoped>
.dialog {
    position: absolute;
    top: 20px;
    left: 1300px;
    padding: 15px;
    background-color: rgba(40, 40, 40, 0.95);
    color: white;
    display: flex;
    flex-direction: column;
    border: 2px solid #555;
    border-radius: 8px;
    cursor: move;
    z-index: 10;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5);
    width: 450px;
    max-height: 80vh;
    overflow-y: auto;
}

.content {
    display: flex;
    flex-direction: column;
    gap: 15px;
}

.title {
    margin-top: 0;
    border-bottom: 1px solid #555;
    padding-bottom: 8px;
}

.basic-info {
    display: flex;
    flex-direction: column;
    gap: 5px;
}

.color-preview {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    display: inline-block;
    margin-right: 10px;
    vertical-align: middle;
    box-shadow:
        0 0 10px var(--glow-color),
        0 0 5px var(--glow-color);
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

.evolution-chart {
    margin-top: 15px;
    padding: 10px;
    background-color: rgba(60, 60, 60, 0.5);
    border-radius: 5px;
}

.evolution-chart h4 {
    margin-top: 0;
    margin-bottom: 10px;
    font-size: 14px;
}

.chart-container {
    margin-top: 10px;
    background-color: rgba(30, 30, 30, 0.5);
    border-radius: 5px;
    padding: 5px;
}

.empty-chart-message {
    text-align: center;
    font-style: italic;
    color: #aaa;
}

.family-tree {
    margin-top: 15px;
    padding: 10px;
    background-color: rgba(60, 60, 60, 0.5);
    border-radius: 5px;
}

.family-tree h4 {
    margin-top: 0;
    margin-bottom: 10px;
    font-size: 14px;
}

.tree-visualization {
    display: flex;
    flex-direction: column;
    align-items: center;
}

.tree-node {
    display: flex;
    flex-direction: column;
    align-items: center;
}

.node-circle {
    width: 30px;
    height: 30px;
    border-radius: 50%;
    margin-bottom: 5px;
    box-shadow:
        0 0 10px var(--glow-color),
        0 0 5px var(--glow-color);
}

.tree-children {
    display: flex;
    flex-direction: column;
    align-items: center;
}

.child-connector {
    width: 2px;
    height: 20px;
    background-color: #666;
}

.children-count {
    padding: 5px 10px;
    background-color: #444;
    border-radius: 5px;
    margin-top: 5px;
    font-size: 12px;
}

.button-container {
    display: flex;
    justify-content: flex-end;
    margin-top: 15px;
}

.close-button {
    background-color: #444;
    color: white;
    border: none;
    padding: 8px 15px;
    border-radius: 5px;
    cursor: pointer;
    transition: background-color 0.2s;
}

.close-button:hover {
    background-color: #555;
}

button {
    background-color: #444;
    color: white;
    border: none;
    padding: 5px 10px;
    border-radius: 5px;
    cursor: pointer;
    margin-right: 5px;
    font-size: 12px;
    transition: background-color 0.2s;
}

button:hover {
    background-color: #555;
}
</style>
