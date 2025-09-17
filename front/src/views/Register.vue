<template>
    <ConnexionComponent
        title="Inscription"
        buttonText="S'inscrire"
        altText="Déjà inscrit ?"
        altButtonText="Se connecter"
        :external-error="registerError"
        @submit="handleRegister"
        @alt-click="goToLogin"
    />
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import auth from "../services/authService.ts";
import ConnexionDto from "@/models/dtos/connexionDto.ts";
import ConnexionComponent from "@/components/ConnexionComponent.vue";

const router = useRouter();
const registerError = ref("");

async function handleRegister(connexionDto: ConnexionDto) {
    try {
        await auth.register(connexionDto);
        await router.push("/login");
    } catch (e) {
        registerError.value = "Pseudo déjà utilisé ou problème serveur";
    }
}

const goToLogin = () => router.push("/login");
</script>
