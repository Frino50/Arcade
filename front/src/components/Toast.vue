<template>
    <div class="toast-container" v-if="messages.length">
        <div
            v-for="(msg, index) in messages"
            :key="index"
            class="toast"
            :class="msg.type"
        >
            {{ msg.text }}
            <button class="close-btn" @click="removeToast(index)">×</button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { useToast } from "@/services/toast.ts";

const { messages, removeToast } = useToast();
</script>

<style scoped>
.toast-container {
    position: fixed;
    top: 1rem;
    right: 1rem;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    z-index: 9999;
}

.toast {
    padding: 1rem;
    border-radius: 5px;
    color: white;
    min-width: 200px;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.toast.success {
    background-color: #4caf50;
}
.toast.error {
    background-color: #f44336;
}
.toast.info {
    background-color: #2196f3;
}

.close-btn {
    background: none;
    border: none;
    color: white;
    font-size: 1rem;
    cursor: pointer;
}
</style>
