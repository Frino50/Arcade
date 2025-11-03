<template>
    <div class="container theme-cyan">
        <div class="sort-menu">
            <div class="sort-field">
                <select v-model="sortField">
                    <option value="prix">Prix</option>
                    <option value="prixParGramme">Prix par gramme</option>
                </select>
            </div>

            <div
                class="sort-order"
                @click="toggleSortOrder"
                :title="sortOrder === 'asc' ? 'Croissant' : 'Décroissant'"
            >
                <span v-if="sortOrder === 'asc'">⬆️</span>
                <span v-else>⬇️</span>
            </div>

            <div class="sort-status">
                <select v-model="sortStatus">
                    <option value="all">Tous</option>
                    <option value="Disponible">Disponible</option>
                    <option value="Épuisé">Épuisé</option>
                    <option value="Indisponible">Indisponible</option>
                </select>
            </div>
        </div>

        <div class="grid scrollable">
            <CoinCard
                v-for="coin in sortedCoins"
                :key="coin.link"
                :coin="coin"
            />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import CoinCard from "../components/CoinCard.vue";
import pieceService from "@/services/pieceService.ts";
import { Coin } from "@/models/Coin.ts";

const coins = ref<Coin[]>([]);

const sortField = ref<"prix" | "prixParGramme">("prix");
const sortOrder = ref<"asc" | "desc">("asc");

const sortStatus = ref<string>("all");

onMounted(async () => {
    try {
        const response = await pieceService.getCoins();
        coins.value = response.data;
    } catch (error) {
        console.error("Erreur lors de la récupération des pièces :", error);
    }
});

const toggleSortOrder = () => {
    sortOrder.value = sortOrder.value === "asc" ? "desc" : "asc";
};

const sortedCoins = computed(() => {
    let result = [...coins.value];

    if (sortStatus.value !== "all") {
        result = result.filter((c) => c.statut === sortStatus.value);
    }

    result.sort((a, b) => {
        const fieldA = a[sortField.value] ?? 0;
        const fieldB = b[sortField.value] ?? 0;
        return sortOrder.value === "asc" ? fieldA - fieldB : fieldB - fieldA;
    });

    return result;
});
</script>

<style scoped>
.container {
    max-width: 100rem;
    margin: 2rem auto;
    padding: 2rem;

    color: var(--text-color);
    display: flex;
    flex-direction: column;
    height: 49rem;
    border-radius: 0.75rem;
    position: relative;
    overflow: hidden;
}

.sort-status select {
    padding: 0.5rem 1rem;
    border-radius: 12px;
    font-size: 1rem;
    background: var(--futurist-input-bg);
    border: 1px solid var(--futurist-border);
    color: var(--text-color);
    cursor: pointer;
    transition: all 0.3s ease;
}
.sort-status select:hover {
    box-shadow:
        0 0 10px var(--futurist-accent),
        0 0 20px var(--futurist-accent-light);
}

.sort-menu {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-bottom: 1.5rem;
    justify-content: center;
}

.sort-field select {
    padding: 0.5rem 1rem;
    border-radius: 12px;
    font-size: 1rem;
    background: var(--futurist-input-bg);
    border: 1px solid var(--futurist-border);
    color: var(--text-color);
    cursor: pointer;
    transition: all 0.3s ease;
}
.sort-field select:hover {
    box-shadow:
        0 0 10px var(--futurist-accent),
        0 0 20px var(--futurist-accent-light);
}

.sort-order {
    cursor: pointer;
    font-size: 1.5rem;
    color: var(--futurist-accent);
    transition: all 0.3s ease;
}
.sort-order:hover {
    transform: scale(1.2);
    text-shadow:
        0 0 10px var(--futurist-accent-light),
        0 0 20px var(--futurist-accent);
}

/* Grid des cartes */
.grid.scrollable {
    flex: 1 1 auto;
    overflow-y: auto;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 1.5rem;
    padding-right: 0.5rem;
}
</style>
