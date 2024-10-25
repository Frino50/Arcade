<template>
    <div
        class="dialog"
        v-if="props.open"
        ref="dialogRef"
        @mousedown="startDragging"
        @mouseup="stopDragging"
    >
        <div style="display: flex; gap: 0.5rem">
            <div>
                <div>Stats</div>
                <div>Taille</div>
                <div>Reproduction</div>
                <div>Vie</div>
                <div>Génération</div>
                <div>Enfants</div>
            </div>
            <div style="display: flex; gap: 0.5rem">
                <div>
                    <div>+Grand</div>
                    <div>
                        {{
                            plusGrandPlusPetit("tailleInitial", true).toFixed(2)
                        }}
                    </div>
                    <div>
                        {{
                            plusGrandPlusPetit("reproduction", true).toFixed(2)
                        }}
                    </div>
                    <div>
                        {{ plusGrandPlusPetit("vieInitial", true).toFixed(2) }}
                    </div>
                    <div>
                        {{ plusGrandPlusPetit("generation", true).toFixed(2) }}
                    </div>
                    <div>
                        {{ plusGrandPlusPetit("nbrEnfant", true).toFixed(2) }}
                    </div>
                </div>
                <div>
                    <div>+Petit</div>
                    <div>
                        {{
                            plusGrandPlusPetit("tailleInitial", false).toFixed(
                                2
                            )
                        }}
                    </div>
                    <div>
                        {{
                            plusGrandPlusPetit("reproduction", false).toFixed(2)
                        }}
                    </div>
                    <div>
                        {{ plusGrandPlusPetit("vieInitial", false).toFixed(2) }}
                    </div>
                    <div>
                        {{ plusGrandPlusPetit("generation", false).toFixed(2) }}
                    </div>
                    <div>
                        {{ plusGrandPlusPetit("nbrEnfant", false).toFixed(2) }}
                    </div>
                </div>
            </div>
        </div>
        <button @click="emit('close', false)">Fermer</button>
    </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import Bacterie from "../model/bacterie";
const mouseStartPosition = ref<{ x: number; y: number }>({ x: 0, y: 0 });

const props = defineProps<{
    listBacterie: Bacterie[];
    open: boolean;
}>();

const emit = defineEmits<(e: "close", type: boolean) => boolean>();

function plusGrandPlusPetit(
    attribut:
        | "tailleInitial"
        | "reproduction"
        | "vieInitial"
        | "generation"
        | "nbrEnfant",
    grand: boolean
): number {
    let nbr: number = grand ? 0 : 100;
    if (grand) {
        props.listBacterie.forEach((bacterie) => {
            if (bacterie[attribut] > nbr) {
                nbr = bacterie[attribut];
            }
        });
    } else {
        props.listBacterie.forEach((bacterie) => {
            if (bacterie[attribut] < nbr) {
                nbr = bacterie[attribut];
            }
        });
    }
    return nbr;
}

const dialogRef = ref<HTMLElement>();

function startDragging(event: MouseEvent) {
    if (event.button === 0) {
        mouseStartPosition.value = {
            x:
                event.pageX -
                (dialogRef.value as HTMLElement).getBoundingClientRect().left,
            y:
                event.pageY -
                (dialogRef.value as HTMLElement).getBoundingClientRect().top,
        };

        document.addEventListener("mousemove", handleDrag);
    }
}

function handleDrag(event: MouseEvent) {
    if (dialogRef.value && mouseStartPosition.value) {
        const dialogWidth = dialogRef.value.offsetWidth;
        const dialogHeight = dialogRef.value.offsetHeight;
        const newLeft = event.clientX - mouseStartPosition.value.x;
        const newTop = event.clientY - mouseStartPosition.value.y;

        if (newLeft >= 0 && newLeft + dialogWidth <= window.innerWidth) {
            dialogRef.value.style.left = `${newLeft}px`;
        }

        if (newTop >= 0 && newTop + dialogHeight <= window.innerHeight) {
            dialogRef.value.style.top = `${newTop}px`;
        }
    }
}

function stopDragging() {
    document.removeEventListener("mousemove", handleDrag);
}
</script>
<style scoped>
.dialog {
    position: absolute;
    top: 20px;
    left: 1200px;
    padding: 10px;
    background-color: gray;
    display: flex;
    flex-direction: column;
    border: 5px solid black;
    cursor: pointer;
    z-index: 1;
}
.detail {
    margin: 5px 0;
}
</style>
