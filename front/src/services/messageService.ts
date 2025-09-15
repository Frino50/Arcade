import apiService from "@/services/apiService.ts";

export default {
    async loadRecentMessages() {
        return await apiService.get("/messages");
    },

    async sendMessage(message: string) {
        return await apiService.post(`/messages/${message}`);
    },
};
