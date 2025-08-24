<template>
    <div class="leaderboard-container">
        <h2>Top 10</h2>
        <ol v-if="records.length > 0">
            <li v-for="(record, key) in records" :key="key">
                <span class="player-pseudo">{{ record.pseudo }}</span>
                <span class="score-value">{{ record.score }}</span>
            </li>
        </ol>
        <p v-else>No records found.</p>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { GameType } from "@/models/enums/gameType.ts";
import req from "@/services/scoreService.ts";
import ClassementDto from "@/models/classementDto.ts";

const props = defineProps<{
    gameName: GameType;
}>();

const records = ref<ClassementDto[]>([]);

async function afficherClassement() {
    try {
        const result = await req.getLeaderboard(props.gameName);
        records.value = result.data;
    } catch (error) {
        console.error("Failed to fetch leaderboard:", error);
    }
}

onMounted(() => {
    afficherClassement();
});
</script>

<style scoped>
.leaderboard-container {
    background-color: var(--marron);
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    color: #fff;
    width: 15rem;
    margin-right: 20px;
    text-align: center;
}

h2 {
    font-size: 1.8rem;
    margin-top: 0;
    color: antiquewhite;
}

ol {
    list-style-type: none;
    padding: 0;
}

li {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

li:last-child {
    border-bottom: none;
}

.player-pseudo {
    font-weight: bold;
}

.score-value {
    font-weight: normal;
}
</style>
