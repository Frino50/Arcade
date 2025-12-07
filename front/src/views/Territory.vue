<template>
    <div class="page-container">
        <header class="library-header">
            <div class="header-content">
                <h1>Bestiaire <span class="accent">v1.0</span></h1>

                <div class="actions-bar">
                    <button class="btn btn-secondary" @click="openFilePicker">
                        <span class="icon">📂</span> Importer un sprite
                    </button>

                    <input
                        ref="fileInput"
                        type="file"
                        accept=".zip,application/zip"
                        style="display: none"
                        @change="onFileSelected"
                    />

                    <div v-if="selectedFile" class="file-status">
                        <span class="file-name">{{ selectedFile.name }}</span>
                        <button class="btn btn-primary" @click="sendToBackend">
                            Envoyer
                        </button>
                    </div>

                    <p v-if="serverResponse" class="server-message">
                        {{ serverResponse }}
                    </p>
                </div>
            </div>
        </header>

        <main class="sprite-library">
            <div v-if="sprites.length === 0" class="empty-state">
                <div class="empty-icon">🐉</div>
                <p>Aucun monstre détecté dans la base de données.</p>
                <small
                    >Importez un fichier pour commencer votre collection.</small
                >
            </div>

            <div v-else class="sprite-grid">
                <div
                    v-for="sprite in sprites"
                    :key="sprite.id"
                    class="sprite-card"
                >
                    <div class="card-header">
                        <span class="badge">ID: {{ sprite.id }}</span>
                    </div>

                    <div class="visual-stage">
                        <Fighter
                            :sprite-src="sprite.idleImageUrl"
                            :width="sprite.width"
                            :height="sprite.height"
                            :frames="sprite.frames"
                            :scale="3.5"
                        />
                    </div>

                    <div class="card-body">
                        <div class="info-group">
                            <label>Nom de l'unité</label>
                            <h3 v-if="!sprite.newName">{{ sprite.name }}</h3>
                            <input
                                type="text"
                                v-model="sprite.newName"
                                :placeholder="sprite.name"
                                class="dark-input"
                            />
                        </div>
                    </div>

                    <div class="card-footer">
                        <button
                            class="btn-icon btn-save"
                            @click="renameSprite(sprite)"
                            title="Sauvegarder le nom"
                            :disabled="
                                !sprite.newName ||
                                sprite.newName === sprite.name
                            "
                        >
                            💾
                        </button>
                        <button
                            class="btn-icon btn-delete"
                            @click="deleteSprite(sprite.name)"
                            title="Supprimer l'unité"
                        >
                            🗑️
                        </button>
                    </div>
                </div>
            </div>
        </main>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import Fighter from "@/components/Fighter.vue";
import SpriteInfo from "@/models/SpriteInfos.ts";
import spriteService from "@/services/spriteService.ts";

const sprites = ref<SpriteInfo[]>([]);
const fileInput = ref<HTMLInputElement | null>(null);
const selectedFile = ref<File | null>(null);
const serverResponse = ref<string | null>(null);

function openFilePicker() {
    fileInput.value?.click();
}

function onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
        selectedFile.value = input.files[0];
        serverResponse.value = null;
    }
}

async function sendToBackend() {
    if (!selectedFile.value) return;

    try {
        const formData = new FormData();
        formData.append("file", selectedFile.value);

        const newSprite = await spriteService.uploadSprite(formData);
        // On s'assure d'initialiser newName pour l'édition
        const addedSprite = { ...newSprite.data, newName: newSprite.data.name };
        sprites.value.push(addedSprite);

        // Reset après succès
        selectedFile.value = null;
        serverResponse.value = "Upload réussi !";
        setTimeout(() => (serverResponse.value = null), 3000);
    } catch (err) {
        serverResponse.value = "Erreur lors de l’envoi";
        console.error(err);
    }
}

async function deleteSprite(name: string) {
    if (!confirm("Êtes-vous sûr de vouloir supprimer ce sprite ?")) return;

    try {
        await spriteService.deleteSprite(name);
        sprites.value = sprites.value.filter((sprite) => sprite.name !== name);
    } catch (err) {
        console.error(err);
    }
}

async function getAllSpritesInfos() {
    try {
        const response = await spriteService.getAllSpritesInfos();
        // On map pour pré-remplir le champ d'édition avec le nom actuel
        sprites.value = response.data.map((s) => ({ ...s, newName: s.name }));
    } catch (err) {
        console.error(err);
    }
}

async function renameSprite(sprite: SpriteInfo) {
    if (!sprite.newName) return;

    try {
        const response = await spriteService.renameSprite(
            sprite.id,
            sprite.newName
        );
        const updatedSprite: SpriteInfo = response.data;
        // Maintient la logique locale
        updatedSprite.newName = updatedSprite.name;

        const index = sprites.value.findIndex((s) => s.id === sprite.id);
        if (index !== -1) {
            sprites.value[index] = updatedSprite;
        }
    } catch (err) {
        console.error("Erreur renommage", err);
    }
}

onMounted(getAllSpritesInfos);
</script>

<style scoped>
/* --- Layout Global --- */
.page-container {
    min-height: 100vh;
    background-color: #0f172a; /* Bleu très sombre */
    background-image: radial-gradient(
        circle at 10% 20%,
        rgba(37, 99, 235, 0.1) 0%,
        transparent 20%
    );
    color: #e2e8f0;
    font-family: "Inter", sans-serif;
}

/* --- Header --- */
.library-header {
    background: rgba(30, 41, 59, 0.8);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid #334155;
    padding: 1rem 2rem;
    position: sticky;
    top: 0;
    z-index: 10;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.header-content {
    max-width: 1400px;
    margin: 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 1rem;
}

h1 {
    margin: 0;
    font-size: 1.5rem;
    font-weight: 700;
    letter-spacing: -0.5px;
    color: white;
}

.accent {
    color: #3b82f6;
    font-size: 0.9rem;
    background: rgba(59, 130, 246, 0.1);
    padding: 2px 8px;
    border-radius: 12px;
}

.actions-bar {
    display: flex;
    align-items: center;
    gap: 1rem;
}

.file-status {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    background: #1e293b;
    padding: 0.3rem 0.8rem;
    border-radius: 8px;
    border: 1px solid #475569;
}

.file-name {
    font-size: 0.85rem;
    color: #94a3b8;
    max-width: 150px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
}

.server-message {
    font-size: 0.8rem;
    color: #10b981;
    margin: 0;
}

/* --- Main Content --- */
.sprite-library {
    padding: 2rem;
    max-width: 1400px;
    margin: 0 auto;
}

.sprite-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 2rem;
}

/* --- Empty State --- */
.empty-state {
    text-align: center;
    padding: 4rem;
    color: #64748b;
}
.empty-icon {
    font-size: 4rem;
    margin-bottom: 1rem;
    opacity: 0.5;
}

/* --- Card Design --- */
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

.visual-stage {
    background: #0f172a; /* Fond noir derrière le sprite pour contraste */
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

/* Petit effet de sol pour le sprite */
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

.card-body {
    padding: 1.5rem 1rem 0.5rem 1rem;
    flex: 1;
}

.info-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.info-group label {
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #64748b;
    font-weight: 700;
}

.dark-input {
    background: #0f172a;
    border: 1px solid #334155;
    color: white;
    padding: 0.6rem;
    border-radius: 6px;
    font-size: 1rem;
    width: 100%;
    box-sizing: border-box;
    transition: border-color 0.2s;
    text-align: center;
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

/* --- Buttons --- */
.btn {
    padding: 0.6rem 1.2rem;
    border-radius: 8px;
    border: none;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    font-size: 0.9rem;
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
}

.btn-primary {
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    color: white;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}
.btn-primary:hover {
    filter: brightness(1.1);
}

.btn-secondary {
    background: #334155;
    color: #f1f5f9;
}
.btn-secondary:hover {
    background: #475569;
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
</style>
