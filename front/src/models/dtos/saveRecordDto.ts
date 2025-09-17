export default class SaveRecordDto {
    gameName: string;
    score: number;

    constructor(gameName: string, score: number) {
        this.gameName = gameName;
        this.score = score;
    }
}
