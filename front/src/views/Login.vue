<template>
    <ConnexionComponent
        title="Connexion"
        buttonText="Se connecter"
        altText="Pas encore inscrit ?"
        altButtonText="Créer un compte"
        :external-error="loginError"
        @submit="handleLogin"
        @alt-click="goToRegister"
    />
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import auth from "../services/authService.ts";
import ConnexionDto from "@/models/dtos/connexionDto.ts";
import ConnexionComponent from "@/components/ConnexionComponent.vue";
import { useLocalStore } from "../store/local.ts";
import LoginResponseDto from "@/models/dtos/loginResponseDto.ts";

const router = useRouter();
const localstore = useLocalStore();
const loginError = ref("");

async function handleLogin(connexionDto: ConnexionDto) {
    try {
        const res = await auth.login(connexionDto);
        const loginResponseDto: LoginResponseDto = res.data;
        localstore.pseudo = loginResponseDto.pseudo;
        localstore.token = loginResponseDto.token;
        await router.push("/");
    } catch (e) {
        loginError.value = "Erreur serveur, veuillez réessayer";
    }
}

const goToRegister = () => router.push("/register");
</script>
