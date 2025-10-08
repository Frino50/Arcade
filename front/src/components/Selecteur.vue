<template>
    <div class="container">
        <div v-for="(page, index) in pages" :key="index">
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
    { title: "2048", link: "2048" },
    { title: "Snake", link: "Snake" },
    { title: "Tetris", link: "Tetris" },
    { title: "Boids", link: "Boids" },
    { title: "Bactérie", link: "Bacterie" },
    { title: "Ball", link: "Ball" },
];

function switchPage(chemin: string) {
    router.push({ name: chemin });
}
</script>

<style scoped>
.container {
    position: fixed;
    right: 0;
    height: 100vh;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: flex-end;
    z-index: 1000;
}

.custom-style {
    background: var(--futurist-blur-bg);
    width: 3rem;
    height: 3rem;
    margin-bottom: 1rem;
    border-top-left-radius: 1rem;
    border-bottom-left-radius: 1rem;
    border: 1px solid var(--futurist-shadow);
    border-right: none;
    box-shadow:
        0 0 10px var(--futurist-shadow),
        inset 0 0 15px rgba(var(--scrollbar-track));
    transition:
        width 0.35s ease,
        background 0.35s ease,
        box-shadow 0.3s ease;
    overflow: hidden;
    display: flex;
    justify-content: center;
    align-items: center;
    cursor: pointer;
    position: relative;
}

.custom-style .content {
    display: flex;
    align-items: center;
    overflow: hidden;
    margin-left: 0.5rem;
    transition: transform 0.5s ease;
}

.image-container {
    height: 2rem;
    width: 2rem;
    filter: drop-shadow(0 0 6px var(--futurist-shadow-strong));
    transition:
        transform 0.3s ease,
        filter 0.3s ease;
}

.custom-style:hover {
    width: 9rem;
    background: rgba(var(--futurist-accent), 0.1);
    box-shadow:
        0 0 25px var(--futurist-shadow-strong),
        inset 0 0 20px rgba(var(--futurist-emoji-hover-bg));
    border-color: var(--futurist-shadow-strong);
}

.custom-style:hover .image-container {
    transform: scale(1.2);
    filter: drop-shadow(0 0 10px var(--futurist-shadow-strong));
}

.custom-style:hover .text {
    opacity: 1;
    transform: translateX(0);
}

.text {
    color: var(--futurist-accent);
    font-weight: bold;
    text-transform: uppercase;
    font-size: 0.85rem;
    letter-spacing: 0.05em;
    margin-left: 0.7rem;
    white-space: nowrap;
    opacity: 0;
    transform: translateX(-10px);
    transition:
        opacity 0.4s ease,
        transform 0.4s ease;
    text-shadow:
        0 0 6px var(--futurist-shadow-strong),
        0 0 12px rgba(var(--button-shadow));
}

@keyframes pulse {
    0%,
    100% {
        box-shadow:
            0 0 10px var(--futurist-shadow),
            inset 0 0 10px rgba(0, 255, 255, 0.05);
    }
    50% {
        box-shadow:
            0 0 25px var(--futurist-shadow-strong),
            inset 0 0 20px rgba(0, 255, 255, 0.1);
    }
}

.custom-style {
    animation: pulse 4s ease-in-out infinite;
}

.custom-style:active {
    transform: scale(0.95);
    background: rgba(var(--futurist-accent), 0.2);
    box-shadow:
        0 0 30px var(--futurist-shadow-strong),
        inset 0 0 25px rgba(var(--scrollbar-track));
}
</style>
