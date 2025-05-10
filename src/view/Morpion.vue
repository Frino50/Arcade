<template>
    <div class="body-container">
        <div v-for="(row, rowIndex) in listMorpion" :key="rowIndex" class="row">
            <div
                v-for="(value, colIndex) in row"
                :key="colIndex"
                @click="handleClick(rowIndex, colIndex, 1)"
                @contextmenu.prevent="handleClick(rowIndex, colIndex, 2)"
                class="case"
                :data-value="value"
            >
                {{ value === 1 ? "X" : value === 2 ? "O" : "" }}
            </div>
        </div>
        <button class="bouton-container" @click="resetBoard">
            Réinitialiser tableau
        </button>
        <Message
            v-if="winner"
            :message="winner + ' gagne'"
            :isGreen="true"
        ></Message>
        <Message
            v-else-if="isBoardFull() && !winner"
            message="Match nul !"
            :isGreen="false"
        ></Message>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import Message from "../composant/Message.vue";

const listMorpion = ref<number[][]>([]);
const winner = ref<string>();

onMounted(() => {
    resetBoard();
});

function isBoardFull() {
    for (const row of listMorpion.value) {
        if (row.includes(0)) {
            return false;
        }
    }
    return true;
}

function handleClick(rowIndex: number, colIndex: number, value: number) {
    if (!winner.value && listMorpion.value[rowIndex][colIndex] === 0) {
        listMorpion.value[rowIndex].splice(colIndex, 1, value);

        if (checkWin(rowIndex, colIndex, value)) {
            winner.value = value === 1 ? "Joueur X" : "Joueur O";
            setTimeout(resetBoard, 1000);
        } else if (isBoardFull()) {
            setTimeout(resetBoard, 1000);
        }
    }
}

function resetBoard() {
    listMorpion.value = Array.from({ length: 3 }, () => [0, 0, 0]);
    winner.value = "";
}

function checkWin(rowIndex: number, colIndex: number, value: number) {
    if (listMorpion.value[rowIndex].every((cell) => cell === value)) {
        return true;
    }

    if (listMorpion.value.every((row) => row[colIndex] === value)) {
        return true;
    }

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
.body-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
}

.row {
    display: flex;
}

.case {
    height: 10rem;
    width: 10rem;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 3rem;
    cursor: pointer;
    border: 1px solid white;
    border-bottom: none;
    border-right: none;
}

.row:first-child .case {
    border-top: none;
}

.case:first-child {
    border-left: none;
}

.case[data-value="1"] {
    color: #ff4444;
}

.case[data-value="2"] {
    color: #4d94ff;
}

.bouton-container {
    margin-top: 5rem;
    width: 15rem;
    padding: 10px;
    cursor: pointer;
}
</style>
