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
import { GameType } from "@/models/enums/gameType.ts";

const props = defineProps<{ gameName: GameType }>();

const recordcomponentRef = ref<InstanceType<typeof RecordComponent>>();
const leaderboardRef = ref<InstanceType<typeof Leaderboard>>();

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

.header {
    padding: 0.8rem 1.2rem;
    border-radius: 8px;
    text-align: center;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 3rem;
    font-weight: bold;
    font-size: 1.1rem;
    color: var(--futurist-accent);
    text-shadow: 0 0 8px var(--futurist-shadow);
    position: relative;
}

.header > * {
    background: var(--futurist-blur-bg);
    padding: 0.8rem 1rem;
    border-radius: 10px;
    border: 1px solid var(--futurist-border);
    box-shadow: 0 0 12px var(--futurist-shadow);
    transition:
        transform 0.25s ease,
        box-shadow 0.25s ease;
}

.header > *:hover {
    transform: scale(1.05);
    box-shadow: 0 0 20px var(--futurist-shadow-strong);
}

.game-container {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
    align-items: center;
    justify-content: center;
    width: 100%;
    min-height: 20rem;
    border-radius: 1rem;
    transition: all 0.3s ease;
}
</style>
