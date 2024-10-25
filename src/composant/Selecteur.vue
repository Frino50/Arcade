<template>
    <div class="container">
        <div v-for="page in pages">
            <div
                @click="switchPage(page.link)"
                class="custom-style"
                :class="{ hovered: isHovered }"
            >
                <div class="content">
                    <img
                        class="image-container"
                        :src="
                            'src/assets/' +
                            page.link.toLocaleLowerCase() +
                            '.png'
                        "
                        :alt="page.link.toLocaleLowerCase() + '.png'"
                    />
                    <span class="text">{{ page.title }}</span>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";

const isHovered = ref(false);
const router = useRouter();
const pages = [
    { title: "Démineur", link: "Demineur" },
    { title: "Morpion", link: "Morpion" },
    { title: "2048", link: "2048" },
    { title: "Boids", link: "Boids" },
    { title: "Bactérie", link: "Bacterie" },
    { title: "Ball", link: "Ball" },
    { title: "Snake", link: "Snake" },
    { title: "Tetris", link: "Tetris" },
];

function switchPage(chemin: string) {
    router.push({ name: chemin });
}
</script>

<style scoped>
.container {
    display: flex;
    flex-direction: column;
    position: absolute;
    right: 0;
    top: 20%;
    align-items: flex-end;
}

.custom-style {
    background-color: rgb(30, 37, 48);
    width: 3rem;
    height: 3rem;
    margin-bottom: 1rem;
    border-top-left-radius: 1rem;
    border-bottom-left-radius: 1rem;
    transition: width 0.3s, left 0.3s;
    overflow: hidden;
    display: flex;
    justify-content: center;
    cursor: pointer;
    border: 0.2rem solid black;
    border-right: none;
}

.custom-style .content {
    display: flex;
    align-items: center;
    overflow: hidden;
    transition: transform 0.5s;
    margin-left: 0.5rem;
}

.custom-style:hover {
    width: 8rem;
    background-color: rgb(28, 45, 72);
}
.custom-style:active {
    background-color: rgb(40, 88, 118);
}

.image-container {
    height: 2rem;
    width: 2rem;
}

.text {
    white-space: nowrap;
    margin-left: 0.5rem;
}
</style>
