<template>
    <header class="library-header">
        <div class="header-content">
            <h1>Bestiaire <span class="accent">v1.0</span></h1>
            <div class="actions-bar">
                <button class="btn btn-secondary" @click="openFilePicker">
                    <span class="icon">📂</span> Importer un sprite
                </button>

                <input
                    id="fileInput"
                    type="file"
                    accept=".zip,application/zip"
                    @change="onFileSelected($event)"
                    style="display: none"
                />
            </div>
        </div>
    </header>
</template>

<script setup lang="ts">
import spriteService from "@/services/spriteService.ts";

const emit = defineEmits(["add-to-list"]);

function openFilePicker() {
    document.getElementById("fileInput")?.click();
}

async function onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
        const formData = new FormData();
        formData.append("file", input.files[0]);

        const newSprite = await spriteService.uploadSprite(formData);
        emit("add-to-list", newSprite.data);
    }
}
</script>
<style scoped>
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

.btn-secondary {
    background: #334155;
    color: #f1f5f9;
}

.btn-secondary:hover {
    background: #475569;
}
</style>
