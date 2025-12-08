export default class ModifSpriteDto {
    id: number;
    newName: string;
    scale: number;

    constructor(id: number, newName: string, scale: number) {
        this.id = id;
        this.newName = newName;
        this.scale = scale;
    }
}
