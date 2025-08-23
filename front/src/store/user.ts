import { defineStore } from "pinia";
import { ref } from "vue";

export const useLocalStore = defineStore(
    "local",
    () => {
        const pseudo = ref("");

        return {
            pseudo,
        };
    },
    {
        persist: true,
    }
);
