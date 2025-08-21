<template>
    <div class="stats-tabs">
        <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="activeTab = tab.id"
            :class="['tab-button', { active: activeTab === tab.id }]"
        >
            {{ tab.name }}
        </button>
    </div>

    <div v-if="activeTab === 'population'" class="tab-content">
        <div class="stat-cards">
            <div class="stat-card">
                <div class="stat-title">Population Totale</div>
                <div class="stat-value">
                    {{ listBacterie.length }}
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-title">Génération Max</div>
                <div class="stat-value">{{ maxGeneration }}</div>
            </div>
            <div class="stat-card">
                <div class="stat-title">Enfants Total</div>
                <div class="stat-value">{{ totalChildren }}</div>
            </div>
        </div>

        <div class="chart-section">
            <h3>Évolution de la Population</h3>
            <div class="chart-container">
                <svg width="500" height="250" viewBox="0 0 500 250">
                    <rect
                        x="40"
                        y="20"
                        width="440"
                        height="200"
                        fill="rgba(30, 30, 30, 0.5)"
                    />

                    <line x1="40" y1="220" x2="480" y2="220" stroke="white" />
                    <line x1="40" y1="20" x2="40" y2="220" stroke="white" />

                    <polyline
                        :points="getPopulationHistoryPoints()"
                        fill="none"
                        stroke="#4CAF50"
                        stroke-width="2"
                    />

                    <text
                        x="35"
                        y="25"
                        text-anchor="end"
                        fill="white"
                        font-size="12"
                    >
                        {{ maxPopulation }}
                    </text>
                    <text
                        x="35"
                        y="220"
                        text-anchor="end"
                        fill="white"
                        font-size="12"
                    >
                        0
                    </text>

                    <text
                        x="40"
                        y="235"
                        text-anchor="middle"
                        fill="white"
                        font-size="12"
                    >
                        0s
                    </text>
                    <text
                        x="480"
                        y="235"
                        text-anchor="middle"
                        fill="white"
                        font-size="12"
                        v-if="trackingInterval"
                    >
                        {{
                            Math.floor(
                                ((historyData.length - 1) * trackingInterval) /
                                    1000
                            )
                        }}s
                    </text>
                </svg>
            </div>
        </div>
    </div>

    <div v-if="activeTab === 'colors'" class="tab-content">
        <h3>Distribution des Couleurs</h3>
        <div class="color-chart-container">
            <svg width="300" height="300" viewBox="0 0 300 300">
                <g transform="translate(150, 150)">
                    <g v-for="(slice, index) in colorPieChartData" :key="index">
                        <path
                            :d="
                                describeArc(
                                    0,
                                    0,
                                    100,
                                    slice.startAngle,
                                    slice.endAngle
                                )
                            "
                            :fill="slice.color"
                            stroke="#333"
                            stroke-width="1"
                        />
                        <text
                            v-if="slice.percentage >= 5"
                            :x="getLabelX(slice.startAngle, slice.endAngle)"
                            :y="getLabelY(slice.startAngle, slice.endAngle)"
                            text-anchor="middle"
                            fill="white"
                            font-size="12"
                        >
                            {{ Math.round(slice.percentage) }}%
                        </text>
                    </g>
                </g>
            </svg>
        </div>

        <div class="color-legend">
            <div
                v-for="group in colorGroups"
                :key="group.color"
                class="color-legend-item"
            >
                <div
                    class="color-sample"
                    :style="{ backgroundColor: group.color }"
                ></div>
                <div class="color-info">
                    <span>{{ group.family }}</span>
                    <span
                        >{{ group.count }} ({{
                            ((group.count / listBacterie.length) * 100).toFixed(
                                1
                            )
                        }}%)</span
                    >
                </div>
            </div>
        </div>
    </div>

    <!-- Attributes Tab -->
    <div v-if="activeTab === 'attributes'" class="tab-content">
        <div class="comparison-table">
            <div class="table-header">
                <div>Stats</div>
                <div>+ Grand</div>
                <div>+ Petit</div>
                <div>Moyenne</div>
            </div>
            <div class="table-row">
                <div>Taille</div>
                <div>
                    {{ plusGrandPlusPetit("tailleInitial", true).toFixed(2) }}
                </div>
                <div>
                    {{ plusGrandPlusPetit("tailleInitial", false).toFixed(2) }}
                </div>
                <div>
                    {{ calculateAverage("tailleInitial").toFixed(2) }}
                </div>
            </div>
            <div class="table-row">
                <div>Reproduction</div>
                <div>
                    {{ plusGrandPlusPetit("reproduction", true).toFixed(2) }}
                </div>
                <div>
                    {{ plusGrandPlusPetit("reproduction", false).toFixed(2) }}
                </div>
                <div>{{ calculateAverage("reproduction").toFixed(2) }}</div>
            </div>
            <div class="table-row">
                <div>Vie</div>
                <div>
                    {{ plusGrandPlusPetit("vieInitial", true).toFixed(2) }}
                </div>
                <div>
                    {{ plusGrandPlusPetit("vieInitial", false).toFixed(2) }}
                </div>
                <div>{{ calculateAverage("vieInitial").toFixed(2) }}</div>
            </div>
            <div class="table-row">
                <div>Génération</div>
                <div>
                    {{ plusGrandPlusPetit("generation", true).toFixed(0) }}
                </div>
                <div>
                    {{ plusGrandPlusPetit("generation", false).toFixed(0) }}
                </div>
                <div>{{ calculateAverage("generation").toFixed(1) }}</div>
            </div>
            <div class="table-row">
                <div>Enfants</div>
                <div>
                    {{ plusGrandPlusPetit("nbrEnfant", true).toFixed(0) }}
                </div>
                <div>
                    {{ plusGrandPlusPetit("nbrEnfant", false).toFixed(0) }}
                </div>
                <div>{{ calculateAverage("nbrEnfant").toFixed(1) }}</div>
            </div>
        </div>

        <div class="attributes-grid">
            <!-- Size Distribution Chart -->
            <div class="attribute-chart">
                <h3>Distribution des Tailles</h3>
                <div class="bar-chart-container">
                    <svg width="220" height="200" viewBox="0 0 220 200">
                        <!-- Axes -->
                        <line
                            x1="40"
                            y1="170"
                            x2="200"
                            y2="170"
                            stroke="white"
                        />
                        <line x1="40" y1="30" x2="40" y2="170" stroke="white" />

                        <!-- Bars -->
                        <g
                            v-for="(bar, index) in sizeBarChartData"
                            :key="index"
                        >
                            <rect
                                :x="
                                    40 + index * (160 / sizeBarChartData.length)
                                "
                                :y="170 - bar.height"
                                :width="160 / sizeBarChartData.length - 2"
                                :height="bar.height"
                                fill="#64B5F6"
                            />
                        </g>

                        <!-- X-axis labels -->
                        <text
                            x="40"
                            y="185"
                            text-anchor="middle"
                            fill="white"
                            font-size="10"
                        >
                            {{ sizeRange.min.toFixed(1) }}
                        </text>
                        <text
                            x="200"
                            y="185"
                            text-anchor="middle"
                            fill="white"
                            font-size="10"
                        >
                            {{ sizeRange.max.toFixed(1) }}
                        </text>
                    </svg>
                </div>
            </div>

            <!-- Reproduction Distribution Chart -->
            <div class="attribute-chart">
                <h3>Distribution des Taux de Reproduction</h3>
                <div class="bar-chart-container">
                    <svg width="220" height="200" viewBox="0 0 220 200">
                        <!-- Axes -->
                        <line
                            x1="40"
                            y1="170"
                            x2="200"
                            y2="170"
                            stroke="white"
                        />
                        <line x1="40" y1="30" x2="40" y2="170" stroke="white" />

                        <!-- Bars -->
                        <g
                            v-for="(bar, index) in reproBarChartData"
                            :key="index"
                        >
                            <rect
                                :x="
                                    40 +
                                    index * (160 / reproBarChartData.length)
                                "
                                :y="170 - bar.height"
                                :width="160 / reproBarChartData.length - 2"
                                :height="bar.height"
                                fill="#FF7043"
                            />
                        </g>

                        <!-- X-axis labels -->
                        <text
                            x="40"
                            y="185"
                            text-anchor="middle"
                            fill="white"
                            font-size="10"
                        >
                            {{ reproRange.min.toFixed(1) }}
                        </text>
                        <text
                            x="200"
                            y="185"
                            text-anchor="middle"
                            fill="white"
                            font-size="10"
                        >
                            {{ reproRange.max.toFixed(1) }}
                        </text>
                    </svg>
                </div>
            </div>
        </div>

        <div class="attributes-grid">
            <!-- Life Distribution Chart -->
            <div class="attribute-chart">
                <h3>Distribution des Vies Initiales</h3>
                <div class="bar-chart-container">
                    <svg width="220" height="200" viewBox="0 0 220 200">
                        <!-- Axes -->
                        <line
                            x1="40"
                            y1="170"
                            x2="200"
                            y2="170"
                            stroke="white"
                        />
                        <line x1="40" y1="30" x2="40" y2="170" stroke="white" />

                        <!-- Bars -->
                        <g
                            v-for="(bar, index) in lifeBarChartData"
                            :key="index"
                        >
                            <rect
                                :x="
                                    40 + index * (160 / lifeBarChartData.length)
                                "
                                :y="170 - bar.height"
                                :width="160 / lifeBarChartData.length - 2"
                                :height="bar.height"
                                fill="#81C784"
                            />
                        </g>

                        <!-- X-axis labels -->
                        <text
                            x="40"
                            y="185"
                            text-anchor="middle"
                            fill="white"
                            font-size="10"
                        >
                            {{ lifeRange.min.toFixed(1) }}
                        </text>
                        <text
                            x="200"
                            y="185"
                            text-anchor="middle"
                            fill="white"
                            font-size="10"
                        >
                            {{ lifeRange.max.toFixed(1) }}
                        </text>
                    </svg>
                </div>
            </div>

            <!-- Generation Distribution Chart -->
            <div class="attribute-chart">
                <h3>Distribution des Générations</h3>
                <div class="bar-chart-container">
                    <svg width="220" height="200" viewBox="0 0 220 200">
                        <!-- Axes -->
                        <line
                            x1="40"
                            y1="170"
                            x2="200"
                            y2="170"
                            stroke="white"
                        />
                        <line x1="40" y1="30" x2="40" y2="170" stroke="white" />

                        <!-- Bars -->
                        <g v-for="(bar, index) in genBarChartData" :key="index">
                            <rect
                                :x="40 + index * (160 / genBarChartData.length)"
                                :y="170 - bar.height"
                                :width="160 / genBarChartData.length - 2"
                                :height="bar.height"
                                fill="#9575CD"
                            />
                        </g>

                        <!-- X-axis labels -->
                        <text
                            x="40"
                            y="185"
                            text-anchor="middle"
                            fill="white"
                            font-size="10"
                        >
                            1
                        </text>
                        <text
                            x="200"
                            y="185"
                            text-anchor="middle"
                            fill="white"
                            font-size="10"
                        >
                            {{ maxGeneration }}
                        </text>
                    </svg>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import Bacterie from "../models/bacterie.ts";
import ColorFamily from "@/models/colorFamily.ts";

const listBacterie = defineModel<Bacterie[]>({
    default: () => [],
});

const activeTab = ref<string>("population");
const historyData = ref<Array<{ time: number; count: number }>>([]);
let trackingInterval: number | undefined;
const intervalTime = 1000;
const tabs = [
    { id: "population", name: "Population" },
    { id: "colors", name: "Couleurs" },
    { id: "attributes", name: "Attributs" },
];

const maxGeneration = computed(() => {
    if (listBacterie.value.length === 0) return 1;
    return Math.max(...listBacterie.value.map((b) => b.generation));
});

const totalChildren = computed(() => {
    if (listBacterie.value.length === 0) return 0;
    return listBacterie.value.reduce((sum, b) => sum + b.nbrEnfant, 0);
});

const maxPopulation = computed(() => {
    if (historyData.value.length === 0) return listBacterie.value.length;
    return Math.max(
        ...historyData.value.map((d) => d.count),
        listBacterie.value.length
    );
});

function getPopulationHistoryPoints() {
    if (historyData.value.length <= 1) return "40,220 480,220";

    return historyData.value
        .map((d, i) => {
            const x = 40 + (i / (historyData.value.length - 1)) * 440;
            const y = 220 - (d.count / maxPopulation.value) * 200;
            return `${x},${y}`;
        })
        .join(" ");
}

const colorFamilies: ColorFamily[] = [
    new ColorFamily(
        "Rouge",
        ["#FF0000", "#FF6666", "#990000"],
        (r, g, b) => r > 1.5 * g && r > 1.5 * b
    ),
    new ColorFamily(
        "Vert",
        ["#00FF00", "#66FF66", "#009900"],
        (r, g, b) => g > 1.5 * r && g > 1.5 * b
    ),
    new ColorFamily(
        "Bleu",
        ["#0000FF", "#6666FF", "#000099"],
        (r, g, b) => b > 1.5 * r && b > 1.5 * g
    ),
    new ColorFamily(
        "Jaune",
        ["#FFFF00", "#FFFF66", "#999900"],
        (r, g, b) => r > 0.7 * 255 && g > 0.7 * 255 && b < 0.5 * 255
    ),
    new ColorFamily(
        "Cyan",
        ["#00FFFF", "#66FFFF", "#009999"],
        (r, g, b) => b > 0.7 * 255 && g > 0.7 * 255 && r < 0.5 * 255
    ),
    new ColorFamily(
        "Magenta",
        ["#FF00FF", "#FF66FF", "#990099"],
        (r, g, b) => r > 0.7 * 255 && b > 0.7 * 255 && g < 0.5 * 255
    ),
    new ColorFamily(
        "Orange",
        ["#FFA500", "#FFB366", "#995C00"],
        (r, g, b) =>
            r > 0.7 * 255 && g > 0.4 * 255 && g < 0.7 * 255 && b < 0.3 * 255
    ),
    new ColorFamily(
        "Rose",
        ["#FFC0CB", "#FFD6DD", "#996B73"],
        (r, g, b) =>
            r > 0.7 * 255 && g > 0.5 * 255 && b > 0.5 * 255 && r > g && r > b
    ),
    new ColorFamily(
        "Marron",
        ["#8B4513", "#A35C2A", "#613014"],
        (r, g, b) =>
            r > 0.3 * 255 &&
            r < 0.7 * 255 &&
            g > 0.1 * 255 &&
            g < 0.5 * 255 &&
            b < 0.3 * 255
    ),
    new ColorFamily(
        "Noir",
        ["#000000", "#333333", "#000000"],
        (r, g, b) => r < 0.2 * 255 && g < 0.2 * 255 && b < 0.2 * 255
    ),
    new ColorFamily(
        "Blanc",
        ["#FFFFFF", "#EEEEEE", "#DDDDDD"],
        (r, g, b) => r > 0.8 * 255 && g > 0.8 * 255 && b > 0.8 * 255
    ),
    new ColorFamily(
        "Gris",
        ["#808080", "#AAAAAA", "#555555"],
        (r, g, b) =>
            Math.abs(r - g) < 30 && Math.abs(r - b) < 30 && Math.abs(g - b) < 30
    ),
];

function getColorFamily(color: string) {
    let r, g, b;

    if (color.startsWith("#")) {
        r = parseInt(color.slice(1, 3), 16);
        g = parseInt(color.slice(3, 5), 16);
        b = parseInt(color.slice(5, 7), 16);
    } else if (color.startsWith("rgb")) {
        const rgbValues = color
            .substring(4, color.length - 1)
            .split(",")
            .map((v: string) => parseInt(v.trim()));

        r = rgbValues[0];
        g = rgbValues[1];
        b = rgbValues[2];
    } else {
        return "Autre";
    }

    for (const family of colorFamilies) {
        if (family.test(r, g, b)) {
            return family.name;
        }
    }

    return "Autre";
}

function getFamilyRepresentativeColor(familyName: string) {
    const family = colorFamilies.find((f) => f.name === familyName);
    return family ? family.range[0] : "#CCCCCC";
}

const colorGroups = computed(() => {
    if (listBacterie.value.length === 0) return [];

    const exactColorCounts: Record<string, number> = {};

    listBacterie.value.forEach((bacterie: Bacterie) => {
        if (exactColorCounts[bacterie.color]) {
            exactColorCounts[bacterie.color]++;
        } else {
            exactColorCounts[bacterie.color] = 1;
        }
    });

    const familyCounts: Record<string, FamilyCount> = {};

    Object.entries(exactColorCounts).forEach(([color, count]) => {
        const family = getColorFamily(color);

        if (familyCounts[family]) {
            familyCounts[family].count += count;
            familyCounts[family].colors.push({ color, count });
        } else {
            familyCounts[family] = {
                count,
                colors: [{ color, count }],
                representativeColor: getFamilyRepresentativeColor(family),
            };
        }
    });

    return Object.entries(familyCounts)
        .map(([family, data]) => ({
            family,
            color: data.representativeColor,
            count: data.count,
            colors: data.colors.sort(
                (a: { count: number }, b: { count: number }) =>
                    b.count - a.count
            ),
        }))
        .sort((a, b) => b.count - a.count);
});

const colorPieChartData = computed(() => {
    if (listBacterie.value.length === 0) return [];

    const total = listBacterie.value.length;
    let startAngle = 0;

    return colorGroups.value.map((group) => {
        const percentage = (group.count / total) * 100;
        const endAngle = startAngle + (percentage / 100) * 360;

        const slice = {
            color: group.color,
            percentage,
            startAngle,
            endAngle,
            family: group.family,
        };

        startAngle = endAngle;
        return slice;
    });
});

function describeArc(
    x: number,
    y: number,
    radius: number,
    startAngle: number,
    endAngle: number
) {
    const startRad = ((startAngle - 90) * Math.PI) / 180;
    const endRad = ((endAngle - 90) * Math.PI) / 180;

    const x1 = x + radius * Math.cos(startRad);
    const y1 = y + radius * Math.sin(startRad);
    const x2 = x + radius * Math.cos(endRad);
    const y2 = y + radius * Math.sin(endRad);

    const largeArcFlag = endAngle - startAngle <= 180 ? "0" : "1";

    return `M ${x} ${y} L ${x1} ${y1} A ${radius} ${radius} 0 ${largeArcFlag} 1 ${x2} ${y2} Z`;
}

function getLabelX(startAngle: number, endAngle: number) {
    const midAngle = (((startAngle + endAngle) / 2 - 90) * Math.PI) / 180;
    return 65 * Math.cos(midAngle);
}

function getLabelY(startAngle: number, endAngle: number) {
    const midAngle = (((startAngle + endAngle) / 2 - 90) * Math.PI) / 180;
    return 65 * Math.sin(midAngle);
}

const sizeRange = computed(() => {
    if (listBacterie.value.length === 0) return { min: 0, max: 10 };

    const sizes = listBacterie.value.map((b) => b.tailleInitial);
    return {
        min: Math.min(...sizes),
        max: Math.max(...sizes),
    };
});

const sizeBarChartData = computed(() => {
    if (listBacterie.value.length === 0) return [];

    const numBins = 10;
    const range = sizeRange.value.max - sizeRange.value.min;
    const binSize = range / numBins;

    const bins = Array(numBins).fill(0);

    listBacterie.value.forEach((bacterie) => {
        const binIndex = Math.min(
            Math.floor(
                (bacterie.tailleInitial - sizeRange.value.min) / binSize
            ),
            numBins - 1
        );
        bins[binIndex]++;
    });

    const maxCount = Math.max(...bins);

    return bins.map((count) => ({
        count,
        height: count > 0 ? (count / maxCount) * 140 : 0,
    }));
});

const reproRange = computed(() => {
    if (listBacterie.value.length === 0) return { min: 0, max: 10 };

    const values = listBacterie.value.map((b) => b.reproduction);
    return {
        min: Math.min(...values),
        max: Math.max(...values),
    };
});

const reproBarChartData = computed(() => {
    if (listBacterie.value.length === 0) return [];

    const numBins = 10;
    const range = reproRange.value.max - reproRange.value.min;
    const binSize = range / numBins;

    const bins = Array(numBins).fill(0);

    listBacterie.value.forEach((bacterie) => {
        const binIndex = Math.min(
            Math.floor(
                (bacterie.reproduction - reproRange.value.min) / binSize
            ),
            numBins - 1
        );
        bins[binIndex]++;
    });

    const maxCount = Math.max(...bins);

    return bins.map((count) => ({
        count,
        height: count > 0 ? (count / maxCount) * 140 : 0,
    }));
});

const lifeRange = computed(() => {
    if (listBacterie.value.length === 0) return { min: 0, max: 20 };

    const values = listBacterie.value.map((b) => b.vieInitial);
    return {
        min: Math.min(...values),
        max: Math.max(...values),
    };
});

const lifeBarChartData = computed(() => {
    if (listBacterie.value.length === 0) return [];

    const numBins = 10;
    const range = lifeRange.value.max - lifeRange.value.min;
    const binSize = range / numBins;

    const bins = Array(numBins).fill(0);

    listBacterie.value.forEach((bacterie) => {
        const binIndex = Math.min(
            Math.floor((bacterie.vieInitial - lifeRange.value.min) / binSize),
            numBins - 1
        );
        bins[binIndex]++;
    });

    const maxCount = Math.max(...bins);

    return bins.map((count) => ({
        count,
        height: count > 0 ? (count / maxCount) * 140 : 0,
    }));
});

const genBarChartData = computed(() => {
    if (listBacterie.value.length === 0) return [];

    const numBins = Math.min(10, maxGeneration.value);
    const binSize = maxGeneration.value / numBins;

    const bins = Array(numBins).fill(0);

    listBacterie.value.forEach((bacterie) => {
        const binIndex = Math.min(
            Math.floor((bacterie.generation - 1) / binSize),
            numBins - 1
        );
        bins[binIndex]++;
    });

    const maxCount = Math.max(...bins);

    return bins.map((count) => ({
        count,
        height: count > 0 ? (count / maxCount) * 140 : 0,
    }));
});

function plusGrandPlusPetit(
    attribut:
        | "tailleInitial"
        | "reproduction"
        | "vieInitial"
        | "generation"
        | "nbrEnfant",
    grand: boolean
): number {
    if (listBacterie.value.length === 0) return 0;

    let nbr: number = grand ? 0 : Number.MAX_VALUE;
    if (grand) {
        listBacterie.value.forEach((bacterie) => {
            if (bacterie[attribut] > nbr) {
                nbr = bacterie[attribut];
            }
        });
    } else {
        listBacterie.value.forEach((bacterie) => {
            if (bacterie[attribut] < nbr) {
                nbr = bacterie[attribut];
            }
        });
    }
    return nbr;
}

function calculateAverage(
    attribut:
        | "tailleInitial"
        | "reproduction"
        | "vieInitial"
        | "generation"
        | "nbrEnfant"
): number {
    if (listBacterie.value.length === 0) return 0;

    const sum = listBacterie.value.reduce(
        (acc, bacterie) => acc + bacterie[attribut],
        0
    );
    return sum / listBacterie.value.length;
}

onMounted(() => {
    trackingInterval = window.setInterval(addHistoryDataPoint, intervalTime);
});

function addHistoryDataPoint() {
    historyData.value.push({
        time: Date.now(),
        count: listBacterie.value.length,
    });

    if (historyData.value.length > 50) {
        historyData.value.shift();
    }
}
</script>

<style scoped>
.stats-tabs {
    display: flex;
    gap: 5px;
    margin-bottom: 15px;
}

.tab-button {
    flex: 1;
    background-color: #333;
    color: #ccc;
    border: 1px solid #555;
    padding: 8px 0;
    cursor: pointer;
    border-radius: 4px;
    transition: all 0.2s ease;
}

.tab-button.active {
    background-color: #4caf50;
    color: white;
    border-color: #4caf50;
}

.tab-button:hover:not(.active) {
    background-color: #444;
}

.tab-content {
    margin-bottom: 15px;
    color: white;
    width: 33rem;
}

.stat-cards {
    display: flex;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 20px;
}

.stat-card {
    flex: 1;
    background-color: rgba(60, 60, 60, 0.5);
    padding: 10px;
    border-radius: 5px;
    text-align: center;
}

.stat-title {
    font-size: 14px;
    margin-bottom: 5px;
    color: #aaa;
}

.stat-value {
    font-size: 20px;
    font-weight: bold;
}

.chart-section {
    background-color: rgba(60, 60, 60, 0.5);
    padding: 10px;
}

.stats-tabs {
    display: flex;
    gap: 5px;
    margin-bottom: 15px;
}

.tab-button {
    flex: 1;
    background-color: #333;
    color: #ccc;
    border: 1px solid #555;
    padding: 8px 0;
    cursor: pointer;
    border-radius: 4px;
    transition: all 0.2s ease;
}

.tab-button.active {
    background-color: #4caf50;
    color: white;
    border-color: #4caf50;
}

.tab-button:hover:not(.active) {
    background-color: #444;
}

.tab-content {
    margin-bottom: 15px;
}

.stat-cards {
    display: flex;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 20px;
}

.stat-card {
    flex: 1;
    background-color: rgba(60, 60, 60, 0.5);
    padding: 10px;
    border-radius: 5px;
    text-align: center;
}

.stat-title {
    font-size: 14px;
    margin-bottom: 5px;
    color: #aaa;
}

.stat-value {
    font-size: 20px;
    font-weight: bold;
}

.chart-section {
    background-color: rgba(60, 60, 60, 0.5);
    padding: 10px;
    border-radius: 5px;
    margin-bottom: 15px;
}

.chart-section h3 {
    margin-top: 0;
    margin-bottom: 10px;
    font-size: 16px;
    color: #ddd;
    text-align: center;
}

.chart-container {
    display: flex;
    justify-content: center;
    padding: 5px;
}

.color-chart-container {
    display: flex;
    justify-content: center;
    margin-bottom: 15px;
}

.color-legend {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: center;
    margin-top: 10px;
}

.color-legend-item {
    display: flex;
    align-items: center;
    background-color: rgba(60, 60, 60, 0.5);
    padding: 5px 8px;
    border-radius: 4px;
    font-size: 12px;
}

.color-sample {
    width: 15px;
    height: 15px;
    border-radius: 3px;
    margin-right: 6px;
    border: 1px solid rgba(255, 255, 255, 0.3);
}

.color-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.comparison-table {
    background-color: rgba(60, 60, 60, 0.5);
    border-radius: 5px;
    margin-bottom: 15px;
    overflow: hidden;
}

.table-header,
.table-row {
    display: grid;
    grid-template-columns: 2fr 1fr 1fr 1fr;
    text-align: center;
}

.table-header {
    background-color: rgba(80, 80, 80, 0.8);
    font-weight: bold;
    padding: 10px 0;
}

.table-row {
    border-top: 1px solid rgba(100, 100, 100, 0.5);
    padding: 8px 0;
}

.table-row:hover {
    background-color: rgba(80, 80, 80, 0.5);
}

.attributes-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
    margin-bottom: 15px;
}

.attribute-chart {
    background-color: rgba(60, 60, 60, 0.5);
    padding: 10px;
    border-radius: 5px;
}

.attribute-chart h3 {
    margin-top: 0;
    margin-bottom: 10px;
    font-size: 14px;
    color: #ddd;
    text-align: center;
}

.bar-chart-container {
    display: flex;
    justify-content: center;
}

.button-container button {
    flex: 1;
    padding: 8px 10px;
    background-color: #444;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    transition: background-color 0.2s;
}

.button-container button:hover {
    background-color: #555;
}

button {
    font-family: inherit;
}

@media (max-width: 600px) {
    .attributes-grid {
        grid-template-columns: 1fr;
    }

    .stat-cards {
        flex-direction: column;
    }
}
</style>
