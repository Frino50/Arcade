import axios from "axios";
import ConnexionDto from "@/models/connexionDto.ts";

export default {
    async register(connexion: ConnexionDto) {
        return await axios.post(
            `http://localhost:8080/api/auth/register`,
            connexion
        );
    },

    async login(connexion: ConnexionDto) {
        return await axios.post(
            `http://localhost:8080/api/auth/login`,
            connexion
        );
    },
};
