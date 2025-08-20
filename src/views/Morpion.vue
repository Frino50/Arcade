<template>
    <div class="body-container">
        <!-- Grille -->
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

        <!-- Bouton reset -->
        <button class="bouton-container" @click="resetBoard">Reset</button>

        <!-- Messages -->
        <Message v-if="winner" :message="winner + ' gagne'" :isGreen="true" />
        <Message
            v-else-if="isBoardFull() && !winner"
            message="Match nul !"
            :isGreen="false"
        />
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
.body-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2rem;
    min-height: 100vh;
}

.row {
    display: flex;
    gap: 2.1rem;
}

.case {
    height: 8rem;
    width: 8rem;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 3rem;
    font-weight: bold;
    cursor: pointer;

    background: var(--marron);
    border-radius: 12px;
    margin: 4px;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);

    transition:
        transform 0.15s ease,
        background 0.2s ease;
}

.case:hover {
    transform: scale(1.05);
    background: var(--marron-clair);
}

/* X et O */
.case[data-value="1"] {
    color: #ff4444; /* rouge vif */
}
.case[data-value="2"] {
    color: #4d94ff; /* bleu */
}
</style>
