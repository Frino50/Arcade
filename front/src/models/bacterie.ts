let nextId = 1;

export default class Bacterie {
    id: number;
    position: { x: number; y: number };
    color: string;
    reproduction: number;
    tailleInitial: number;
    taille: number;
    vie: number;
    vieInitial: number;
    generation: number;
    nbrEnfant: number;
    division: boolean;

    constructor(
        x: number,
        y: number,
        color: string,
        reproduction: number,
        taille: number,
        vie: number,
        generation: number,
        nbrEnfant: number
    ) {
        this.position = { x, y };
        this.color = color;
        this.reproduction = reproduction;
        this.tailleInitial = taille;
        this.taille = taille;
        this.vie = vie;
        this.id = nextId;
        this.vieInitial = vie;
        this.generation = generation;
        this.nbrEnfant = nbrEnfant;
        this.division = false;
        nextId++;
    }
}
