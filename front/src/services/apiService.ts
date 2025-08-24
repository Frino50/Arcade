import axios from "axios";
import { useLocalStore } from "@/store/user.ts";

const apiService = axios.create({
    baseURL: "http://localhost:8080/api",
    headers: {
        "Content-Type": "application/json",
    },
});

// Ajoute l'intercepteur de requête
apiService.interceptors.request.use(
    (config) => {
        const localstore = useLocalStore();
        const token = localstore.token;

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default apiService;
