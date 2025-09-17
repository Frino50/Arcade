export default class ConnexionDto {
    pseudo: string;
    password: string;

    constructor(pseudo: string, password: string) {
        this.pseudo = pseudo;
        this.password = password;
    }
}
