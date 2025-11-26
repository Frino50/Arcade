<template>
    <div class="main-container" @click="toggleDropdown">
        <div class="dropdown-header">
            <span
                :class="['current-option', { 'is-placeholder': !modelValue }]"
            >
                {{ displayValue }}
            </span>
        </div>

        <Transition name="dropdown">
            <ul v-if="isOpen" class="options">
                <li
                    v-for="(suggestion, index) in suggestions"
                    :key="index"
                    :class="{ selected: suggestion === modelValue }"
                    @click.stop="selectItem(suggestion)"
                >
                    {{ suggestion }}
                </li>
            </ul>
        </Transition>
    </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";

const modelValue = defineModel<any | null>({ default: null });

interface DropdownProps<T> {
    suggestions: T[];
    placeholder?: string;
}

const props = defineProps<DropdownProps<any>>();

const emit = defineEmits<{
    (e: "change"): void;
}>();

const isOpen = ref(false);

const displayValue = computed(() => {
    return modelValue.value
        ? String(modelValue.value)
        : props.placeholder || "Sélectionner";
});

function selectItem(suggestion: any) {
    modelValue.value = suggestion;

    emit("change");

    isOpen.value = false;
}

function toggleDropdown() {
    isOpen.value = !isOpen.value;
}
</script>

<style scoped>
.main-container {
    position: relative;
    cursor: pointer;
    user-select: none;
    font-size: 0.85rem;
    width: 3rem;
    text-transform: capitalize;
    background: var(--futurist-blur-bg, rgba(30, 30, 50, 0.7));
    color: var(--text-color, #a8dadc);
    padding: 0.3rem 0.5rem;
    border-radius: 0.3rem;
    border: 1px solid var(--futurist-border);
    box-shadow: 0 0 12px var(--futurist-shadow, rgba(0, 255, 255, 0.5));
    transition:
        transform 0.25s ease,
        box-shadow 0.25s ease;
}

.current-option {
    font-weight: bold;
    color: var(--futurist-accent, #ff79c6);
}

.current-option.is-placeholder {
    font-weight: normal;
    color: var(--text-color, #a8dadc);
}

.options {
    position: absolute;
    top: 100%;
    left: 0;
    width: 100%;
    background: var(--futurist-panel-bg, rgba(20, 20, 35, 0.95));
    border-radius: 0.3rem;
    margin-top: 0.2rem;
    padding: 0 0;
    list-style: none;
    border: 1px solid var(--futurist-border);
    box-shadow: 0 0 12px var(--futurist-shadow, rgba(0, 255, 255, 0.5));
    overflow: hidden;
}

.options li {
    padding: 0.3rem 0.5rem;
    cursor: pointer;
}

.options li.selected {
    font-weight: bold;
    color: var(--futurist-accent, #ff79c6);
    background: rgba(0, 255, 255, 0.1);
}

.options li:hover {
    background: rgba(255, 255, 255, 0.1);
}

.dropdown-enter-active,
.dropdown-leave-active {
    transition:
        max-height 0.35s ease,
        opacity 0.35s ease,
        transform 0.35s ease;
    overflow: hidden;
}

.dropdown-enter-from {
    opacity: 0;
    max-height: 0;
    transform: translateY(-10px);
}

.dropdown-enter-to {
    opacity: 1;
    max-height: 200px;
    transform: translateY(0);
}

.dropdown-leave-from {
    opacity: 1;
    max-height: 200px;
    transform: translateY(0);
}

.dropdown-leave-to {
    opacity: 0;
    max-height: 0;
    transform: translateY(-10px);
}
</style>
