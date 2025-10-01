<template>
    <div class="body-container">
        <div class="main-container">
            <Chat />
            <div class="game-content">
                <div class="header">
                    <slot name="header" />
                    <RecordComponent
                        v-if="props.gameName"
                        ref="recordcomponentRef"
                        :game-name="props.gameName"
                    />
                </div>
                <div class="game-container">
                    <slot />
                </div>
            </div>

            <Leaderboard
                v-if="props.gameName"
                ref="leaderboardRef"
                :gameName="props.gameName"
            />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import Chat from "@/components/Chat.vue";
import RecordComponent from "@/components/RecordComponent.vue";
import Leaderboard from "@/components/LeaderBord.vue";

const props = defineProps<{ gameName: string }>();

const recordcomponentRef = ref<RecordComponent>();
const leaderboardRef = ref<Leaderboard>();

function refreshComponents() {
    recordcomponentRef.value?.refresh();
    leaderboardRef.value?.refresh();
}

defineExpose({
    refresh: refreshComponents,
});
</script>

<style scoped>
.main-container {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    gap: 4rem;
}

.game-container {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    align-items: center;
}

.game-content {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    align-items: center;
}

.header {
    padding: 10px;
    border-radius: 6px;
    text-align: center;
    display: flex;
    align-items: center;
    gap: 3rem;
    font-weight: bold;
    font-size: 1rem;
}

.header > * {
    background: var(--marron-fonce);
    padding: 10px;
    border-radius: 6px;
    text-align: center;
}
</style>
