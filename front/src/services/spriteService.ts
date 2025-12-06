import apiService from "@/services/apiService.ts";
import SaveRecordDto from "@/models/dtos/saveRecordDto.ts";

export default {
    async saveRecord(saveRecordDto: SaveRecordDto) {
        return await apiService.post("/record", saveRecordDto);
    },

    async getLeaderboard(gameName: string) {
        return await apiService.get(`/record/leaderboard/${gameName}`);
    },

    async getBestScore(gameName: string) {
        return await apiService.get(`/record/bestScore/${gameName}`);
    },
};
