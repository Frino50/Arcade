<template>
    <div class="leaderboard-container" v-loading="isLoading">
        <h2>Leaderboard</h2>
        <ol v-if="records.length > 0">
            <li v-for="(record, key) in records" :key="key">
                <span class="player-pseudo">{{ record.pseudo }}</span>
                <span class="score-value">{{ record.score }}</span>
            </li>
        </ol>
        <p v-else>Pas encore de record</p>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { GameType } from "@/models/enums/gameType.ts";
import req from "@/services/scoreService.ts";
import ClassementDto from "@/models/dtos/classementDto.ts";

const props = defineProps<{
    gameName: GameType;
}>();

const records = ref<ClassementDto[]>([]);
const isLoading = ref(false);

async function afficherClassement() {
    try {
        isLoading.value = true;
        const result = await req.getLeaderboard(props.gameName);
        records.value = result.data;
    } finally {
        isLoading.value = false;
    }
}

defineExpose({
    refresh: afficherClassement,
});

onMounted(() => {
    afficherClassement();
});
</script>

<style scoped>
.leaderboard-container {
    background: radial-gradient(
        circle at top left,
        var(--futurist-bg-dark),
        var(--futurist-bg-light)
    );
    border-radius: 1rem;
    padding: 1.5rem;
    width: 15rem;
    text-align: center;
    color: var(--futurist-text-light);
    box-shadow: 0 0 20px var(--futurist-shadow);
    border: 1px solid var(--futurist-border);
    backdrop-filter: blur(8px);
    position: relative;
    overflow: hidden;
}

.leaderboard-container::before {
    content: "";
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: conic-gradient(
        from 180deg,
        transparent,
        var(--futurist-shadow),
        transparent
    );
    animation: rotate 8s linear infinite;
    z-index: 0;
}

@keyframes rotate {
    100% {
        transform: rotate(360deg);
    }
}

h2 {
    font-size: 1.6rem;
    margin-top: 0;
    margin-bottom: 1rem;
    color: var(--futurist-accent);
    text-shadow: 0 0 10px var(--futurist-accent);
    z-index: 1;
    position: relative;
}

ol {
    list-style-type: none;
    padding: 0;
    margin: 0;
    z-index: 1;
    position: relative;
}

li {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.6rem 0.5rem;
    margin: 0.3rem 0;
    border-radius: 10px;
    background: var(--futurist-list-bg);
    border: 1px solid var(--futurist-border);
    box-shadow: 0 0 8px var(--futurist-shadow);
    transition:
        transform 0.2s ease,
        box-shadow 0.2s ease,
        background 0.2s ease;
}

li:hover {
    transform: scale(1.05);
    background: var(--futurist-list-bg-hover);
    box-shadow: 0 0 15px var(--futurist-shadow);
}

.player-pseudo {
    font-weight: bold;
    color: var(--futurist-accent);
    text-shadow: 0 0 6px var(--futurist-accent);
    font-size: 1rem;
}

.score-value {
    font-weight: 500;
    color: var(--futurist-text-weak);
    font-size: 0.95rem;
    text-shadow: 0 0 5px var(--futurist-shadow);
}
</style>
