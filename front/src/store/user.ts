import { defineStore } from "pinia";
import { ref } from "vue";

export const useLocalStore = defineStore("local", () => {
    const pseudo = ref<string>("");

    return {
        pseudo,
    };
});
