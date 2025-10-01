<template>
    <div class="score-box" v-loading="isLoading">
        <div>Record</div>
        <div class="score">{{ bestScore }}</div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import scoreService from "@/services/scoreService.ts";
import { GameType } from "@/models/enums/gameType.ts";

const props = defineProps<{
    gameName: GameType;
}>();

const bestScore = ref<number>(0);
const isLoading = ref<boolean>(false);

async function getBestScore() {
    try {
        isLoading.value = true;
        const response = await scoreService.getBestScore(props.gameName);
        bestScore.value = response.data;
    } finally {
        isLoading.value = false;
    }
}

defineExpose({
    refresh: getBestScore,
});

onMounted(() => {
    getBestScore();
});
</script>

<style scoped>
.score-box {
    background: var(--marron-fonce);
    padding: 10px;
    border-radius: 6px;
    text-align: center;
}
.score {
    font-weight: bold;
    font-size: 1.2rem;
}
</style>
