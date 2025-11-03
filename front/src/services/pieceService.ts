import apiService from "@/services/apiService.ts";

export default {
    async getCoins() {
        return await apiService.get(`/coins`);
    },
};
