<template>
    <teleport to="body">
        <div v-if="visible" class="modal-overlay">
            <div class="modal-content">
                <button class="close-btn" @click="$emit('close')">X</button>
                <div class="sprite-list">
                    <Fighter
                        v-for="spriteInfo in sprites"
                        :key="spriteInfo.id"
                        :sprite-src="spriteInfo.idleImageUrl"
                        :width="spriteInfo.width"
                        :height="spriteInfo.height"
                        :frames="spriteInfo.frames"
                        :scale="Number(spriteInfo.scale)"
                    />
                </div>
            </div>
        </div>
    </teleport>
</template>

<script setup lang="ts">
import Fighter from "@/components/Fighter.vue";
import type SpriteInfo from "@/models/SpriteInfos.ts";

defineProps<{
    sprites: SpriteInfo[];
    visible: boolean;
}>();
defineEmits(["close"]);
</script>

<style scoped>
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.6);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 999;
}

.modal-content {
    background: #1e293b;
    padding: 2rem;
    border-radius: 12px;
    max-height: 80%;
    overflow-y: auto;
    position: relative;
}
.close-btn {
    position: absolute;
    top: 0.5rem;
    right: 0.5rem;
    background: transparent;
    border: none;
    font-size: 1.5rem;
    cursor: pointer;
    color: white;
    box-shadow: none;
}
.sprite-list {
    display: flex;
    flex-wrap: wrap;
    gap: 1rem;
}
</style>
