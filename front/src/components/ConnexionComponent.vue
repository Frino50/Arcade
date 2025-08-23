<template>
    <div class="auth-page">
        <div class="auth-card">
            <h2>{{ props.title }}</h2>

            <div class="input-group">
                <input v-model="pseudo" placeholder="Pseudo" />
            </div>

            <div class="input-group">
                <input
                    v-model="password"
                    type="password"
                    placeholder="Mot de passe"
                />
            </div>

            <button @click="handleSubmit">{{ props.buttonText }}</button>

            <p class="error" v-if="errorToShow">{{ errorToShow }}</p>

            <p class="alt-text">
                {{ props.altText }}
                <button @click="$emit('alt-click')" class="link-button">
                    {{ props.altButtonText }}
                </button>
            </p>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import ConnexionDto from "@/models/connexionDto.ts";

const props = defineProps({
    title: String,
    buttonText: String,
    altText: String,
    altButtonText: String,
    externalError: String,
});

const emit = defineEmits<{
    (e: "submit", connexionDto: ConnexionDto): void;
    (e: "alt-click"): void;
}>();

const pseudo = ref("");
const password = ref("");
const internalError = ref("");

const errorToShow = computed(
    () => internalError.value || props.externalError || ""
);

function handleSubmit() {
    if (!pseudo.value || !password.value) {
        internalError.value = "Veuillez remplir tous les champs";
        return;
    }
    internalError.value = "";
    emit("submit", new ConnexionDto(pseudo.value, password.value));
}
</script>

<style scoped>
.auth-page {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    background: var(--marron-back);
    font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
}

.auth-card {
    background: var(--marron-clair);
    padding: 2rem 3rem;
    border-radius: 10px;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
    width: 100%;
    max-width: 400px;
    display: flex;
    flex-direction: column;
    align-items: center;
}

h2 {
    margin-bottom: 1.5rem;
    color: var(--marron-fonce);
}

.input-group {
    margin-bottom: 1rem;
    width: 100%;
}

input {
    width: 92%;
    padding: 0.75rem 1rem;
    border-radius: 5px;
    border: 1px solid var(--marron);
    outline: none;
    font-size: 1rem;
    transition: border-color 0.3s;
    background-color: white;
    color: var(--marron-fonce);
}

input:focus {
    border-color: var(--marron-fonce);
}

.error {
    color: #d71011;
    margin-top: 0.75rem;
    text-align: center;
    font-weight: bold;
}

.alt-text {
    margin-top: 1.5rem;
    font-size: 0.9rem;
    color: var(--marron-fonce);
}

.link-button {
    background: none;
    border: none;
    color: var(--marron-fonce);
    cursor: pointer;
    text-decoration: underline;
    padding: 0;
    font-size: 0.9rem;
}
</style>
