<template>
    <div class="container">
        <div
            v-for="rectangle in listRectangle"
            :key="rectangle.id"
            class="rectangle"
            :style="{
                width: rectangle.width,
                height: rectangle.height,
            }"
        ></div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import Rectangle from "../model/rectangle.ts";

const listRectangle = ref<Rectangle[]>([]);

function calculateDimensions(taille: number) {
    const sqrtTaille = Math.sqrt(taille);
    const colonnes = Math.ceil(sqrtTaille);
    const lignes = Math.ceil(taille / colonnes);

    const total = colonnes * lignes;
    const width = 100 / colonnes + "%";
    const height = 100 / lignes + "%";

    return { width, height, total };
}

onMounted(() => {
    const taille = 100;
    const { width, height, total } = calculateDimensions(taille);

    for (let i = 0; i < total; i++) {
        listRectangle.value.push(new Rectangle(i, width, height));
    }
});
</script>

<style scoped>
.container {
    display: flex;
    flex-wrap: wrap;
    background-color: #242424;
    height: 97.8vh;
}

.rectangle {
    border: 1px solid red;
    box-sizing: border-box;
}

.rectangle:hover {
    border: 3px solid black;
    box-sizing: border-box;
    cursor: pointer;
    background-color: beige;
}
</style>
