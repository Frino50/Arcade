<template>
    <div style="image-rendering: pixelated" :style="playerAnimStyle" />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from "vue";
import spriteService from "@/services/spriteService.ts";

const props = defineProps({
    spriteSrc: { type: String, default: "/assets/Mushroom-Idle.png" },
    speed: { type: Number, default: 120 },
    width: { type: Number, default: 120 },
    height: { type: Number, default: 120 },
    frames: { type: Number, default: 8 },
    frameRate: { type: Number, default: 8 },
    scale: { type: Number, default: 1 },
});

const animFrameId = ref<number | null>(null);
const lastTimestamp = ref<number | null>(null);
const frameIndex = ref(0);
const frameTimer = ref(0);
const spriteBlobUrl = ref<string>("");
const frameWidth = props.width / props.frames;

const baseStyle = {
    width: `${frameWidth}px`,
    height: `${props.height}px`,
    transformOrigin: "bottom",
};
const playerAnimStyle = computed(() => ({
    ...baseStyle,
    transform: `scale(${props.scale})`,
    backgroundImage: spriteBlobUrl.value ? `url(${spriteBlobUrl.value})` : "",
    backgroundPosition: `-${frameIndex.value * frameWidth}px`,
}));

function animFrame() {
    const now = performance.now();

    if (lastTimestamp.value == null) {
        lastTimestamp.value = now;
    }

    const dt = (now - lastTimestamp.value) / 1000;
    lastTimestamp.value = now;

    frameTimer.value += dt;

    if (frameTimer.value >= 1 / props.frameRate) {
        frameTimer.value = 0;

        frameIndex.value++;
        if (frameIndex.value === props.frames) {
            frameIndex.value = 0;
        }
    }

    animFrameId.value = requestAnimationFrame(animFrame);
}

function onVisibilityChange() {
    if (document.visibilityState === "visible" && animFrameId.value === null) {
        lastTimestamp.value = null;
        animFrame();
    }
}

onMounted(async () => {
    spriteBlobUrl.value = await spriteService.getImage(props.spriteSrc);
    document.addEventListener("visibilitychange", onVisibilityChange);
    animFrame();
});

onBeforeUnmount(() => {
    document.removeEventListener("visibilitychange", onVisibilityChange);
    if (animFrameId.value !== null) cancelAnimationFrame(animFrameId.value);
});
</script>
