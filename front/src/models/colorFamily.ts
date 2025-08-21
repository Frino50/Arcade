export default class ColorFamily {
    name: string;
    range: string[];
    test: (r: number, g: number, b: number) => boolean;

    constructor(
        name: string,
        range: string[],
        test: (r: number, g: number, b: number) => boolean
    ) {
        this.name = name;
        this.range = range;
        this.test = test;
    }
}
