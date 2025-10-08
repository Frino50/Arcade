<template>
    <div
        class="theme-switcher"
        v-click-outside="closeDropdown"
        @click="toggleDropdown"
    >
        <div class="current-theme">{{ currentTheme }}</div>
        <ul v-if="dropdownOpen" class="options">
            <li
                v-for="theme in themes"
                :key="theme"
                @click="selectTheme(theme)"
                :class="{ selected: theme === currentTheme }"
            >
                {{ theme }}
            </li>
        </ul>
    </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

const themes = ["cyan", "yellow", "purple"] as const;
type Theme = (typeof themes)[number];

const currentTheme = ref<Theme>("cyan");
const dropdownOpen = ref(false);

function toggleDropdown() {
    dropdownOpen.value = !dropdownOpen.value;
}

function closeDropdown() {
    dropdownOpen.value = false;
}

function selectTheme(theme: Theme) {
    currentTheme.value = theme;
    closeDropdown();

    document.documentElement.classList.remove(
        ...themes.map((t) => `theme-${t}`)
    );
    document.documentElement.classList.add(`theme-${theme}`);
}
</script>
<style scoped>
.theme-switcher {
    position: relative;
    cursor: pointer;
    user-select: none;
    font-size: 0.85rem;
    width: 3rem;
    text-transform: capitalize;
    background: var(--futurist-blur-bg);
    color: var(--text-color);
    padding: 0.3rem 0.5rem;
    border-radius: 0.3rem;
    border: 1px solid var(--futurist-border);
    box-shadow: 0 0 12px var(--futurist-shadow);
    transition:
        transform 0.25s ease,
        box-shadow 0.25s ease;
}

.current-theme {
    font-weight: bold;
    color: var(--futurist-accent);
}

.options {
    position: absolute;
    top: 100%;
    left: 0;
    width: 100%;
    background: var(--futurist-panel-bg);
    border-radius: 0.3rem;
    margin-top: 0.2rem;
    padding: 0.2rem 0;
    list-style: none;
    z-index: 10;
    border: 1px solid var(--futurist-border);
    box-shadow: 0 0 12px var(--futurist-shadow);
    transition:
        transform 0.25s ease,
        box-shadow 0.25s ease;
}

.options li {
    padding: 0.3rem 0.5rem;
    cursor: pointer;
}

.options li.selected {
    font-weight: bold;
}

.options li:hover {
    background: rgba(255, 255, 255, 0.1);
}
</style>
