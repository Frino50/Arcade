<template>
    <div class="toast-container" :class="{ 'has-messages': messages.length }">
        <TransitionGroup name="toast-list" tag="div">
            <div
                v-for="(msg, index) in messages"
                :key="index"
                class="toast"
                :class="msg.type"
            >
                <div class="toast-content">
                    <span class="toast-icon">
                        <svg
                            v-if="msg.type === 'success'"
                            xmlns="http://www.w3.org/2000/svg"
                            viewBox="0 0 24 24"
                        >
                            <path
                                fill="currentColor"
                                d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"
                            />
                        </svg>
                        <svg
                            v-if="msg.type === 'error'"
                            xmlns="http://www.w3.org/2000/svg"
                            viewBox="0 0 24 24"
                        >
                            <path
                                fill="currentColor"
                                d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"
                            />
                        </svg>
                        <svg
                            v-if="msg.type === 'info'"
                            xmlns="http://www.w3.org/2000/svg"
                            viewBox="0 0 24 24"
                        >
                            <path
                                fill="currentColor"
                                d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"
                            />
                        </svg>
                    </span>
                    <p class="toast-text">{{ msg.text }}</p>
                </div>
                <button class="close-btn" @click="removeToast(index)">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                        <path
                            fill="currentColor"
                            d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"
                        />
                    </svg>
                </button>
            </div>
        </TransitionGroup>
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
    gap: 0.75rem;
    z-index: 9999;
    max-width: 90%;
}

.toast {
    padding: 1rem 1.25rem;
    border-radius: 8px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    min-width: 250px;
    max-width: 400px;
    transition: all 0.5s ease;
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

.toast-content {
    display: flex;
    align-items: center;
    gap: 0.75rem;
}

.toast-icon svg {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.toast-text {
    margin: 0;
    font-size: 1rem;
    font-weight: 500;
    line-height: 1.4;
    white-space: nowrap;
}

.close-btn {
    background: none;
    border: none;
    color: #fff;
    cursor: pointer;
    padding: 0.25rem;
    margin-left: 1rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    transition: background-color 0.2s ease;
}

.close-btn:hover {
    background-color: rgba(255, 255, 255, 0.2);
}

.close-btn svg {
    width: 18px;
    height: 18px;
}

.toast-list-enter-active,
.toast-list-leave-active {
    transition: all 0.4s cubic-bezier(0.55, 0, 0.1, 1);
}

.toast-list-enter-from,
.toast-list-leave-to {
    opacity: 0;
    transform: translateX(100%);
}

.toast-list-leave-active {
    position: absolute;
}
</style>
