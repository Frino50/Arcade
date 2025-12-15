import apiService from "@/services/apiService";
import SpriteInfo from "@/models/SpriteInfos.ts";
import ModifSpriteDto from "@/models/dtos/modifSpriteDto.ts";

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

    async renameSprite(modifSpriteDto: ModifSpriteDto) {
        return await apiService.put(`/sprite/rename`, modifSpriteDto);
    },

    async getImage(spritePath: string) {
        const response = await apiService.get(
            `/sprite/sprite-storage/${spritePath}`,
            {
                responseType: "blob",
            }
        );
        return URL.createObjectURL(response.data);
    },

    async getAllSprites(spriteId: number) {
        const response = await apiService.get(
            `/sprite/all-sprites/${spriteId}`
        );
        return response.data;
    },
};
