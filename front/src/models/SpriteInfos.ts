export default interface SpriteInfo {
    animationId: number;
    name: string;
    newName: string;
    imageUrl: string;
    width: number;
    height: number;
    frames: number;
    scale: number;
    frameRate: number;
    hitboxX?: number;
    hitboxY?: number;
    hitboxWidth?: number;
    hitboxHeight?: number;
}

export interface Hitbox {
    x: number;
    y: number;
    width: number;
    height: number;
}
