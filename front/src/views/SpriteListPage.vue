<template>
    <div class="sprite-list-page">
        <h2>Unités</h2>
        <p v-if="sprites.length === 0" class="info-message">
            Aucun sprite trouvé en base de données.
        </p>

        <div v-else class="sprite-grid">
            <div v-for="sprite in sprites" :key="sprite.id" class="sprite-card">
                <h3>{{ sprite.name }}</h3>
                <Test
                    :idle-image-url="sprite.idleImageUrl"
                    :width="sprite.width"
                    :height="sprite.height"
                    :frames="sprite.frames"
                    :scale="4"
                />
                <p class="details">ID BDD: {{ sprite.id }}</p>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import apiService from "@/services/apiService.ts";
import Test from "@/views/test.vue";

interface SpriteSummary {
    id: number;
    name: string;
    idleImageUrl: string;
    width: number;
    height: number;
    frames: number;
}

const sprites = ref<SpriteSummary[]>([]);

async function fetchSprites() {
    const response = await apiService.get<SpriteSummary[]>("/sprite/summary");
    sprites.value = response.data;
}

onMounted(fetchSprites);
</script>

<style scoped>
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
    border: 1px solid #ddd;
    border-radius: 8px;
    padding: 1rem;
    text-align: center;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}

.details {
    font-size: 0.8rem;
    color: #666;
}
</style>
