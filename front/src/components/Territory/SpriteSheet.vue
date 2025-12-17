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
    background-color: #bdbdbd;
    background-image:
        linear-gradient(45deg, #999 25%, transparent 25%),
        linear-gradient(-45deg, #999 25%, transparent 25%),
        linear-gradient(45deg, transparent 75%, #999 75%),
        linear-gradient(-45deg, transparent 75%, #999 75%);

    background-size: 16px 16px;
    image-rendering: pixelated;
}
</style>
