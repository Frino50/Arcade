export default class ModifSpriteDto {
    id: number;
    name: string;
    scale: number;

    constructor(id: number, name: string, scale: number) {
        this.id = id;
        this.name = name;
        this.scale = scale;
    }
}
