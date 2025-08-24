import ConnexionDto from "@/models/connexionDto.ts";
import apiService from "@/services/apiService.ts";

export default {
    async register(connexion: ConnexionDto) {
        return await apiService.post(
            `${import.meta.env.VITE_API_BASE_URL}/auth/register`,
            connexion
        );
    },

    async login(connexion: ConnexionDto) {
        return await apiService.post(
            `${import.meta.env.VITE_API_BASE_URL}/auth/login`,
            connexion
        );
    },
};
