<template>
    <div
        class="dialog"
        v-if="props.open"
        ref="dialogRef"
        @mousedown="startDragging"
        @mouseup="stopDragging"
    >
        <div v-if="props.bacterieSelected">
            <p class="detail">Couleur : {{ props.bacterieSelected.color }}</p>
            <p class="detail">Id : {{ props.bacterieSelected.id }}</p>
            <p class="detail">
                x : {{ props.bacterieSelected.position.x.toFixed(2) }}
            </p>
            <p class="detail">
                y : {{ props.bacterieSelected.position.y.toFixed(2) }}
            </p>
            <p class="detail">
                Reproduction : {{ props.bacterieSelected.reproduction }}%
            </p>
            <p class="detail">
                Taille : {{ props.bacterieSelected.taille.toFixed(2) }}
            </p>
            <p class="detail">
                Vie : {{ props.bacterieSelected.vie.toFixed(2) }}
            </p>
            <p class="detail">
                Vie initiale : {{ props.bacterieSelected.vieInitial }}
            </p>
            <p class="detail">
                Génération : {{ props.bacterieSelected.generation }}
            </p>
            <p class="detail">
                Nombre d'enfants : {{ props.bacterieSelected.nbrEnfant }}
            </p>
        </div>
        <button @click="emit('close', false)">Fermer</button>
    </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import Bacterie from "../model/bacterie";
const mouseStartPosition = ref<{ x: number; y: number }>({ x: 0, y: 0 });

const props = defineProps({
    bacterieSelected: Bacterie,
    open: { type: Boolean, default: false },
});

const emit = defineEmits<(e: "close", type: boolean) => boolean>();

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
    left: 1300px;
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
