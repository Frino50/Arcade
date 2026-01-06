import axios from "axios";
import { localStore, LocalState } from "@/store/local.ts";
import router from "@/router";
import { useToast } from "@/services/toast.ts";

const toast = useToast();

const apiService = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    headers: {
        "Content-Type": "application/json",
    },
});

let isTokenExpiredToastShown = false;

apiService.interceptors.request.use(
    (config) => {
        const token = localStore.token;

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
        if (!axios.isAxiosError(error)) {
            showError("Erreur inconnue");
            return Promise.reject(error);
        }

        if (error.code === "ERR_NETWORK") {
            showError("Serveur injoignable");
            return Promise.reject(error);
        }

        if (error.response) {
            const { status, data } = error.response;
            const message = data?.message || "Erreur inconnue";

            if (status === 401 && data?.error === "INVALID_OR_EXPIRED_TOKEN") {
                handleInvalidToken(localStore);
                return Promise.reject(error);
            }

            if (status === 500) {
                showError(
                    "Une erreur serveur est survenue. Veuillez réessayer plus tard."
                );
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

function handleInvalidToken(localstore: LocalState) {
    if (isTokenExpiredToastShown) return;

    isTokenExpiredToastShown = true;
    localstore.pseudo = "";
    localstore.token = "";

    router.push("/login").catch(() => {});
    showError("Session expirée, veuillez vous reconnecter.");

    setTimeout(() => {
        isTokenExpiredToastShown = false;
    }, 5000);
}

export default apiService;
