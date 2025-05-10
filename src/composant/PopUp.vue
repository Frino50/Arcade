<template>
    <Teleport to="body">
        <Transition name="dialog-fade">
            <div
                v-if="modelValue"
                ref="dialogRef"
                :aria-labelledby="dialogId + '-title'"
                :class="{ resizable: resizable }"
                :style="dialogStyle"
                class="dialog"
                tabindex="-1"
                @keydown.esc="closeOnEsc && close()"
            >
                <div
                    :id="dialogId + '-title'"
                    class="dialog-header"
                    @mousedown="startDragging"
                    @touchstart="startDragging"
                >
                    <slot name="header">
                        <span class="dialog-title">{{ title }}</span>
                    </slot>
                    <button
                        aria-label="Fermer"
                        class="close-btn"
                        @click="close()"
                    >
                        &times;
                    </button>
                </div>

                <div class="dialog-content">
                    <slot></slot>
                </div>

                <div v-if="$slots.footer" class="dialog-footer">
                    <slot name="footer"></slot>
                </div>

                <div
                    v-if="resizable"
                    class="resize-handle"
                    @mousedown="startResizing"
                    @touchstart="startResizing"
                ></div>
            </div>
        </Transition>
    </Teleport>
</template>

<script lang="ts" setup>
import {
    computed,
    nextTick,
    onBeforeUnmount,
    onMounted,
    ref,
    watch,
} from "vue";

interface Position {
    x: number | null;
    y: number | null;
}

const props = defineProps({
    modelValue: {
        type: Boolean,
        default: false,
    },
    title: {
        type: String,
        default: "Gestion",
    },
    initialPosition: {
        type: Object as () => Position,
        default: () => ({ x: null, y: null }),
    },
    width: {
        type: [Number, String],
        default: 300,
    },
    height: {
        type: [Number, String],
        default: "auto",
    },
    resizable: {
        type: Boolean,
        default: true,
    },
    closeOnEsc: {
        type: Boolean,
        default: false,
    },
    minWidth: {
        type: Number,
        default: 200,
    },
    minHeight: {
        type: Number,
        default: 100,
    },
    maxWidth: {
        type: Number,
        default: 800,
    },
    maxHeight: {
        type: Number,
        default: 600,
    },
});

const emit = defineEmits(["update:modelValue", "close"]);

const dialogRef = ref<HTMLElement | null>(null);
const dialogId = ref(`dialog-${Math.random().toString(36).substring(2, 11)}`);
const position = ref<{ x: number | null; y: number | null }>({
    x: null,
    y: null,
});
const size = ref<{ width: number | string; height: number | string }>({
    width: parseFloat(props.width as string) || 300,
    height: props.height || "auto",
});

const isDragging = ref(false);
const mouseStartPosition = ref<{ x: number; y: number }>({ x: 0, y: 0 });
const dialogStartPosition = ref<{ x: number; y: number }>({ x: 0, y: 0 });

const isResizing = ref(false);
const resizeStartPosition = ref<{ x: number; y: number }>({ x: 0, y: 0 });
const initialSize = ref<{ width: number; height: number }>({
    width: 0,
    height: 0,
});

const dialogStyle = computed(() => {
    const style: any = {};

    if (position.value.x !== null) style.left = `${position.value.x}px`;
    if (position.value.y !== null) style.top = `${position.value.y}px`;

    if (size.value.width) style.width = size.value.width;
    if (size.value.height && size.value.height !== "auto") {
        style.height = size.value.height;
    }

    return style;
});

function centerDialog() {
    if (!dialogRef.value) return;

    const windowWidth = window.innerWidth;
    const windowHeight = window.innerHeight;
    const dialogWidth = dialogRef.value.offsetWidth;
    const dialogHeight = dialogRef.value.offsetHeight;

    position.value = {
        x: Math.max(0, (windowWidth - dialogWidth) / 2),
        y: Math.max(0, (windowHeight - dialogHeight) / 3),
    };
}

function startDragging(event: MouseEvent | TouchEvent) {
    if (event instanceof MouseEvent && event.button !== 0) return;
    if (!dialogRef.value) return;

    event.preventDefault();
    isDragging.value = true;

    const clientX =
        event instanceof MouseEvent ? event.clientX : event.touches[0].clientX;
    const clientY =
        event instanceof MouseEvent ? event.clientY : event.touches[0].clientY;

    mouseStartPosition.value = { x: clientX, y: clientY };
    const rect = dialogRef.value.getBoundingClientRect();
    dialogStartPosition.value = { x: rect.left, y: rect.top };

    document.addEventListener("mousemove", handleDrag);
    document.addEventListener("touchmove", handleDrag, { passive: false });
    document.addEventListener("mouseup", stopDragging);
    document.addEventListener("touchend", stopDragging);
}

function handleDrag(event: MouseEvent | TouchEvent) {
    if (!isDragging.value || !dialogRef.value) return;

    event.preventDefault();

    const clientX =
        event instanceof MouseEvent ? event.clientX : event.touches[0].clientX;
    const clientY =
        event instanceof MouseEvent ? event.clientY : event.touches[0].clientY;

    const deltaX = clientX - mouseStartPosition.value.x;
    const deltaY = clientY - mouseStartPosition.value.y;

    let newLeft = dialogStartPosition.value.x + deltaX;
    let newTop = dialogStartPosition.value.y + deltaY;

    const dialogWidth = dialogRef.value.offsetWidth;
    const dialogHeight = dialogRef.value.offsetHeight;

    const maxLeft = window.innerWidth - dialogWidth;
    const maxTop = window.innerHeight - dialogHeight;

    newLeft = Math.max(0, Math.min(newLeft, maxLeft));
    newTop = Math.max(0, Math.min(newTop, maxTop));

    position.value = { x: newLeft, y: newTop };
}

function stopDragging() {
    isDragging.value = false;

    document.removeEventListener("mousemove", handleDrag);
    document.removeEventListener("touchmove", handleDrag);
    document.removeEventListener("mouseup", stopDragging);
    document.removeEventListener("touchend", stopDragging);
}

function startResizing(event: MouseEvent | TouchEvent) {
    if (event instanceof MouseEvent && event.button !== 0) return;
    if (!dialogRef.value) return;

    event.preventDefault();
    isResizing.value = true;

    const clientX =
        event instanceof MouseEvent ? event.clientX : event.touches[0].clientX;
    const clientY =
        event instanceof MouseEvent ? event.clientY : event.touches[0].clientY;

    resizeStartPosition.value = { x: clientX, y: clientY };
    initialSize.value = {
        width: dialogRef.value.offsetWidth,
        height: dialogRef.value.offsetHeight,
    };

    document.addEventListener("mousemove", handleResize);
    document.addEventListener("touchmove", handleResize, { passive: false });
    document.addEventListener("mouseup", stopResizing);
    document.addEventListener("touchend", stopResizing);
}

function handleResize(event: MouseEvent | TouchEvent) {
    if (!isResizing.value || !dialogRef.value) return;

    event.preventDefault();

    const clientX =
        event instanceof MouseEvent ? event.clientX : event.touches[0].clientX;
    const clientY =
        event instanceof MouseEvent ? event.clientY : event.touches[0].clientY;

    const deltaX = clientX - resizeStartPosition.value.x;
    const deltaY = clientY - resizeStartPosition.value.y;

    let newWidth = initialSize.value.width + deltaX;
    let newHeight = initialSize.value.height + deltaY;

    newWidth = Math.max(props.minWidth, Math.min(newWidth, props.maxWidth));
    newHeight = Math.max(props.minHeight, Math.min(newHeight, props.maxHeight));

    size.value = { width: newWidth, height: newHeight };
}

function stopResizing() {
    isResizing.value = false;

    document.removeEventListener("mousemove", handleResize);
    document.removeEventListener("touchmove", handleResize);
    document.removeEventListener("mouseup", stopResizing);
    document.removeEventListener("touchend", stopResizing);
}

function close() {
    emit("update:modelValue", false);
    emit("close");
}

function setupFocusTrap() {
    if (!dialogRef.value) return;

    const focusableElements = dialogRef.value.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );

    if (focusableElements.length) {
        (focusableElements[0] as HTMLElement).focus();
    } else {
        dialogRef.value.focus();
    }

    dialogRef.value.addEventListener("keydown", handleTabKey);
}

function handleTabKey(event: KeyboardEvent) {
    if (!dialogRef.value || event.key !== "Tab") return;

    const focusableElements = Array.from(
        dialogRef.value.querySelectorAll(
            'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        )
    ) as HTMLElement[];

    if (focusableElements.length === 0) return;

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    if (event.shiftKey && document.activeElement === firstElement) {
        event.preventDefault();
        lastElement.focus();
    } else if (!event.shiftKey && document.activeElement === lastElement) {
        event.preventDefault();
        firstElement.focus();
    }
}

watch(
    () => props.modelValue,
    (newVal) => {
        if (newVal) {
            if (
                props.initialPosition.x === null ||
                props.initialPosition.y === null
            ) {
                nextTick(() => centerDialog());
            } else {
                position.value = {
                    x: props.initialPosition.x,
                    y: props.initialPosition.y,
                };
            }

            nextTick(() => {
                setupFocusTrap();
            });
        }
    }
);

onMounted(() => {
    if (props.initialPosition.x !== null && props.initialPosition.y !== null) {
        position.value = {
            x: props.initialPosition.x,
            y: props.initialPosition.y,
        };
    } else if (props.modelValue) {
        nextTick(() => centerDialog());
    }

    window.addEventListener("resize", handleWindowResize);
});

onBeforeUnmount(() => {
    document.removeEventListener("mousemove", handleDrag);
    document.removeEventListener("touchmove", handleDrag);
    document.removeEventListener("mouseup", stopDragging);
    document.removeEventListener("touchend", stopDragging);
    document.removeEventListener("mousemove", handleResize);
    document.removeEventListener("touchmove", handleResize);
    document.removeEventListener("mouseup", stopResizing);
    document.removeEventListener("touchend", stopResizing);
    window.removeEventListener("resize", handleWindowResize);

    if (dialogRef.value) {
        dialogRef.value.removeEventListener("keydown", handleTabKey);
    }

    document.body.style.overflow = "";
});

function handleWindowResize() {
    if (!dialogRef.value || !props.modelValue) return;

    const dialogRect = dialogRef.value.getBoundingClientRect();
    const windowWidth = window.innerWidth;
    const windowHeight = window.innerHeight;

    if (dialogRect.right > windowWidth && position.value.x !== null) {
        position.value = {
            ...position.value,
            x: Math.max(0, windowWidth - dialogRect.width),
        };
    }

    if (dialogRect.bottom > windowHeight && position.value.y !== null) {
        position.value = {
            ...position.value,
            y: Math.max(0, windowHeight - dialogRect.height),
        };
    }
}
</script>

<style scoped>
.dialog {
    position: fixed;
    border-radius: 0.5rem;
    background-color: rgba(40, 40, 40, 0.95);
    color: white;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    max-width: 95vw;
    max-height: 95vh;
    min-width: 200px;
    z-index: 50;
}

.dialog.resizable {
    resize: both;
}

.dialog-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.75rem 1rem;
    background-color: rgba(40, 40, 40, 0.95);
    border-bottom: 1px solid #555;
    cursor: move;
    user-select: none;
    flex-shrink: 0;
}

.dialog-title {
    font-size: 1rem;
    font-weight: 600;
    color: white;
    margin: 0;
}

.close-btn {
    background-color: transparent;
    border: none;
    font-size: 1.5rem;
    font-weight: bold;
    color: #666;
    cursor: pointer;
    line-height: 1;
    padding: 0;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    transition:
        background-color 0.2s,
        color 0.2s;
}

.close-btn:hover {
    background-color: #f0f0f0;
    color: #e74c3c;
}

.close-btn:focus {
    outline: 2px solid #b3d8ff;
    outline-offset: 2px;
}

.dialog-content {
    padding: 1rem;
    overflow-y: auto;
    flex-grow: 1;
    color: #555;
}

.dialog-footer {
    padding: 0.75rem 1rem;
    background-color: rgba(40, 40, 40, 0.95);
    border-bottom: 1px solid #555;
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
}

.resize-handle {
    position: absolute;
    right: 0;
    bottom: 0;
    width: 16px;
    height: 16px;
    cursor: nwse-resize;
    background: linear-gradient(
        135deg,
        transparent 50%,
        #ccc 50%,
        #ccc 60%,
        transparent 60%
    );
    z-index: 10;
}

@media (max-width: 768px) {
    .dialog {
        width: 90vw !important;
        max-height: 80vh;
        top: 10vh !important;
        left: 5vw !important;
        right: 5vw !important;
        bottom: auto !important;
        transform: none !important;
    }

    .resize-handle {
        display: none;
    }
}
</style>
