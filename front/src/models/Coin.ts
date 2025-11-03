export interface Coin {
    nom: string;
    description: string;
    prix: number;
    poids?: number;
    prixParGramme?: number;
    link: string;
    statut: string;
}
