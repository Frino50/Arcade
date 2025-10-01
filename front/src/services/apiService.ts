import axios from "axios";
import { useLocalStore } from "@/store/local.ts";
import router from "@/router";
import { useToast } from "@/services/toast.ts";

const toast = useToast();

const apiService = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    headers: {
        "Content-Type": "application/json",
    },
});

apiService.interceptors.request.use(
    (config) => {
        const localstore = useLocalStore();
        const token = localstore.token;

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

apiService.interceptors.response.use(
    (response) => response,
    (error) => {
        const localstore = useLocalStore();

        if (!axios.isAxiosError(error)) {
            showError("Erreur inconnue");
            return Promise.reject(error);
        }

        // Back non lancé
        if (error.code === "ERR_NETWORK") {
            showError("Serveur injoignable");
            return Promise.reject(error);
        }

        // Gestion des réponses du serveur
        if (error.response) {
            const { data } = error.response;
            const message = data?.message || "Erreur inconnue";

            if (data?.error === "INVALID_OR_EXPIRED_TOKEN") {
                handleInvalidToken(localstore);
                return Promise.reject(error);
            }

            showError(message);
        } else {
            showError("Erreur inconnue");
        }

        return Promise.reject(error);
    }
);

function showError(message: string) {
    toast.show(message, "error");
}

function handleInvalidToken(localstore: ReturnType<typeof useLocalStore>) {
    localstore.pseudo = "";
    localstore.token = "";
    router.push("/login").catch(() => {});
    showError("Session expirée, veuillez vous reconnecter.");
}

export default apiService;
