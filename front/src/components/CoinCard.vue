<template>
    <div class="coin-card panel">
        <h2>{{ props.coin.nom }}</h2>
        <p class="description">{{ props.coin.statut }}</p>
        <p class="description">{{ props.coin.description }}</p>
        <p class="price">{{ formatPrice(props.coin.prix) }} €</p>
        <p class="weight">Poids: {{ props.coin.poids || "N/A" }} g</p>
        <p v-if="props.coin.prixParGramme" class="price-per-gram">
            Prix par gramme : {{ props.coin.prixParGramme }} €
        </p>
        <a :href="props.coin.link" target="_blank" class="link">
            Voir la pièce
        </a>
    </div>
</template>

<script setup lang="ts">
import { defineProps } from "vue";
import type { Coin } from "../models/Coin";

const props = defineProps<{ coin: Coin }>();

const formatPrice = (price: number | string) => {
    if (!price) return "0";
    return new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 2 }).format(
        Number(price)
    );
};
</script>

<style scoped>
.coin-card {
    background: var(--futurist-panel-bg);
    border: 1px solid var(--futurist-border);
    border-radius: 12px;
    padding: 1rem;
    transition: 0.2s;
    box-shadow: 0 4px 10px var(--futurist-shadow);
    z-index: 1;
}

.coin-card:hover {
    box-shadow: 0 8px 16px var(--futurist-shadow-strong);
}

.coin-card h2 {
    font-size: 1.25rem;
    font-weight: 600;
    margin-bottom: 0.5rem;
    color: var(--futurist-accent);
}

.coin-card .description {
    color: var(--futurist-text-weak);
    margin-bottom: 0.5rem;
}

.coin-card .price {
    color: var(--piece-l-light);
    font-weight: bold;
    margin-bottom: 0.5rem;
}

.coin-card .weight {
    color: var(--futurist-text-weak);
    margin-bottom: 0.5rem;
}

.coin-card .price-per-gram {
    color: var(--piece-s-light);
    font-weight: 600;
    margin-bottom: 0.5rem;
}

.coin-card .link {
    color: var(--futurist-accent);
    text-decoration: none;
}

.coin-card .link:hover {
    text-decoration: underline;
}
</style>
