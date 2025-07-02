export class Nourriture {
    position: { x: number; y: number };
    valeur: number;

    constructor(x: number, y: number, valeur: number) {
        this.position = { x, y };
        this.valeur = valeur;
    }
}
