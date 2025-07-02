<template>
    <div
        class="message"
        :style="{
            backgroundColor: props.isGreen
                ? 'rgba(8, 173, 63, 0.9)'
                : 'rgba(258, 0, 0, 0.9)',
        }"
    >
        {{ props.message }}
    </div>
</template>

<script setup lang="ts">
const props = defineProps({
    message: { type: String },
    isGreen: { type: Boolean },
});
</script>

<style scoped>
.message {
    z-index: 1;
    position: absolute;
    padding: 1rem;
    border-radius: 5px;
    color: white;
    font-size: 1.5rem;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
}
</style>
