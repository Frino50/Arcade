<template>
    <ConnexionComponent
        title="Inscription"
        buttonText="S'inscrire"
        altText="Déjà inscrit ?"
        altButtonText="Se connecter"
        @submit="handleRegister"
        @alt-click="goToLogin"
    />
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import auth from "../services/authService.ts";
import ConnexionDto from "@/models/dtos/connexionDto.ts";
import ConnexionComponent from "@/components/ConnexionComponent.vue";

const router = useRouter();

async function handleRegister(connexionDto: ConnexionDto) {
    await auth.register(connexionDto);
    await router.push("/login");
}

const goToLogin = () => router.push("/login");
</script>
