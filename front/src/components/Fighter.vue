<template>
    <div class="fighter-wrapper">
        <div style="image-rendering: pixelated" :style="playerAnimStyle" />
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from "vue";

const props = defineProps({
    spriteSrc: { type: String, default: "/assets/Mushroom-Idle.png" },
    speed: { type: Number, default: 120 },
    width: { type: Number, default: 120 },
    height: { type: Number, default: 120 },
    frames: { type: Number, default: 8 },
    frameRate: { type: Number, default: 8 },
});

const animFrameId = ref<number | null>(null);
const lastTimestamp = ref<number | null>(null);

const frameIndex = ref(0);
const frameTimer = ref(0);

const playerAnimStyle = computed(() => {
    return {
        width: `${props.width / props.frames}px`,
        height: `${props.height}px`,
        backgroundImage: `url(${props.spriteSrc})`,
        backgroundPosition: `-${(frameIndex.value * props.width) / props.frames}px`,
        backgroundRepeat: "no-repeat",
    };
});

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
        frameTimer.value = 0;
        frameIndex.value++;
        frameIndex.value %= props.frames;
    }

    animFrameId.value = requestAnimationFrame(animFrame);
}

function onVisibilityChange() {
    if (document.visibilityState === "visible" && animFrameId.value === null) {
        lastTimestamp.value = null;
        animFrame();
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
.fighter-wrapper {
    height: 10rem;
    display: flex;
    align-items: flex-end;
}
</style>
