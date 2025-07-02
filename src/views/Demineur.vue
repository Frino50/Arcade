<template>
    <div class="body-container">
        <Message v-if="perdu" :message="'Défaite'" :isGreen="false"></Message>
        <Message
            v-if="estGagne()"
            :message="'Victoire'"
            :isGreen="true"
        ></Message>
        <div class="boutton-container">
            <button @click="genererList()">Recommencer</button>
            <button @click="tricher()">Tricher</button>
        </div>
        <div class="compteur-container">
            <div class="drapeau-compteur-container">
                <img
                    class="drapeau-container"
                    src="../assets/drapeau.png"
                    alt="drapeau.png"
                />
                {{ 40 - nombreCasesCocherBombe() }}
            </div>
            {{ formatMinutes() }}:{{ formatSecondes() }}
        </div>
        <div
            class="tableau-container"
            :class="{ 'tableau-inactif': perdu }"
            :style="{ width: `${nombreDeLignes * 2.6375}rem` }"
        >
            <div
                class="case-container"
                v-for="casee in listCase"
                :key="casee.id"
                :style="{
                    backgroundImage: getImage(casee),
                    backgroundColor: getColor(casee),
                }"
                @click="reveleCase(casee)"
                @contextmenu.prevent="cocherBombe(casee)"
            >
                <div v-if="casee.visible && casee.nbrBombeAlentoure !== 0">
                    {{ casee.nbrBombeAlentoure }}
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import Case from "../models/case.ts";
import Message from "@/components/Message.vue";

const listCase = ref<Case[]>([]);
const taille = ref<number>(252);
const nombreDeBombes = ref<number>(40);
const nombreDeLignes = ref<number>(18);
const perdu = ref<boolean>();
const tempsEcoule = ref<number>(0);
const tricherConst = ref<boolean>(false);

function formatMinutes(): string {
    const minutes = Math.floor(tempsEcoule.value / 60);
    return minutes < 10 ? `0${minutes}` : `${minutes}`;
}

function formatSecondes(): string {
    const secondes = tempsEcoule.value % 60;
    return secondes < 10 ? `0${secondes}` : `${secondes}`;
}

function getColor(casee: Case) {
    if (casee.bombe && casee.visible) {
        return "red";
    }
    if (casee.visible) {
        return "white";
    } else {
        return "gray";
    }
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

function cocherBombe(clickedCase: Case) {
    if (!clickedCase.visible) {
        clickedCase.cocherBombe = !clickedCase.cocherBombe;
    }
}

function reveleCase(clickedCase: Case) {
    if (!clickedCase.visible && !clickedCase.cocherBombe) {
        // Marque la case comme visible
        clickedCase.visible = true;
        if (clickedCase.bombe) {
            perdu.value = true;
            allBombeTrue();
        }

        // Vérifie si la case ne contient pas de bombe et le nombre de bombes alentour est égal à zéro
        if (clickedCase.nbrBombeAlentoure === 0 && !clickedCase.bombe) {
            // Obtient les indices de ligne et colonne de la case dans une grille de nombreDeLignes.value colonnes
            const ligne = Math.floor(
                listCase.value.indexOf(clickedCase) / nombreDeLignes.value
            );
            const colonne =
                listCase.value.indexOf(clickedCase) % nombreDeLignes.value;
            // Double boucle pour parcourir les voisins de la case
            for (let j = -1; j <= 1; j++) {
                for (let k = -1; k <= 1; k++) {
                    // Calcul de l'index du voisin dans le tableau listCase.value
                    const voisinIndex =
                        (ligne + j) * nombreDeLignes.value + (colonne + k);

                    // Vérifie les conditions pour s'assurer que le voisin est valide
                    if (
                        voisinIndex >= 0 &&
                        voisinIndex < taille.value &&
                        ligne + j ===
                            Math.floor(voisinIndex / nombreDeLignes.value) &&
                        colonne + k === voisinIndex % nombreDeLignes.value
                    ) {
                        // Récupération du voisin à partir de l'index calculé
                        const voisin = listCase.value[voisinIndex];

                        // Vérifie si le voisin existe, n'est pas déjà visible et n'a pas de drapeau
                        if (voisin && !voisin.visible && !voisin.cocherBombe) {
                            // Appel récursif de la fonction reveleCase pour révéler le voisin
                            reveleCase(voisin);
                        }
                    }
                }
            }
        }
    }
}

function genererList() {
    tempsEcoule.value = 0;
    perdu.value = false;
    listCase.value = [];
    let bombesPlacees = 0;

    // Génération initiale de la liste
    for (let i = 0; i < taille.value; i++) {
        const bombe =
            bombesPlacees < nombreDeBombes.value && Math.random() < 0.4;
        const nouvelleCase = new Case(bombe, tricherConst.value);
        listCase.value.push(nouvelleCase);
        if (bombe) {
            bombesPlacees++;
        }
    }

    // Mélanger la liste
    for (let i = listCase.value.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [listCase.value[i], listCase.value[j]] = [
            listCase.value[j],
            listCase.value[i],
        ];
    }

    // Mettre à jour nbrBombeAlentoure
    for (let i = 0; i < taille.value; i++) {
        const caseCourante = listCase.value[i];

        if (!caseCourante.bombe) {
            const ligne = Math.floor(i / nombreDeLignes.value);
            const colonne = i % nombreDeLignes.value;

            // Vérifier les cases autour
            for (let j = -1; j <= 1; j++) {
                for (let k = -1; k <= 1; k++) {
                    const voisinIndex =
                        (ligne + j) * nombreDeLignes.value + (colonne + k);

                    if (
                        voisinIndex >= 0 &&
                        voisinIndex < taille.value &&
                        ligne + j ===
                            Math.floor(voisinIndex / nombreDeLignes.value) &&
                        colonne + k === voisinIndex % nombreDeLignes.value
                    ) {
                        const voisin = listCase.value[voisinIndex];

                        if (voisin.bombe) {
                            caseCourante.nbrBombeAlentoure++;
                        }
                    }
                }
            }
        }
    }
}

function nombreCasesCocherBombe(): number {
    return listCase.value.filter((casee) => casee.cocherBombe).length;
}

function allBombeTrue() {
    listCase.value.forEach((value: Case) => {
        if (value.bombe) {
            value.visible = true;
        }
    });
}

function tricher() {
    tricherConst.value = !tricherConst.value;
    listCase.value.forEach((value: Case) => {
        value.visible = !value.visible;
    });
}

function estGagne(): boolean {
    return listCase.value.every((casee) => casee.cocherBombe === casee.bombe);
}

function miseAJourTimer() {
    tempsEcoule.value++;
}

onMounted(() => {
    //Ouvrir une fenetre

    // const myExternalWindow = window.open(
    //     "http://localhost:5173",
    //     "myWindowName",
    //     "resizable"
    // );
    // if (myExternalWindow) {
    //     myExternalWindow.resizeTo(200, 200);
    // }

    genererList();
    setInterval(miseAJourTimer, 1000);
});
</script>

<style scoped>
.body-container {
    height: 100%;
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    gap: 1.5rem;
    color: black;
}

.boutton-container {
    display: flex;
    gap: 1.5rem;
}

.compteur-container {
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    gap: 5rem;
}

.drapeau-compteur-container {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.3rem;
}

.drapeau-container {
    width: 1.5rem;
    height: 1.5rem;
}
.tableau-container {
    display: flex;
    flex-wrap: wrap;
}
.tableau-inactif {
    pointer-events: none;
}
.case-container {
    border: 1px solid black;
    height: 2.5rem;
    width: 2.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
    font-size: 1.5rem;
}
</style>
