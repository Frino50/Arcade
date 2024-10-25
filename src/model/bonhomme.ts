import { Color } from "./enum/color";

let nextId = 1;

export default class Bonhomme {
    x: number;
    y: number;
    color: Color;
    time: number;
    id: number;
    diametre: number;

    constructor(x: number, y: number, color?: Color, diametre?: number) {
        this.x = x;
        this.y = y;
        this.color = color ?? this.getRandomColor();
        this.time = 0.5;
        this.id = nextId;
        this.diametre = diametre ?? this.generateRandomDiameter();
        nextId++;
    }

    private getRandomColor(): Color {
        const colors = Object.values(Color);
        const randomIndex = Math.floor(Math.random() * colors.length);
        return colors[randomIndex];
    }

    private generateRandomDiameter(): number {
        return Math.floor(Math.random() * (250 - 25 + 1)) + 25;
    }
}
