let nextId = 0;

export default class Boid {
    id: number;
    x: number;
    y: number;
    vx: number;
    vy: number;

    constructor() {
        this.id = nextId++;
        this.x = Math.random() * (window.innerWidth - 17);
        this.y = Math.random() * (window.innerHeight - -17);
        this.vx = Math.random() * 2 - 1;
        this.vy = Math.random() * 2 - 1;
    }
}
