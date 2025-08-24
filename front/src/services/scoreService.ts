import apiService from "@/services/apiService.ts";
import SaveRecordDto from "@/models/saveRecordDto.ts";

export default {
    async saveRecord(saveRecordDto: SaveRecordDto) {
        return await apiService.post(
            `http://localhost:8080/api/record`,
            saveRecordDto
        );
    },

    async getLeaderboard(gameName: string) {
        return await apiService.get(
            `http://localhost:8080/api/record/leaderboard/${gameName}`
        );
    },

    async getBestScore(gameName: string) {
        return await apiService.get(
            `http://localhost:8080/api/record/bestScore/${gameName}`
        );
    },
};
