import ConnexionDto from "@/models/dtos/connexionDto.ts";
import apiService from "@/services/apiService.ts";

export default {
    async register(connexion: ConnexionDto) {
        return await apiService.post("/auth/register", connexion);
    },

    async login(connexion: ConnexionDto) {
        return await apiService.post("/auth/login", connexion);
    },
};
