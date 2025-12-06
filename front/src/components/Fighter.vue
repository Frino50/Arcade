<template>
    <div class="stage" @mousedown.left="startAttack">
        <div class="player-container" :style="playerStyle">
            <div style="image-rendering: pixelated" :style="playerAnimStyle" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from "vue";

const props = defineProps({
    idleSpriteSrc: { type: String, default: "/assets/Mushroom-Idle.png" },
    runSpriteSrc: { type: String, default: "/assets/Mushroom-Run.png" },
    attackSpriteSrc: { type: String, default: "/assets/Mushroom-Attack.png" },

    speed: { type: Number, default: 120 },

    runFrameCount: { type: Number, default: 8 },
    idleFrameCount: { type: Number, default: 7 },
    attackFrameCount: { type: Number, default: 10 },

    runFrameRate: { type: Number, default: 12 },
    idleFrameRate: { type: Number, default: 8 },
    attackFrameRate: { type: Number, default: 16 },

    scale: { type: Number, default: 1 },
    hitbox: { type: Number, default: 1 },
    direction: { type: Boolean, default: false },
});

const x = ref(-75);
const isWalking = ref(false);
const movingRight = ref<boolean>(false);
const isAttacking = ref(false);
const animFrameId = ref<number | null>(null);
const lastTimestamp = ref<number | null>(null);

const frameIndex = ref(0);
const frameTimer = ref(0);

const idleWidth = ref(0);
const runWidth = ref(0);
const attackWidth = ref(0);

function loadImage(src: string, targetRef: typeof idleWidth) {
    const img = new Image();
    img.src = src;
    img.onload = () => (targetRef.value = img.width);
}

const currentSpriteSrc = computed(() => {
    if (isAttacking.value) return props.attackSpriteSrc;
    if (isWalking.value) return props.runSpriteSrc;
    return props.idleSpriteSrc;
});

const currentFrameCount = computed(() => {
    if (isAttacking.value) return props.attackFrameCount;
    if (isWalking.value) return props.runFrameCount;
    return props.idleFrameCount;
});

const currentFrameRate = computed(() => {
    if (isAttacking.value) return props.attackFrameRate;
    if (isWalking.value) return props.runFrameRate;
    return props.idleFrameRate;
});

const frameWidth = computed(() => {
    if (isAttacking.value && attackWidth.value > 0)
        return attackWidth.value / props.attackFrameCount;
    if (isWalking.value && runWidth.value > 0)
        return runWidth.value / props.runFrameCount;
    if (idleWidth.value > 0) return idleWidth.value / props.idleFrameCount;
    return 64;
});

const playerStyle = computed(() => {
    const scaleX = (movingRight.value ? -1 : 1) * (props.direction ? -1 : 1);
    return {
        transform: `translate3d(${x.value}px, 0, 0) scaleX(${scaleX})`,
    };
});

const playerAnimStyle = computed(() => {
    const divWidth = frameWidth.value * props.scale * props.hitbox;
    const frameW = frameWidth.value * props.scale;

    return {
        width: `${divWidth}px`,
        height: `${64 * props.scale}px`,
        backgroundImage: `url(${currentSpriteSrc.value})`,
        backgroundSize: `${frameW * currentFrameCount.value}px 100%`,
        backgroundPosition: `calc(-${frameIndex.value * frameW}px + ${(divWidth - frameW) / 2}px) 0`,
        backgroundRepeat: "no-repeat",
    };
});

function startMoving(right: boolean) {
    if (isAttacking.value) return;
    movingRight.value = right;
    isWalking.value = true;
    if (animFrameId.value === null) {
        lastTimestamp.value = null;
        animFrame();
    }
}

function stopMoving() {
    isWalking.value = false;
}

function startAttack() {
    if (isAttacking.value) return;
    isAttacking.value = true;
    frameIndex.value = 0;
    frameTimer.value = 0;

    const attackDuration = props.attackFrameCount / props.attackFrameRate;
    setTimeout(() => {
        isAttacking.value = false;
        frameIndex.value = 0;
    }, attackDuration * 1000);
}

function animFrame(timestamp?: number) {
    if (timestamp == null) {
        animFrameId.value = requestAnimationFrame(animFrame);
        return;
    }

    if (lastTimestamp.value == null) lastTimestamp.value = timestamp;
    const dt = (timestamp - lastTimestamp.value) / 1000;
    lastTimestamp.value = timestamp;

    if (isWalking.value && !isAttacking.value) {
        x.value += (movingRight.value ? 1 : -1) * props.speed * dt;
    }

    frameTimer.value += dt;
    const frameDuration = 1 / currentFrameRate.value;
    if (frameTimer.value >= frameDuration) {
        frameTimer.value = 0;
        frameIndex.value++;
        if (isAttacking.value && frameIndex.value >= props.attackFrameCount) {
            isAttacking.value = false;
            frameIndex.value = 0;
        } else {
            frameIndex.value %= currentFrameCount.value;
        }
    }

    animFrameId.value = requestAnimationFrame(animFrame);
}

function onKeyDown(e: KeyboardEvent) {
    if (e.repeat) return;
    if (e.key === "ArrowRight") startMoving(true);
    if (e.key === "ArrowLeft") startMoving(false);
}

function onKeyUp(e: KeyboardEvent) {
    if (e.key === "ArrowRight" || e.key === "ArrowLeft") stopMoving();
}

function onVisibilityChange() {
    if (document.visibilityState === "visible" && animFrameId.value === null) {
        lastTimestamp.value = null;
        animFrame();
    }
}

onMounted(() => {
    loadImage(props.idleSpriteSrc, idleWidth);
    loadImage(props.runSpriteSrc, runWidth);
    loadImage(props.attackSpriteSrc, attackWidth);

    window.addEventListener("keydown", onKeyDown);
    window.addEventListener("keyup", onKeyUp);
    document.addEventListener("visibilitychange", onVisibilityChange);
    animFrame();
});

onBeforeUnmount(() => {
    window.removeEventListener("keydown", onKeyDown);
    window.removeEventListener("keyup", onKeyUp);
    document.removeEventListener("visibilitychange", onVisibilityChange);
    if (animFrameId.value !== null) cancelAnimationFrame(animFrameId.value);
});
</script>

<style scoped>
.stage {
    position: relative;
}

.player-container {
    position: absolute;
    bottom: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
}
</style>
