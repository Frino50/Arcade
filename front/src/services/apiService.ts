import axios from "axios";
import { useLocalStore } from "@/store/user.ts";
import router from "@/router";

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

        if (error.response && error.response.status === 401) {
            const message = error.response.data?.error;

            if (message === "INVALID_OR_EXPIRED_TOKEN") {
                localstore.pseudo = "";
                localstore.token = "";

                router.push("/login").catch(() => {});
            }
        }

        return Promise.reject(error);
    }
);

export default apiService;
