<template>
    <teleport to="body">
        <transition name="fade">
            <div
                v-if="visible"
                class="modal-overlay"
                @click.self="$emit('close')"
            >
                <div class="modal-content">
                    <header class="modal-header">
                        <h2>Inspecteur de Sprites</h2>
                        <button
                            class="close-btn"
                            @click="$emit('close')"
                            aria-label="Fermer"
                        >
                            <span>x</span>
                        </button>
                    </header>

                    <div class="sprites-grid">
                        <div
                            class="sprite-card"
                            v-for="spriteInfo in listSprites"
                            :key="spriteInfo.animationId"
                        >
                            <div class="card-header">
                                <span class="badge"
                                    >ID: {{ spriteInfo.animationId }}</span
                                >
                            </div>

                            <div class="card-body">
                                <div class="preview-box">
                                    <span class="label">Rendu Animation</span>
                                    <div class="animation-container">
                                        <Animation
                                            :key="spriteInfo.width"
                                            :sprite-src="spriteInfo.imageUrl"
                                            :width="spriteInfo.width"
                                            :height="spriteInfo.height"
                                            :frames="spriteInfo.frames"
                                            :scale="Number(spriteInfo.scale)"
                                        />
                                    </div>
                                </div>

                                <div class="preview-box">
                                    <span class="label">Planche Source</span>
                                    <div class="sheet-container">
                                        <SpriteSheet
                                            :key="spriteInfo.width"
                                            :sprite-src="spriteInfo.imageUrl"
                                            :width="spriteInfo.width"
                                            :height="spriteInfo.height"
                                        />
                                    </div>
                                </div>
                            </div>

                            <div class="card-footer">
                                <button
                                    class="action-btn"
                                    @click="
                                        reBuildImage(spriteInfo.animationId)
                                    "
                                >
                                    <span>Améliorer le sprite</span>
                                </button>
                            </div>
                        </div>
                    </div>

                    <div v-if="!listSprites?.length" class="empty-state">
                        Aucun sprite à afficher.
                    </div>
                </div>
            </div>
        </transition>
    </teleport>
</template>

<script setup lang="ts">
import Animation from "@/components/Territory/Animation.vue";
import type SpriteInfo from "@/models/SpriteInfos.ts";
import spriteService from "@/services/spriteService.ts";
import SpriteSheet from "@/components/Territory/SpriteSheet.vue";

defineProps<{
    visible: boolean;
}>();

defineEmits(["close"]);

const listSprites = defineModel<SpriteInfo[]>();

async function reBuildImage(animationId: number) {
    if (!listSprites.value) return;

    const updatedSprite: SpriteInfo =
        await spriteService.reBuildImage(animationId);

    const index = listSprites.value.findIndex(
        (s) => s.animationId === animationId
    );

    if (index !== -1) {
        listSprites.value[index] = updatedSprite;
    }
}
</script>

<style scoped>
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.75);
    backdrop-filter: blur(4px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 999;
}

.modal-content {
    background: #0f172a;
    width: 90%;
    max-width: 1000px;
    height: 85%;
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
    border: 1px solid #334155;
    overflow: hidden;
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem 2rem;
    background: #1e293b;
    border-bottom: 1px solid #334155;
}

.modal-header h2 {
    margin: 0;
    color: #e2e8f0;
    font-size: 1.5rem;
}

.close-btn {
    background: transparent;
    border: none;
    color: #94a3b8;
    font-size: 2rem;
    line-height: 1;
    cursor: pointer;
    transition: color 0.2s;
    padding: 0;
    box-shadow: none;
}
.close-btn:hover {
    color: #fff;
}

.sprites-grid {
    padding: 2rem;
    overflow-y: auto;
    display: grid;
    gap: 1.5rem;
}

.sprite-card {
    background: #1e293b;
    border-radius: 12px;
    border: 1px solid #334155;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    height: 18.8rem;
    transition: transform 0.2s;
}

.sprite-card:hover {
    border-color: #475569;
}

.card-header {
    padding: 0.75rem 1rem;
    background: #253146;
    border-bottom: 1px solid #334155;
}

.badge {
    background: #334155;
    color: #94a3b8;
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
    font-size: 0.75rem;
    font-weight: bold;
    font-family: monospace;
}

.card-body {
    padding: 1rem 1rem 0 1rem;
    display: flex;
    gap: 1rem;
    flex-grow: 1;
    align-items: flex-start;
    justify-content: space-between;
}

.card-body .preview-box:first-child {
    flex: 0 0 20%;
}

.card-body .preview-box:last-child {
    flex: 0 0 80%;
}

.label {
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #64748b;
    font-weight: 600;
}

.preview-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.5rem;
    flex: 1;
    height: 100%;
    overflow: hidden;
}

.animation-container {
    display: flex;
    align-items: end;
    justify-content: center;
    height: 100%;
    width: 100%;
    padding-bottom: 1rem;
}

.sheet-container {
    overflow-x: auto;
    overflow-y: hidden;
    display: flex;
    align-items: end;
    height: 100%;
    width: 100%;
    padding-bottom: 1rem;
}

.card-footer {
    padding: 1rem;
    background: #182336;
    border-top: 1px solid #334155;
    display: flex;
    justify-content: center;
}

.action-btn {
    background: #3b82f6;
    color: white;
    border: none;
    padding: 0.5rem 1rem;
    border-radius: 6px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    width: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
}

.action-btn:hover:not(:disabled) {
    background: #2563eb;
    transform: translateY(-1px);
}

.action-btn:disabled {
    background: #475569;
    cursor: not-allowed;
    opacity: 0.7;
}

.empty-state {
    text-align: center;
    color: #94a3b8;
    margin-top: 4rem;
}

.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s;
}
.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}

@keyframes rotation {
    0% {
        transform: rotate(0deg);
    }
    100% {
        transform: rotate(360deg);
    }
}
</style>
