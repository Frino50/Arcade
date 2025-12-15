<template>
    <div class="sprite-card">
        <!-- Header -->
        <div class="card-header">
            <span class="badge">ID: {{ sprite.id }}</span>
        </div>

        <!-- Visual stage -->
        <div class="visual-stage">
            <Fighter
                :sprite-src="sprite.idleImageUrl"
                :width="sprite.width"
                :height="sprite.height"
                :frames="sprite.frames"
                :scale="Number(sprite.scale)"
            />
        </div>

        <!-- Card body -->
        <div class="card-body">
            <div class="info-group">
                <div class="input-row">
                    <label>Scale:</label>
                    <input v-model.number="sprite.scale" class="dark-input" />
                </div>

                <div class="input-row">
                    <label>Nom:</label>
                    <input
                        type="text"
                        v-model="sprite.name"
                        class="dark-input"
                    />
                </div>

                <button class="btn-view" @click="searchAllSprites">
                    Voir tous les sprites
                </button>
            </div>
        </div>

        <!-- Card footer -->
        <div class="card-footer">
            <button
                class="btn-icon btn-save"
                @click="renameSprite()"
                :disabled="!sprite.scale"
            >
                💾
            </button>
            <button
                class="btn-icon btn-delete"
                @click="$emit('delete')"
                title="Supprimer l'unité"
            >
                🗑️
            </button>
        </div>

        <!-- Popup pour afficher tous les sprites -->
        <SpriteListModal
            :sprites="listSpriteInfo"
            :visible="showModal"
            @close="showModal = false"
        />
    </div>
</template>

<script setup lang="ts">
import Fighter from "@/components/Fighter.vue";
import type SpriteInfo from "@/models/SpriteInfos.ts";
import ModifSpriteDto from "@/models/dtos/modifSpriteDto.ts";
import spriteService from "@/services/spriteService.ts";
import { ref } from "vue";
import SpriteListModal from "@/components/Territory/SpriteListModal.vue";

defineEmits(["delete"]);

const sprite = defineModel<SpriteInfo>({ required: true });
const listSpriteInfo = ref<SpriteInfo[]>([]);
const showModal = ref(false);

/**
 * Récupère tous les sprites liés et ouvre la popup
 */
async function searchAllSprites() {
    listSpriteInfo.value = await spriteService.getAllSprites(sprite.value.id);
    showModal.value = true;
}

/**
 * Renomme le sprite et met à jour l'échelle
 */
async function renameSprite() {
    const modifSpriteDto: ModifSpriteDto = new ModifSpriteDto(
        sprite.value.id,
        sprite.value.name,
        sprite.value.scale
    );
    await spriteService.renameSprite(modifSpriteDto);
}
</script>

<style scoped>
.sprite-card {
    background: #1e293b;
    border: 1px solid #334155;
    border-radius: 16px;
    overflow: hidden;
    transition:
        transform 0.3s ease,
        box-shadow 0.3s ease;
    display: flex;
    flex-direction: column;
}

.sprite-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
    border-color: #3b82f6;
}

.card-header {
    padding: 0.75rem 1rem;
    display: flex;
    justify-content: flex-end;
}

.badge {
    font-size: 0.7rem;
    background: #0f172a;
    padding: 2px 8px;
    border-radius: 4px;
    color: #64748b;
    font-family: monospace;
}

.card-body {
    padding: 1.5rem 1rem 0.5rem 1rem;
    flex: 1;
}

.info-group {
    display: flex;
    flex-direction: column;
    gap: 0.8rem;
}

.input-row {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.input-row label {
    min-width: 50px;
    color: #94a3b8;
    font-size: 0.9rem;
    text-align: right;
}

.dark-input {
    background: #0f172a;
    border: 1px solid #334155;
    color: white;
    padding: 0.6rem;
    border-radius: 6px;
    font-size: 1rem;
    flex: 1;
    box-sizing: border-box;
    transition: border-color 0.2s;
}

.dark-input:focus {
    outline: none;
    border-color: #3b82f6;
}

.card-footer {
    padding: 1rem;
    display: flex;
    gap: 0.5rem;
    border-top: 1px solid #334155;
    margin-top: 1rem;
}

.btn-icon {
    flex: 1;
    padding: 0.6rem;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-size: 1.1rem;
    transition: all 0.2s;
}

.btn-save {
    background: #059669;
    color: white;
}

.btn-save:hover:not(:disabled) {
    background: #10b981;
}

.btn-save:disabled {
    background: #334155;
    opacity: 0.5;
    cursor: not-allowed;
}

.btn-delete {
    background: #be123c;
    color: white;
}

.btn-delete:hover {
    background: #e11d48;
}

.visual-stage {
    background: #0f172a;
    min-height: 150px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 1rem;
    border-radius: 8px;
    border: 1px dashed #334155;
    position: relative;
    overflow: hidden;
}

.visual-stage::after {
    content: "";
    position: absolute;
    bottom: 0;
    width: 80%;
    height: 10px;
    background: radial-gradient(
        ellipse at center,
        rgba(0, 0, 0, 0.5) 0%,
        transparent 70%
    );
}

.btn-view {
    background: #3b82f6;
    color: white;
    border: none;
    padding: 0.5rem 1rem;
    border-radius: 6px;
    cursor: pointer;
    transition: background 0.2s;
}

.btn-view:hover {
    background: #2563eb;
}
</style>
