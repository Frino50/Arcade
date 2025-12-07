import apiService from "@/services/apiService";
import SpriteInfo from "@/models/SpriteInfos.ts";

export default {
    async uploadSprite(formData: FormData) {
        return await apiService.post("/sprite", formData, {
            headers: { "Content-Type": "multipart/form-data" },
        });
    },

    async getAllSpritesInfos() {
        return await apiService.get<SpriteInfo[]>("/sprite/summary");
    },

    async deleteSprite(name: string) {
        return await apiService.delete(`/sprite/delete/${name}`);
    },

    async renameSprite(id: number, name: string) {
        return await apiService.put(`/sprite/rename/${id}/${name}`);
    },
};
