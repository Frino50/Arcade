<template>
    <GameLayout :game-name="GAME_NAME" ref="gameLayoutRef">
        <template #header>
            <div class="header-container">
                <img
                    class="drapeau-container"
                    src="../assets/drapeau.png"
                    alt="drapeau.png"
                />
                {{ nombreDeBombes - nombreCasesCocherBombe }}
            </div>
            <div>{{ formatMinutes() }}:{{ formatSecondes() }}</div>
        </template>
        <template #default>
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
                        backgroundColor:
                            casee.visible && !casee.bombe
                                ? '#122b3d'
                                : '#0b1d26',
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

            <button @click="genererList()">Reset</button>
        </template>
    </GameLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import Case from "../models/case.ts";
import SaveRecordDto from "@/models/dtos/saveRecordDto.ts";
import scoreService from "@/services/scoreService.ts";
import { GameType } from "@/models/enums/gameType.ts";
import GameLayout from "@/components/GameLayout.vue";

const nombreDeBombes = ref<number>(40);
const nombreDeLignes = ref<number>(14);
const nombreDeColonnes = ref<number>(18);
const tempsEcoule = ref<number>(0);
const intervalId = ref<number | null>(null);
const perdu = ref<boolean>();
const firstClick = ref<boolean>(true);
const GAME_NAME = GameType.DEMINEUR;
const gameLayoutRef = ref<InstanceType<typeof GameLayout>>();
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
        verifierToutesBombesTrouvees();
    }
}

async function verifierToutesBombesTrouvees() {
    const bombes = listCase.value.flat().filter((c) => c.bombe);
    const gagne =
        bombes.length === nombreDeBombes.value &&
        bombes.every((c) => c.cocherBombe) &&
        nombreCasesCocherBombe.value === nombreDeBombes.value;

    if (gagne) {
        clearInterval(intervalId.value as number); // stop timer

        // sauvegarder le temps en ms comme score
        const score = tempsEcoule.value * 1000;
        if (score > 0) {
            const recordData = new SaveRecordDto(GAME_NAME, score);
            try {
                await scoreService.saveRecord(recordData);
                gameLayoutRef.value?.refresh();
            } catch (error) {
                console.error(
                    "Erreur lors de la sauvegarde du record :",
                    error
                );
            }
        }
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
.header-container {
    display: flex;
    align-items: center;
    gap: 0.3rem;
}

.drapeau-container {
    width: 1.4rem;
    height: 1.4rem;
    filter: drop-shadow(0 0 6px var(--futurist-accent));
}

.tableau-container {
    display: grid;
    grid-template-columns: repeat(var(--colonnes), 2.8rem);
    gap: 0.12rem;
    transition: all 0.3s ease;
}

.case-container {
    height: 2.8rem;
    width: 2.8rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 0.6rem;
    font-size: 1.2rem;
    font-weight: bold;
    cursor: pointer;
    transition:
        background 0.25s ease,
        transform 0.15s ease,
        box-shadow 0.3s ease;
    background-color: var(--futurist-bg-dark);
    box-shadow:
        inset 0 0 4px var(--futurist-shadow),
        0 0 2px var(--futurist-shadow-strong);
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
}

.case-container[style*="122b3d"] {
    background-color: var(--futurist-bg-mid);
    box-shadow:
        inset 0 0 6px var(--futurist-shadow),
        0 0 4px var(--futurist-shadow-strong);
}

.case-container:hover {
    transform: scale(1.08);
    box-shadow:
        0 0 12px var(--futurist-shadow-strong),
        inset 0 0 8px var(--futurist-shadow);
    background-color: var(--futurist-list-bg-hover);
}

/* Nombres de 1 à 8 */
.nombre-1 {
    color: var(--piece-s-light);
    text-shadow: 0 0 6px var(--piece-s-light);
}
.nombre-2 {
    color: var(--piece-s-dark);
    text-shadow: 0 0 6px var(--piece-s-dark);
}
.nombre-3 {
    color: var(--piece-j-light);
    text-shadow: 0 0 6px var(--piece-j-light);
}
.nombre-4 {
    color: var(--piece-l-light);
    text-shadow: 0 0 6px var(--piece-l-light);
}
.nombre-5 {
    color: var(--piece-l-dark);
    text-shadow: 0 0 6px var(--piece-l-dark);
}
.nombre-6 {
    color: var(--piece-t-light);
    text-shadow: 0 0 6px var(--piece-t-light);
}
.nombre-7 {
    color: var(--piece-t-dark);
    text-shadow: 0 0 6px var(--piece-t-dark);
}
.nombre-8 {
    color: var(--text-color);
    text-shadow: 0 0 6px var(--text-color);
}

.case-container[style*="demineur.png"] {
    filter: drop-shadow(0 0 8px var(--futurist-danger));
}

.case-container[style*="drapeau.png"] {
    filter: drop-shadow(0 0 6px var(--futurist-accent));
}
</style>
