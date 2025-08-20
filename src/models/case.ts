export default class Case {
    id: number;
    bombe: boolean;
    cocherBombe: boolean;
    visible: boolean;
    nbrBombeAlentoure: number;

    constructor(bombe: boolean) {
        this.id = Math.random();
        this.bombe = bombe;
        this.cocherBombe = false;
        this.visible = false;
        this.nbrBombeAlentoure = 0;
    }
}
