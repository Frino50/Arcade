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
import auth from "../services/auth.ts";
import ConnexionDto from "@/models/connexionDto.ts";
import ConnexionComponent from "@/components/ConnexionComponent.vue";
import { useLocalStore } from "../store/user.ts";

const router = useRouter();
const localstore = useLocalStore();
const loginError = ref("");

async function handleLogin(connexionDto: ConnexionDto) {
    try {
        const res = await auth.login(connexionDto);
        if (res.data === true) {
            localstore.pseudo = connexionDto.pseudo;
            await router.push("/");
        } else {
            loginError.value = "Pseudo ou mot de passe incorrect";
        }
    } catch (e) {
        loginError.value = "Erreur serveur, veuillez réessayer";
    }
}

const goToRegister = () => router.push("/register");
</script>
