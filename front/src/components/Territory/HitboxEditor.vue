<template>
    <div class="hitbox-editor">
        <div class="editor-header">
            <h3>Édition de la Hitbox</h3>
            <button class="close-btn" @click="$emit('close')">x</button>
        </div>

        <div class="editor-body">
            <div class="canvas-container">
                <canvas
                    ref="canvasRef"
                    @mousedown="startDrag"
                    @mousemove="onDrag"
                    @mouseup="endDrag"
                    @mouseleave="endDrag"
                    class="hitbox-canvas"
                />
            </div>

            <div class="controls">
                <div class="control-group">
                    <label>X:</label>
                    <input
                        type="number"
                        v-model.number="hitbox.x"
                        @input="redraw"
                        class="input-control"
                    />
                </div>
                <div class="control-group">
                    <label>Y:</label>
                    <input
                        type="number"
                        v-model.number="hitbox.y"
                        @input="redraw"
                        class="input-control"
                    />
                </div>
                <div class="control-group">
                    <label>Largeur:</label>
                    <input
                        type="number"
                        v-model.number="hitbox.width"
                        @input="redraw"
                        class="input-control"
                    />
                </div>
                <div class="control-group">
                    <label>Hauteur:</label>
                    <input
                        type="number"
                        v-model.number="hitbox.height"
                        @input="redraw"
                        class="input-control"
                    />
                </div>

                <div class="action-buttons">
                    <button class="btn-save" @click="saveHitbox">
                        Sauvegarder
                    </button>
                    <button
                        class="btn-delete"
                        @click="deleteHitbox"
                        v-if="hasHitbox"
                    >
                        Supprimer
                    </button>
                    <button class="btn-reset" @click="resetHitbox">
                        Réinitialiser
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from "vue";
import type SpriteInfo from "@/models/SpriteInfos.ts";
import type { Hitbox } from "@/models/SpriteInfos.ts";
import spriteService from "@/services/spriteService.ts";

const props = defineProps<{
    sprite: SpriteInfo;
}>();

const emit = defineEmits(["close", "saved"]);

const canvasRef = ref<HTMLCanvasElement | null>(null);
const hitbox = ref<Hitbox>({
    x: props.sprite.hitboxX ?? 0,
    y: props.sprite.hitboxY ?? 0,
    width: props.sprite.hitboxWidth ?? 50,
    height: props.sprite.hitboxHeight ?? 50,
});

const isDragging = ref(false);
const dragMode = ref<"move" | "resize" | null>(null);
const dragCorner = ref<"tl" | "tr" | "bl" | "br" | null>(null);
const dragStart = ref({ x: 0, y: 0 });
const initialHitbox = ref<Hitbox>({ ...hitbox.value });

const hasHitbox = computed(() => {
    return props.sprite.hitboxX !== undefined && props.sprite.hitboxX !== null;
});

let spriteImage: HTMLImageElement | null = null;
const frameWidth = computed(() => props.sprite.width / props.sprite.frames);

watch(() => props.sprite, loadSprite, { immediate: true });

async function loadSprite() {
    const blobUrl = await spriteService.getImage(props.sprite.imageUrl);
    spriteImage = new Image();
    spriteImage.onload = () => redraw();
    spriteImage.src = blobUrl;
}

onMounted(() => {
    if (canvasRef.value) {
        canvasRef.value.width = frameWidth.value * 2;
        canvasRef.value.height = props.sprite.height * 2;
    }
});

function redraw() {
    if (!canvasRef.value || !spriteImage) return;

    const ctx = canvasRef.value.getContext("2d");
    if (!ctx) return;

    const scale = 2;
    ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height);

    ctx.imageSmoothingEnabled = false;
    ctx.drawImage(
        spriteImage,
        0,
        0,
        frameWidth.value,
        props.sprite.height,
        0,
        0,
        frameWidth.value * scale,
        props.sprite.height * scale
    );

    ctx.strokeStyle = "#ff0000";
    ctx.lineWidth = 2;
    ctx.strokeRect(
        hitbox.value.x * scale,
        hitbox.value.y * scale,
        hitbox.value.width * scale,
        hitbox.value.height * scale
    );

    ctx.fillStyle = "rgba(255, 0, 0, 0.2)";
    ctx.fillRect(
        hitbox.value.x * scale,
        hitbox.value.y * scale,
        hitbox.value.width * scale,
        hitbox.value.height * scale
    );

    drawHandle(ctx, hitbox.value.x * scale, hitbox.value.y * scale);
    drawHandle(
        ctx,
        (hitbox.value.x + hitbox.value.width) * scale,
        hitbox.value.y * scale
    );
    drawHandle(
        ctx,
        hitbox.value.x * scale,
        (hitbox.value.y + hitbox.value.height) * scale
    );
    drawHandle(
        ctx,
        (hitbox.value.x + hitbox.value.width) * scale,
        (hitbox.value.y + hitbox.value.height) * scale
    );
}

function drawHandle(ctx: CanvasRenderingContext2D, x: number, y: number) {
    ctx.fillStyle = "#ffffff";
    ctx.strokeStyle = "#ff0000";
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(x, y, 5, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();
}

function startDrag(e: MouseEvent) {
    if (!canvasRef.value) return;

    const rect = canvasRef.value.getBoundingClientRect();
    const x = (e.clientX - rect.left) / 2;
    const y = (e.clientY - rect.top) / 2;

    const handleSize = 5;
    const corners = [
        { x: hitbox.value.x, y: hitbox.value.y, corner: "tl" },
        {
            x: hitbox.value.x + hitbox.value.width,
            y: hitbox.value.y,
            corner: "tr",
        },
        {
            x: hitbox.value.x,
            y: hitbox.value.y + hitbox.value.height,
            corner: "bl",
        },
        {
            x: hitbox.value.x + hitbox.value.width,
            y: hitbox.value.y + hitbox.value.height,
            corner: "br",
        },
    ];

    for (const corner of corners) {
        if (
            Math.abs(x - corner.x) <= handleSize &&
            Math.abs(y - corner.y) <= handleSize
        ) {
            isDragging.value = true;
            dragMode.value = "resize";
            dragCorner.value = corner.corner as typeof dragCorner.value;
            dragStart.value = { x, y };
            initialHitbox.value = { ...hitbox.value };
            if (corner.corner === "tl" || corner.corner === "br") {
                canvasRef.value.style.cursor = "nwse-resize";
            } else {
                canvasRef.value.style.cursor = "nesw-resize";
            }
            return;
        }
    }

    if (
        x >= hitbox.value.x &&
        x <= hitbox.value.x + hitbox.value.width &&
        y >= hitbox.value.y &&
        y <= hitbox.value.y + hitbox.value.height
    ) {
        isDragging.value = true;
        dragMode.value = "move";
        dragStart.value = { x, y };
        initialHitbox.value = { ...hitbox.value };
        canvasRef.value.style.cursor = "move";
    }
}

function onDrag(e: MouseEvent) {
    if (!isDragging.value || !canvasRef.value) return;

    const rect = canvasRef.value.getBoundingClientRect();
    const x = (e.clientX - rect.left) / 2;
    const y = (e.clientY - rect.top) / 2;

    if (dragMode.value === "move") {
        const dx = x - dragStart.value.x;
        const dy = y - dragStart.value.y;

        hitbox.value.x = Math.max(
            0,
            Math.min(
                frameWidth.value - hitbox.value.width,
                initialHitbox.value.x + dx
            )
        );
        hitbox.value.y = Math.max(
            0,
            Math.min(
                props.sprite.height - hitbox.value.height,
                initialHitbox.value.y + dy
            )
        );
    } else if (dragMode.value === "resize") {
        const dx = x - dragStart.value.x;
        const dy = y - dragStart.value.y;
        const minSize = 10;

        // Start from initial values
        let newX = initialHitbox.value.x;
        let newY = initialHitbox.value.y;
        let newW = initialHitbox.value.width;
        let newH = initialHitbox.value.height;

        switch (dragCorner.value) {
            case "br":
                newW = initialHitbox.value.width + dx;
                newH = initialHitbox.value.height + dy;
                break;
            case "tr":
                newW = initialHitbox.value.width + dx;
                newY = initialHitbox.value.y + dy;
                newH = initialHitbox.value.height - dy;
                break;
            case "tl":
                newX = initialHitbox.value.x + dx;
                newW = initialHitbox.value.width - dx;
                newY = initialHitbox.value.y + dy;
                newH = initialHitbox.value.height - dy;
                break;
            case "bl":
                newX = initialHitbox.value.x + dx;
                newW = initialHitbox.value.width - dx;
                newH = initialHitbox.value.height + dy;
                break;
        }

        // Constrain minimum sizes
        if (newW < minSize) {
            if (dragCorner.value === "tl" || dragCorner.value === "bl") {
                newX -= minSize - newW;
            }
            newW = minSize;
        }
        if (newH < minSize) {
            if (dragCorner.value === "tl" || dragCorner.value === "tr") {
                newY -= minSize - newH;
            }
            newH = minSize;
        }

        if (newX < 0) {
            if (dragCorner.value === "tl" || dragCorner.value === "bl") {
                newW -= 0 - newX;
            }
            newX = 0;
        }
        if (newY < 0) {
            if (dragCorner.value === "tl" || dragCorner.value === "tr") {
                newH -= 0 - newY;
            }
            newY = 0;
        }
        const maxRight = frameWidth.value;
        const right = newX + newW;
        if (right > maxRight) {
            if (dragCorner.value === "tr" || dragCorner.value === "br") {
                newW -= right - maxRight;
            } else {
                const overflow = right - maxRight;
                newX -= overflow;
            }
        }
        const maxBottom = props.sprite.height;
        const bottom = newY + newH;
        if (bottom > maxBottom) {
            if (dragCorner.value === "bl" || dragCorner.value === "br") {
                newH -= bottom - maxBottom;
            } else {
                const overflow = bottom - maxBottom;
                newY -= overflow;
            }
        }

        newW = Math.max(minSize, Math.min(newW, maxRight - newX));
        newH = Math.max(minSize, Math.min(newH, maxBottom - newY));

        hitbox.value.x = newX;
        hitbox.value.y = newY;
        hitbox.value.width = newW;
        hitbox.value.height = newH;
    }

    redraw();
}

function endDrag() {
    isDragging.value = false;
    dragMode.value = null;
    dragCorner.value = null;
    if (canvasRef.value) {
        canvasRef.value.style.cursor = "default";
    }
}

async function saveHitbox() {
    await spriteService.saveHitbox(props.sprite.animationId, hitbox.value);
    emit("saved", hitbox.value);
    emit("close");
}

async function deleteHitbox() {
    await spriteService.deleteHitbox(props.sprite.animationId);
    hitbox.value = { x: 0, y: 0, width: 50, height: 50 };
    emit("saved", null);
    emit("close");
}

function resetHitbox() {
    hitbox.value = {
        x: props.sprite.hitboxX ?? 0,
        y: props.sprite.hitboxY ?? 0,
        width: props.sprite.hitboxWidth ?? 50,
        height: props.sprite.hitboxHeight ?? 50,
    };
    redraw();
}
</script>

<style scoped>
.hitbox-editor {
    background: #1e293b;
    border-radius: 12px;
    border: 1px solid #334155;
    overflow: hidden;
    max-width: 600px;
    margin: 0 auto;
}

.editor-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem 1.5rem;
    background: #0f172a;
    border-bottom: 1px solid #334155;
}

.editor-header h3 {
    margin: 0;
    color: #e2e8f0;
}

.close-btn {
    background: transparent;
    border: none;
    color: #94a3b8;
    font-size: 2rem;
    line-height: 1;
    cursor: pointer;
    transition: color 0.2s;
    padding: 0;
    box-shadow: none;
}
.close-btn:hover {
    color: #fff;
}

.editor-body {
    padding: 1.5rem;
}

.canvas-container {
    background: #0f172a;
    border: 2px solid #334155;
    border-radius: 8px;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 1rem;
    margin-bottom: 1.5rem;
}

.hitbox-canvas {
    image-rendering: pixelated;
    cursor: default;
}

.controls {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.control-group {
    display: grid;
    grid-template-columns: 100px 1fr;
    align-items: center;
    gap: 0.5rem;
}

.control-group label {
    color: #94a3b8;
    font-weight: 600;
}

.input-control {
    background: #0f172a;
    border: 1px solid #334155;
    color: white;
    padding: 0.5rem;
    border-radius: 6px;
    font-size: 1rem;
}

.input-control:focus {
    outline: none;
    border-color: #3b82f6;
}

.action-buttons {
    display: flex;
    gap: 0.5rem;
    margin-top: 1rem;
}

.action-buttons button {
    flex: 1;
    padding: 0.75rem;
    border: none;
    border-radius: 6px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
}

.btn-save {
    background: #059669;
    color: white;
}

.btn-save:hover {
    background: #10b981;
}

.btn-delete {
    background: #be123c;
    color: white;
}

.btn-delete:hover {
    background: #e11d48;
}

.btn-reset {
    background: #475569;
    color: white;
}

.btn-reset:hover {
    background: #64748b;
}
</style>
