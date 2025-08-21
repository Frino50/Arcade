<template>
    <div class="body-container">
        <Message v-if="winner" :message="winner + ' gagne'" :isGreen="true" />
        <Message
            v-else-if="isBoardFull() && !winner"
            message="Match nul !"
            :isGreen="false"
        />

        <div class="tableau-container">
            <div
                v-for="(value, index) in listMorpion.flat()"
                :key="index"
                @click="handleClick(Math.floor(index / 3), index % 3, 1)"
                @contextmenu.prevent="
                    handleClick(Math.floor(index / 3), index % 3, 2)
                "
                class="case-container"
                :data-value="value"
            >
                {{ value === 1 ? "X" : value === 2 ? "O" : "" }}
            </div>
        </div>

        <div class="boutton-container">
            <button @click="resetBoard">Reset</button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import Message from "@/components/Message.vue";

const listMorpion = ref<number[][]>([]);
const winner = ref<string>();

onMounted(() => {
    resetBoard();
});

function isBoardFull() {
    return listMorpion.value.every((row) => !row.includes(0));
}

function handleClick(rowIndex: number, colIndex: number, value: number) {
    if (!winner.value && listMorpion.value[rowIndex][colIndex] === 0) {
        listMorpion.value[rowIndex].splice(colIndex, 1, value);

        if (checkWin(rowIndex, colIndex, value)) {
            winner.value = value === 1 ? "Joueur X" : "Joueur O";
            setTimeout(resetBoard, 1200);
        } else if (isBoardFull()) {
            setTimeout(resetBoard, 1200);
        }
    }
}

function resetBoard() {
    listMorpion.value = Array.from({ length: 3 }, () => [0, 0, 0]);
    winner.value = "";
}

function checkWin(rowIndex: number, colIndex: number, value: number) {
    if (listMorpion.value[rowIndex].every((cell) => cell === value))
        return true;
    if (listMorpion.value.every((row) => row[colIndex] === value)) return true;

    if (
        rowIndex === colIndex &&
        listMorpion.value.every((row, index) => row[index] === value)
    ) {
        return true;
    }

    return (
        rowIndex + colIndex === 2 &&
        listMorpion.value.every((row, index) => row[2 - index] === value)
    );
}
</script>

<style scoped>
.tableau-container {
    display: grid;
    grid-template-columns: repeat(3, 6rem);
    grid-template-rows: repeat(3, 6rem);
    gap: 4px;
    background: var(--marron);
    padding: 6px;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.case-container {
    height: 6rem;
    width: 6rem;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 2.5rem;
    font-weight: bold;
    cursor: pointer;

    border-radius: 6px;
    background: var(--marron);
    box-shadow: inset 0 0 3px rgba(0, 0, 0, 0.2);

    transition:
        background 0.2s ease,
        transform 0.1s ease;
}

.case-container:hover {
    transform: scale(1.05);
    background: var(--marron-clair);
}

.case-container[data-value="1"] {
    color: #ff4444;
}
.case-container[data-value="2"] {
    color: #4d94ff;
}
</style>
