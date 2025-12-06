<template>
    <div>
        <div class="player-anim" :style="playerAnimStyle" />
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from "vue";

const props = defineProps({
    idleImageUrl: { type: String, required: true },
    width: { type: Number, required: true },
    height: { type: Number, required: true },
    frames: { type: Number, required: true },
    frameRate: { type: Number, default: 8 },
    scale: { type: Number, default: 1 },
});

const animFrameId = ref<number | null>(null);
const lastTimestamp = ref<number | null>(null);
const frameIndex = ref(0);
const frameTimer = ref(0);

const playerAnimStyle = computed(() => ({
    width: `${props.width}px`,
    height: `${props.height * 8}px`,
    backgroundImage: `url(${props.idleImageUrl})`,
    backgroundSize: `${props.width * props.frames}px`,
    backgroundPosition: `-${frameIndex.value * props.width}px`,
}));

function animFrame(timestamp?: number) {
    if (timestamp == null) {
        animFrameId.value = requestAnimationFrame(animFrame);
        return;
    }

    if (lastTimestamp.value == null) lastTimestamp.value = timestamp;
    const dt = (timestamp - lastTimestamp.value) / 1000;
    lastTimestamp.value = timestamp;

    frameTimer.value += dt;
    const frameDuration = 1 / props.frameRate;

    if (frameTimer.value >= frameDuration) {
        frameTimer.value -= frameDuration;
        frameIndex.value = (frameIndex.value + 1) % props.frames;
    }

    animFrameId.value = requestAnimationFrame(animFrame);
}

function onVisibilityChange() {
    if (document.visibilityState === "visible" && animFrameId.value === null) {
        lastTimestamp.value = null;
        animFrame();
    } else if (
        document.visibilityState !== "visible" &&
        animFrameId.value !== null
    ) {
        cancelAnimationFrame(animFrameId.value);
        animFrameId.value = null;
    }
}

onMounted(() => {
    document.addEventListener("visibilitychange", onVisibilityChange);
    animFrame();
});

onBeforeUnmount(() => {
    document.removeEventListener("visibilitychange", onVisibilityChange);
    if (animFrameId.value !== null) cancelAnimationFrame(animFrameId.value);
});
</script>

<style scoped>
.player-anim {
    background-repeat: no-repeat;
    image-rendering: pixelated;
}
</style>
