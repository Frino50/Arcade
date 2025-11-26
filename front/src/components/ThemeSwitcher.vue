<template>
    <Dropdown
        v-model="currentTheme"
        :suggestions="themes"
        @change="handleThemeChange"
        class="theme-switcher"
    />
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useLocalStore } from "@/store/local";
import Dropdown from "@/components/Dropdown.vue";

const store = useLocalStore();
const themes: string[] = ["cyan", "yellow", "purple"];
const currentTheme = ref<string>(store.theme);

onMounted(() => {
    applyTheme(currentTheme.value);
});

function handleThemeChange() {
    const newTheme = currentTheme.value;

    if (newTheme) {
        store.theme = newTheme;

        applyTheme(newTheme);
    }
}

function applyTheme(theme: string) {
    document.documentElement.classList = "";
    document.documentElement.classList.add(`theme-${theme}`);
}
</script>

<style scoped>
.theme-switcher {
    position: fixed;
    top: 0.75rem;
    right: 0.75rem;
    z-index: 1;
}
</style>
