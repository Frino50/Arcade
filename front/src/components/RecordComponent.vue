<template>
    <div v-loading="isLoading">
        <div>Record</div>
        <div>{{ bestScore }}</div>
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
