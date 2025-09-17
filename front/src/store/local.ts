import { defineStore } from "pinia";
import { ref } from "vue";

export const useLocalStore = defineStore(
    "local",
    () => {
        const pseudo = ref("");
        const token = ref("");

        return {
            pseudo,
            token,
        };
    },
    {
        persist: true,
    }
);
