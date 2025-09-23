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

        if (axios.isAxiosError(error)) {
            if (error.code === "ERR_NETWORK") {
                toast.show("Serveur injoignable", "error");
            } else if (error.response) {
                const status = error.response.status;
                const message =
                    error.response.data?.message || "Erreur inconnue";

                if (status === 401) {
                    const err = error.response.data?.error;
                    if (err === "INVALID_OR_EXPIRED_TOKEN") {
                        localstore.pseudo = "";
                        localstore.token = "";
                        router.push("/login").catch(() => {});
                    }
                }
                toast.show(message, "error");
            } else {
                toast.show("Erreur inconnue", "error");
            }
        }

        return Promise.reject(error);
    }
);

export default apiService;
