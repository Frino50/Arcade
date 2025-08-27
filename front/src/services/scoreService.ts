import apiService from "@/services/apiService.ts";
import SaveRecordDto from "@/models/saveRecordDto.ts";

export default {
    async saveRecord(saveRecordDto: SaveRecordDto) {
        return await apiService.post(
            `${import.meta.env.VITE_API_BASE_URL}/record`,
            saveRecordDto
        );
    },

    async getLeaderboard(gameName: string) {
        return await apiService.get(
            `${import.meta.env.VITE_API_BASE_URL}/record/leaderboard/${gameName}`
        );
    },

    async getBestScore(gameName: string) {
        return await apiService.get(
            `${import.meta.env.VITE_API_BASE_URL}/record/bestScore/${gameName}`
        );
    },
};
