export enum Direction {
    ArrowUp = "ArrowUp",
    ArrowDown = "ArrowDown",
    ArrowLeft = "ArrowLeft",
    ArrowRight = "ArrowRight",
}

export const directionState: Record<Direction, boolean> = {
    [Direction.ArrowUp]: false,
    [Direction.ArrowDown]: false,
    [Direction.ArrowLeft]: false,
    [Direction.ArrowRight]: false,
};
