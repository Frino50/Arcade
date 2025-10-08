<template>
    <div class="page-home">
        <div class="col-lg-8">
            <div
                @click="link(hexagon.link)"
                class="hexagon-item"
                v-for="hexagon in hexagonItems"
                :key="hexagon.title"
            >
                <div class="hex-item">
                    <div></div>
                    <div></div>
                    <div></div>
                </div>
                <div class="hex-item">
                    <div></div>
                    <div></div>
                    <div></div>
                </div>
                <a class="hex-content">
                    <span class="hex-content-inner">
                        <span class="title">{{ hexagon.title }}</span>
                    </span>
                    <svg viewBox="0 0 173.20508075688772 200" height="200">
                        <path
                            d="M86.60254037844386 0L173.20508075688772 50L173.20508075688772 150L86.60254037844386 200L0 150L0 50Z"
                        ></path>
                    </svg>
                </a>
            </div>
        </div>
    </div>
</template>
<script setup lang="ts">
import { useRouter } from "vue-router";
const router = useRouter();
const hexagonItems = [
    { title: "Démineur", link: "Demineur" },
    { title: "Morpion", link: "Morpion" },
    { title: "2048", link: "2048" },
    { title: "Boids", link: "Boids" },
    { title: "Bactérie", link: "Bacterie" },
    { title: "Ball", link: "Ball" },
    { title: "Snake", link: "Snake" },
];

function link(link: string) {
    router.push({ name: link });
}
</script>
<style scoped>
.hexagon-item {
    cursor: pointer;
    width: 200px;
    height: 173px;
    float: left;
    margin-left: -29px;
    z-index: 0;
    position: relative;
    transform: rotate(30deg);
    transition: all 0.3s ease;
}

.hexagon-item:hover {
    z-index: 1;
}

.hexagon-item:hover .hex-item:first-child {
    opacity: 1;
    transform: scale(1.2);
}

.hexagon-item:hover .hex-item div::before,
.hexagon-item:hover .hex-item div::after {
    background-color: var(--futurist-accent-light);
}

.hex-item {
    position: absolute;
    top: 0;
    left: 50px;
    width: 100px;
    height: 173px;
}

.hex-item:first-child {
    z-index: 0;
    transform: scale(0.9);
    transition: all 0.3s cubic-bezier(0.165, 0.84, 0.44, 1);
}

.hex-item div {
    box-sizing: border-box;
    position: absolute;
    top: 0;
    width: 100px;
    height: 173px;
    transform-origin: center center;
}

.hex-item div::before,
.hex-item div::after {
    background-color: var(--futurist-bg-mid);
    content: "";
    position: absolute;
    width: 100%;
    height: 3px;
    transition: all 0.3s cubic-bezier(0.165, 0.84, 0.44, 1);
}

.hex-item div:after {
    bottom: 0;
}

.hex-item div:nth-child(2) {
    transform: rotate(60deg);
}
.hex-item div:nth-child(3) {
    transform: rotate(120deg);
}

.hex-content {
    color: var(--futurist-text-light);
    display: block;
    height: 180px;
    margin: 0 auto;
    position: relative;
    text-align: center;
    transform: rotate(-30deg);
    width: 156px;
}

.hex-content .hex-content-inner {
    left: 50%;
    margin: -3px 0 0 2px;
    position: absolute;
    top: 50%;
    transform: translate(-50%, -50%);
}

.hex-content .title {
    font-family: "Open Sans", sans-serif;
    font-size: 14px;
    letter-spacing: 1px;
    line-height: 24px;
    text-transform: uppercase;
}

.hex-content svg {
    left: -7px;
    position: absolute;
    top: -13px;
    transform: scale(0.87);
    z-index: -1;
    transition: all 0.3s cubic-bezier(0.165, 0.84, 0.44, 1);
}

.hex-content svg path {
    fill: var(--futurist-bg-dark);
}

.page-home {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
}

.page-home .hexagon-item:nth-last-child(1),
.page-home .hexagon-item:nth-last-child(2),
.page-home .hexagon-item:nth-last-child(3) {
    transform: rotate(30deg) translate(87px, -80px);
}

@media (min-width: 1200px) {
    .col-lg-8 {
        width: 40%;
    }
}

.hexagon-item:first-child {
    margin-left: 0;
}

.hexagon-item:hover .title {
    animation: focus-in-contract 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
}

@keyframes focus-in-contract {
    0% {
        letter-spacing: 1em;
        filter: blur(12px);
        opacity: 0;
    }
    100% {
        filter: blur(0px);
        opacity: 1;
    }
}
</style>
