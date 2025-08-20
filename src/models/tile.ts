export default interface Tile {
    id: number;
    value: number;
    row: number;
    col: number;
    merged?: boolean;
}
