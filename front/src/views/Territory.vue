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
    <SpriteListPage />
</template>

<script setup lang="ts">
import { ref } from "vue";
import apiService from "@/services/apiService.ts";
import SpriteListPage from "@/views/SpriteListPage.vue";

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

        await apiService.post("/sprite", formData, {
            headers: { "Content-Type": "multipart/form-data" },
        });
    } catch (err) {
        serverResponse.value = "Erreur lors de l’envoi";
        console.error(err);
    }
}
</script>

<style scoped>
.actions {
    display: flex;
    align-items: center;
    gap: 1rem;
}
</style>
