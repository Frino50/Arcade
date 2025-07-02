let nextId = 1;

export default class Case {
    id: number;
    bombe: boolean;
    visible: boolean;
    nbrBombeAlentoure: number;
    cocherBombe: boolean;

    constructor(bombe: boolean, visible: boolean) {
        this.id = nextId++;
        this.bombe = bombe;
        this.visible = visible;
        this.nbrBombeAlentoure = 0;
        this.cocherBombe = false;
    }
}
