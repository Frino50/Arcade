export default class ModifSpriteDto {
    oldName: string;
    newName: string;
    scale: number;

    constructor(oldName: string, newName: string, scale: number) {
        this.oldName = oldName;
        this.newName = newName;
        this.scale = scale;
    }
}
