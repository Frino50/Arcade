<template>
    <div class="body-container">
        <Message
            v-if="estGagne()"
            :message="'Victoire'"
            :isGreen="true"
        ></Message>

        <div class="compteur-container">
            <div class="drapeau-compteur-container">
                <img
                    class="drapeau-container"
                    src="../assets/drapeau.png"
                    alt="drapeau.png"
                />
                {{ nombreDeBombes - nombreCasesCocherBombe }}
            </div>
            <div class="timer">
                {{ formatMinutes() }}:{{ formatSecondes() }}
            </div>
        </div>

        <div
            class="tableau-container"
            :style="{ '--colonnes': nombreDeColonnes }"
        >
            <div
                v-for="(casee, index) in listCase.flat()"
                :key="casee.id"
                class="case-container"
                :style="{
                    backgroundImage: getImage(casee),
                    backgroundColor: getColor(casee),
                }"
                @click="
                    reveleCase(
                        Math.floor(index / nombreDeColonnes),
                        index % nombreDeColonnes
                    )
                "
                @contextmenu.prevent="cocherBombe(casee)"
            >
                <div
                    v-if="
                        casee.visible &&
                        casee.nbrBombeAlentoure !== 0 &&
                        !casee.bombe
                    "
                    :class="`nombre-${casee.nbrBombeAlentoure}`"
                >
                    {{ casee.nbrBombeAlentoure }}
                </div>
            </div>
        </div>

        <div class="boutton-container">
            <button @click="genererList()">Reset</button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import Case from "../models/case.ts";
import Message from "@/components/Message.vue";

const nombreDeBombes = ref<number>(40);
const nombreDeLignes = ref<number>(14);
const nombreDeColonnes = ref<number>(18);
const perdu = ref<boolean>();
const tempsEcoule = ref<number>(0);
const firstClick = ref<boolean>(true);
const intervalId = ref<number | null>(null);

const listCase = ref<Case[][]>([]);

const nombreCasesCocherBombe = computed(() => {
    return listCase.value.flat().filter((c) => c.cocherBombe).length;
});

function formatMinutes(): string {
    const minutes = Math.floor(tempsEcoule.value / 60);
    return minutes < 10 ? `0${minutes}` : `${minutes}`;
}
function formatSecondes(): string {
    const secondes = tempsEcoule.value % 60;
    return secondes < 10 ? `0${secondes}` : `${secondes}`;
}

function getColor(casee: Case) {
    if (casee.bombe && casee.visible) return "#f65e3b";
    if (casee.visible) return "var(--marron-clair)";
    return "var(--marron)";
}
function getImage(casee: Case) {
    if (casee.visible && casee.bombe) {
        return "url(../src/assets/demineur.png)";
    }
    if (!casee.visible && casee.cocherBombe) {
        return "url(../src/assets/drapeau.png)";
    }
    return "none";
}

function getVoisins(i: number, j: number): [number, number][] {
    const voisins: [number, number][] = [];
    for (let di = -1; di <= 1; di++) {
        for (let dj = -1; dj <= 1; dj++) {
            if (di === 0 && dj === 0) continue;
            const ni = i + di;
            const nj = j + dj;
            if (
                ni >= 0 &&
                ni < nombreDeLignes.value &&
                nj >= 0 &&
                nj < nombreDeColonnes.value
            ) {
                voisins.push([ni, nj]);
            }
        }
    }
    return voisins;
}

function cocherBombe(clickedCase: Case) {
    if (!clickedCase.visible) {
        clickedCase.cocherBombe = !clickedCase.cocherBombe;
    }
}

function reveleCase(i: number, j: number) {
    if (!perdu.value) {
        const clickedCase = listCase.value[i][j];

        if (firstClick.value) {
            genererCarte(i, j);
            firstClick.value = false;
            intervalId.value = setInterval(
                miseAJourTimer,
                1000
            ) as unknown as number;
        }

        if (!clickedCase.visible && !clickedCase.cocherBombe) {
            clickedCase.visible = true;

            if (clickedCase.bombe) {
                perdu.value = true;
                allBombeTrue();
                clearInterval(intervalId.value as number);
                return;
            }

            if (clickedCase.nbrBombeAlentoure === 0) {
                for (const [ni, nj] of getVoisins(i, j)) {
                    reveleCase(ni, nj);
                }
            }
        }
    }
}

function genererCarte(firstI: number, firstJ: number) {
    const safe = new Set<string>();
    for (const [ni, nj] of getVoisins(firstI, firstJ).concat([
        [firstI, firstJ],
    ])) {
        safe.add(`${ni},${nj}`);
    }

    let bombs = 0;
    while (bombs < nombreDeBombes.value) {
        const i = Math.floor(Math.random() * nombreDeLignes.value);
        const j = Math.floor(Math.random() * nombreDeColonnes.value);
        if (!safe.has(`${i},${j}`) && !listCase.value[i][j].bombe) {
            listCase.value[i][j].bombe = true;
            bombs++;
        }
    }

    for (let i = 0; i < nombreDeLignes.value; i++) {
        for (let j = 0; j < nombreDeColonnes.value; j++) {
            const c = listCase.value[i][j];
            if (!c.bombe) {
                c.nbrBombeAlentoure = getVoisins(i, j).filter(
                    ([ni, nj]) => listCase.value[ni][nj].bombe
                ).length;
            }
        }
    }
}

function initialiserJeu() {
    listCase.value = [];
    perdu.value = false;
    tempsEcoule.value = 0;
    firstClick.value = true;
    if (intervalId.value) clearInterval(intervalId.value);

    for (let i = 0; i < nombreDeLignes.value; i++) {
        const ligne: Case[] = [];
        for (let j = 0; j < nombreDeColonnes.value; j++) {
            ligne.push(new Case(false));
        }
        listCase.value.push(ligne);
    }
}

function genererList() {
    initialiserJeu();
}

function allBombeTrue() {
    listCase.value.flat().forEach((c) => {
        if (c.bombe) c.visible = true;
    });
}

function estGagne(): boolean {
    if (listCase.value.length === 0) return false;
    return listCase.value
        .flat()
        .filter((c) => !c.bombe)
        .every((c) => c.visible);
}

function miseAJourTimer() {
    tempsEcoule.value++;
}

onMounted(() => {
    initialiserJeu();
});
</script>

<style scoped>
:root {
    --colonnes: 18;
}

.compteur-container {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 2rem;
}

.drapeau-compteur-container,
.timer {
    background: var(--marron-fonce);
    color: white;
    padding: 0.5rem 1rem;
    border-radius: 8px;
    font-size: 1.2rem;
    font-weight: bold;
    min-width: 80px;
    text-align: center;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.drapeau-container {
    width: 1.2rem;
    height: 1.2rem;
}

.tableau-container {
    display: grid;
    grid-template-columns: repeat(var(--colonnes), 2.5rem);
    gap: 4px;
    background: var(--marron);
    padding: 6px;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.case-container {
    height: 2.5rem;
    width: 2.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 6px;
    font-size: 1.2rem;
    font-weight: bold;
    cursor: pointer;
    transition:
        background 0.2s ease,
        transform 0.1s ease;
    box-shadow: inset 0 0 3px rgba(0, 0, 0, 0.2);
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
}

.case-container:hover {
    transform: scale(1.05);
}

.nombre-1 {
    color: #0000ff;
}
.nombre-2 {
    color: #008000;
}
.nombre-3 {
    color: #ff0000;
}
.nombre-4 {
    color: #000080;
}
.nombre-5 {
    color: #800000;
}
.nombre-6 {
    color: #008080;
}
.nombre-7 {
    color: #000000;
}
.nombre-8 {
    color: #808080;
}
</style>
