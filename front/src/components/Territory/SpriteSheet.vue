<template>
    <div
        class="checkerboard"
        :style="{
            width: `${props.width * props.scale}px`,
            height: `${props.height * props.scale}px`,
        }"
    >
        <img v-if="spriteUrl" :src="spriteUrl" alt="sprite" />
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import spriteService from "@/services/spriteService";

const props = defineProps({
    spriteSrc: { type: String, required: true },
    scale: { type: Number, default: 1 },
    width: { type: Number, default: 1 },
    height: { type: Number, default: 1 },
});

const spriteUrl = ref<string>("");

onMounted(async () => {
    spriteUrl.value = await spriteService.getImage(props.spriteSrc);
});
</script>

<style scoped>
.checkerboard {
    background-color: #0f172a;
    background-image: repeating-conic-gradient(
        #1e293b 0 25%,
        transparent 0 50%
    );
    background-size: 20px 20px;
    image-rendering: pixelated;
}
</style>
