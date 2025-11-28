import apiService from "@/services/apiService.ts";

export default {
    async loadMessages(page: number, size: number) {
        return await apiService.get(`/messages/${page}/${size}`);
    },

    async sendMessage(message: string) {
        return await apiService.post(`/messages`, { message });
    },
};
