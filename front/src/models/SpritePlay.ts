import SpriteInfo from "@/models/SpriteInfos.ts";

export default interface SpritePlay {
    id: number;
    idle: SpriteInfo;
    walk: SpriteInfo;
    attack: SpriteInfo;
}
