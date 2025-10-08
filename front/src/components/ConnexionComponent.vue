<template>
    <div class="auth-page">
        <div class="auth-card" v-loading="isLoading">
            <h2>{{ props.title }}</h2>

            <div class="input-group">
                <input
                    v-model="pseudo"
                    placeholder="Pseudo"
                    @keyup.enter="handleSubmit"
                />
            </div>

            <div class="input-group">
                <input
                    v-model="password"
                    type="password"
                    placeholder="Mot de passe"
                    @keyup.enter="handleSubmit"
                />
            </div>

            <button @click="handleSubmit">{{ props.buttonText }}</button>

            <div class="alt-text">
                {{ props.altText }}
                <div @click="redirection()" class="alt-button">
                    {{ props.altButtonText }}
                </div>
            </div>

            <p v-if="internalError" class="error-text">{{ internalError }}</p>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import ConnexionDto from "@/models/dtos/connexionDto.ts";
import auth from "@/services/authService.ts";
import LoginResponseDto from "@/models/dtos/loginResponseDto.ts";
import router from "@/router.ts";
import { useLocalStore } from "@/store/local.ts";
const localstore = useLocalStore();

const props = defineProps({
    title: String,
    buttonText: String,
    altText: String,
    altButtonText: String,
    mode: String,
});

const pseudo = ref("");
const password = ref("");
const internalError = ref("");
const isLoading = ref(false);

async function handleSubmit() {
    if (!pseudo.value || !password.value) {
        internalError.value = "Veuillez remplir tous les champs";
        return;
    }

    internalError.value = "";
    isLoading.value = true;

    try {
        if (props.mode === "login") {
            await login();
        } else {
            await register();
        }
    } finally {
        isLoading.value = false;
    }
}

async function login() {
    const res = await auth.login(connexionDto());
    const loginResponseDto: LoginResponseDto = res.data;
    localstore.pseudo = loginResponseDto.pseudo;
    localstore.token = loginResponseDto.token;
    await router.push("/");
}

async function register() {
    await auth.register(connexionDto());
    await router.push("/login");
}

function connexionDto(): ConnexionDto {
    return new ConnexionDto(pseudo.value, password.value);
}

function redirection() {
    router.push(props.mode === "login" ? "/register" : "/login");
}
</script>

<style scoped>
.auth-page {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    background: var(--futurist-bg-dark);
}

.auth-card {
    background: var(--futurist-panel-bg);
    padding: 2rem 3rem;
    border-radius: 10px;
    box-shadow: 0 0 20px var(--futurist-shadow-strong);
    width: 100%;
    max-width: 400px;
    display: flex;
    flex-direction: column;
    align-items: center;
}

h2 {
    margin-bottom: 1.5rem;
    color: var(--futurist-text-light);
}

.input-group {
    margin-bottom: 1rem;
    width: 100%;
}

input {
    width: 92%;
    padding: 0.75rem 1rem;
    border-radius: 5px;
    border: 1px solid var(--futurist-border);
    outline: none;
    font-size: 1rem;
    transition:
        border-color 0.3s,
        box-shadow 0.3s;
    background-color: var(--futurist-input-bg);
    color: var(--futurist-text-light);
}

input:focus {
    border-color: var(--futurist-accent);
    box-shadow: 0 0 8px var(--futurist-accent);
}

.alt-button {
    display: inline-block;
    margin-left: 0.5rem;
    font-weight: bold;
    text-decoration: underline;
    cursor: pointer;
    color: var(--futurist-accent);
    font-size: 0.9rem;
    transition: color 0.2s;
}

.alt-button:hover {
    color: var(--futurist-accent-light);
    text-decoration-thickness: 2px;
    transform: translateY(-2px);
}

.alt-text {
    display: flex;
    align-items: center;
    margin-top: 1.5rem;
    font-size: 0.9rem;
    color: var(--futurist-text-weak);
}

.error-text {
    margin-top: 1rem;
    color: var(--futurist-danger);
    font-size: 0.9rem;
}
</style>
