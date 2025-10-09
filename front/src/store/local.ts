import { defineStore } from "pinia";
import { ref } from "vue";

export const useLocalStore = defineStore(
    "local",
    () => {
        const pseudo = ref("");
        const token = ref("");
        const theme = ref("cyan");

        return {
            pseudo,
            token,
            theme,
        };
    },
    {
        persist: true,
    }
);
