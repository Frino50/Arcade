<template>
    <div class="actions">
        <button @click="openFilePicker">Importer un sprite</button>
        <input
            ref="fileInput"
            type="file"
            style="display: none"
            @change="onFileSelected"
        />
        <button :disabled="!selectedFile" @click="sendToBackend">
            Envoyer au backend
        </button>

        <p v-if="selectedFile">Fichier sélectionné : {{ selectedFile.name }}</p>
        <p v-if="serverResponse">Réponse backend : {{ serverResponse }}</p>
    </div>
    <div class="sprite-list-page">
        <h2>Unités</h2>
        <p v-if="sprites.length === 0" class="info-message">
            Aucun sprite trouvé en base de données.
        </p>

        <div v-else class="sprite-grid">
            <div v-for="sprite in sprites" :key="sprite.id" class="sprite-card">
                <h3>{{ sprite.name }}</h3>

                <Fighter
                    :sprite-src="sprite.idleImageUrl"
                    :width="sprite.width"
                    :height="sprite.height"
                    :frames="sprite.frames"
                    :scale="4"
                />
                <input v-model="sprite.newName" placeholder="Nouveau nom" />
                <button @click="renameSprite(sprite)">RENOMMER</button>

                <button @click="deleteSprite(sprite.name)">DELETE</button>
            </div>
        </div>
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
        sprites.value.push(newSprite.data);
    } catch (err) {
        serverResponse.value = "Erreur lors de l’envoi";
        console.error(err);
    }
}

async function deleteSprite(name: string) {
    await spriteService.deleteSprite(name);
    sprites.value = sprites.value.filter((sprite) => sprite.name !== name);
}

async function getAllSpritesInfos() {
    const response = await spriteService.getAllSpritesInfos();
    sprites.value = response.data;
}

async function renameSprite(sprite: SpriteInfo) {
    if (!sprite.newName) return;

    const response = await spriteService.renameSprite(
        sprite.id,
        sprite.newName
    );
    const updatedSprite: SpriteInfo = response.data;

    const index = sprites.value.findIndex((s) => s.id === sprite.id);
    if (index !== -1) {
        sprites.value[index] = updatedSprite;
    }
}

onMounted(getAllSpritesInfos);
</script>

<style scoped>
.actions {
    display: flex;
    align-items: center;
    gap: 1rem;
}

.sprite-list-page {
    padding: 2rem;
}

.sprite-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 2rem;
    margin-top: 2rem;
}

.sprite-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    border: 1px solid #ddd;
    gap: 1rem;
    border-radius: 8px;
    padding: 1rem;
    text-align: center;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}
</style>
