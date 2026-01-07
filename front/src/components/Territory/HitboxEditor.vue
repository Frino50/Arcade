<template>
    <div class="hitbox-editor-overlay" @click.self="$emit('close')">
        <div class="hitbox-editor">
            <header class="editor-header">
                <div class="header-title">
                    <span class="icon">🎯</span>
                    <h3>Éditeur de Hitbox</h3>
                </div>
                <div class="header-actions">
                    <span class="zoom-info">{{ Math.round(zoom * 100) }}%</span>
                    <button
                        class="close-btn"
                        @click="$emit('close')"
                        title="Fermer"
                    >
                        ✕
                    </button>
                </div>
            </header>

            <div class="editor-layout">
                <div class="canvas-wrapper" @wheel.prevent="handleWheel">
                    <div class="canvas-scroller">
                        <canvas
                            ref="canvasRef"
                            @mousedown="startDrag"
                            @mousemove="onMouseMove"
                            @mouseup="endDrag"
                            @mouseleave="endDrag"
                            class="hitbox-canvas"
                        />
                    </div>
                    <div class="canvas-toolbar">
                        <button @click="zoomOut" title="Zoom Arrière">-</button>
                        <button @click="resetZoom" title="Réinitialiser Zoom">
                            1:1
                        </button>
                        <button @click="zoomIn" title="Zoom Avant">+</button>
                    </div>
                </div>

                <aside class="sidebar">
                    <div class="form-section">
                        <h4>Position</h4>
                        <div class="input-row">
                            <div class="input-group">
                                <label>X</label>
                                <input
                                    type="number"
                                    v-model.number="hitbox.x"
                                    @input="requestDraw"
                                />
                            </div>
                            <div class="input-group">
                                <label>Y</label>
                                <input
                                    type="number"
                                    v-model.number="hitbox.y"
                                    @input="requestDraw"
                                />
                            </div>
                        </div>
                    </div>

                    <div class="form-section">
                        <h4>Dimensions</h4>
                        <div class="input-row">
                            <div class="input-group">
                                <label>Largeur</label>
                                <input
                                    type="number"
                                    v-model.number="hitbox.width"
                                    @input="requestDraw"
                                />
                            </div>
                            <div class="input-group">
                                <label>Hauteur</label>
                                <input
                                    type="number"
                                    v-model.number="hitbox.height"
                                    @input="requestDraw"
                                />
                            </div>
                        </div>
                    </div>

                    <div class="info-box">
                        <small
                            >💡 Utilisez les flèches du clavier pour déplacer la
                            sélection au pixel près.</small
                        >
                    </div>

                    <div class="sidebar-footer">
                        <button class="btn btn-primary" @click="saveHitbox">
                            Sauvegarder
                        </button>
                        <div class="btn-row">
                            <button
                                class="btn btn-secondary"
                                @click="resetHitbox"
                            >
                                Reset
                            </button>
                            <button
                                class="btn btn-danger"
                                @click="deleteHitbox"
                                v-if="hasHitbox"
                            >
                                Supprimer
                            </button>
                        </div>
                    </div>
                </aside>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, onUnmounted } from "vue";
import type SpriteInfo from "@/models/SpriteInfos.ts";
import type { Hitbox } from "@/models/SpriteInfos.ts";
import spriteService from "@/services/spriteService.ts";

const props = defineProps<{
    sprite: SpriteInfo;
}>();

const emit = defineEmits(["close", "saved"]);

// --- Refs & State ---
const canvasRef = ref<HTMLCanvasElement | null>(null);
const zoom = ref(2); // Zoom par défaut
const VIEW_PADDING = 60; // Espace autour du sprite

const hitbox = ref<Hitbox>({
    x: props.sprite.hitboxX ?? 0,
    y: props.sprite.hitboxY ?? 0,
    width: props.sprite.hitboxWidth ?? 32,
    height: props.sprite.hitboxHeight ?? 32,
});

// Dragging Logic
const isDragging = ref(false);
const dragMode = ref<"move" | "resize" | null>(null);
const dragCorner = ref<string | null>(null);
const dragStart = ref({ x: 0, y: 0 }); // Mouse pos at start
const initialHitbox = ref<Hitbox>({ ...hitbox.value }); // Hitbox state at start

// Asset Loading
let spriteImage: HTMLImageElement | null = null;
const frameWidth = computed(() => props.sprite.width / props.sprite.frames);
const hasHitbox = computed(
    () => props.sprite.hitboxX !== undefined && props.sprite.hitboxX !== null
);

// --- Lifecycle & Watchers ---

watch(() => props.sprite, loadSprite, { immediate: true });

// Écouteur clavier pour les flèches
onMounted(() => {
    window.addEventListener("keydown", handleKeyDown);
    updateCanvasSize();
});

onUnmounted(() => {
    window.removeEventListener("keydown", handleKeyDown);
});

// Quand le zoom change, on redimensionne et on redessine
watch(zoom, () => {
    updateCanvasSize();
    requestDraw();
});

async function loadSprite() {
    try {
        const blobUrl = await spriteService.getImage(props.sprite.imageUrl);
        spriteImage = new Image();
        spriteImage.onload = () => {
            updateCanvasSize();
            requestDraw();
        };
        spriteImage.src = blobUrl;
    } catch (e) {
        console.error("Erreur chargement sprite", e);
    }
}

// --- Drawing Logic ---

function updateCanvasSize() {
    if (canvasRef.value) {
        // La taille logique du canvas inclut le padding + la frame
        const logicalWidth = frameWidth.value + VIEW_PADDING * 2;
        const logicalHeight = props.sprite.height + VIEW_PADDING * 2;

        // La taille d'affichage dépend du zoom
        canvasRef.value.width = logicalWidth * zoom.value;
        canvasRef.value.height = logicalHeight * zoom.value;

        // CSS Style pour la netteté
        canvasRef.value.style.width = `${logicalWidth * zoom.value}px`;
        canvasRef.value.style.height = `${logicalHeight * zoom.value}px`;
    }
}

function requestDraw() {
    requestAnimationFrame(draw);
}

function draw() {
    if (!canvasRef.value || !spriteImage) return;
    const ctx = canvasRef.value.getContext("2d");
    if (!ctx) return;

    // Reset transform & clear
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height);

    // Paramètres graphiques
    ctx.imageSmoothingEnabled = false;

    // --- 1. Dessiner le fond (Checkerboard) ---
    drawCheckerboard(ctx);

    // --- 2. Transformation de vue ---
    // On déplace l'origine pour centrer le sprite dans le padding, avec le zoom
    ctx.translate(VIEW_PADDING * zoom.value, VIEW_PADDING * zoom.value);

    // --- 3. Dessiner le Sprite (Frame 1) ---
    ctx.drawImage(
        spriteImage,
        0,
        0,
        frameWidth.value,
        props.sprite.height, // Source
        0,
        0,
        frameWidth.value * zoom.value,
        props.sprite.height * zoom.value // Destination
    );

    // Bordure du sprite (repère visuel)
    ctx.strokeStyle = "rgba(148, 163, 184, 0.5)"; // Slate-400
    ctx.lineWidth = 1;
    ctx.strokeRect(
        0,
        0,
        frameWidth.value * zoom.value,
        props.sprite.height * zoom.value
    );

    // --- 4. Dessiner la Hitbox ---
    const hx = hitbox.value.x * zoom.value;
    const hy = hitbox.value.y * zoom.value;
    const hw = hitbox.value.width * zoom.value;
    const hh = hitbox.value.height * zoom.value;

    // Remplissage semi-transparent
    ctx.fillStyle = "rgba(239, 68, 68, 0.25)"; // Red-500
    ctx.fillRect(hx, hy, hw, hh);

    // Contour
    ctx.strokeStyle = "#ef4444"; // Red-500
    ctx.lineWidth = 2;
    ctx.strokeRect(hx, hy, hw, hh);

    // --- 5. Dessiner les Poignées (Handles) ---
    drawHandles(ctx, hx, hy, hw, hh);
}

function drawCheckerboard(ctx: CanvasRenderingContext2D) {
    const s = 10 * zoom.value; // Taille carreau
    const w = canvasRef.value!.width;
    const h = canvasRef.value!.height;

    ctx.fillStyle = "#1e293b"; // Dark Slate (background)
    ctx.fillRect(0, 0, w, h);

    ctx.fillStyle = "#334155"; // Lighter Slate (checker)
    for (let x = 0; x < w; x += s) {
        for (let y = 0; y < h; y += s) {
            if ((Math.floor(x / s) + Math.floor(y / s)) % 2 === 0) {
                ctx.fillRect(x, y, s, s);
            }
        }
    }
}

function drawHandles(
    ctx: CanvasRenderingContext2D,
    x: number,
    y: number,
    w: number,
    h: number
) {
    const handleRadius = 4;
    ctx.fillStyle = "#ffffff";
    ctx.strokeStyle = "#ef4444";
    ctx.lineWidth = 2;

    const coords = [
        { x: x, y: y }, // TL
        { x: x + w, y: y }, // TR
        { x: x, y: y + h }, // BL
        { x: x + w, y: y + h }, // BR
    ];

    coords.forEach((p) => {
        ctx.beginPath();
        ctx.arc(p.x, p.y, handleRadius, 0, Math.PI * 2);
        ctx.fill();
        ctx.stroke();
    });
}

// --- Interaction Logic ---

// Convertit Event Mouse (Screen) -> Sprite Coords (Logique)
function getMousePos(e: MouseEvent) {
    if (!canvasRef.value) return { x: 0, y: 0 };
    const rect = canvasRef.value.getBoundingClientRect();
    // Position relative au canvas (0,0 en haut a gauche du canvas HTML)
    const clientX = e.clientX - rect.left;
    const clientY = e.clientY - rect.top;

    // Conversion en coordonnées Sprite
    // On divise par le zoom et on retire le padding
    const spriteX = clientX / zoom.value - VIEW_PADDING;
    const spriteY = clientY / zoom.value - VIEW_PADDING;

    return { x: spriteX, y: spriteY };
}

function getHandleAt(mx: number, my: number): string | null {
    // Tolérance de détection ajustée au zoom (plus facile à cliquer dézoomé)
    const tolerance = 6 / Math.min(1, zoom.value);
    const { x, y, width: w, height: h } = hitbox.value;

    // Check corners
    if (dist(mx, my, x, y) <= tolerance) return "tl";
    if (dist(mx, my, x + w, y) <= tolerance) return "tr";
    if (dist(mx, my, x, y + h) <= tolerance) return "bl";
    if (dist(mx, my, x + w, y + h) <= tolerance) return "br";

    // Check edges
    const inX = mx >= x && mx <= x + w;
    const inY = my >= y && my <= y + h;

    if (inY && Math.abs(mx - x) <= tolerance) return "l";
    if (inY && Math.abs(mx - (x + w)) <= tolerance) return "r";
    if (inX && Math.abs(my - y) <= tolerance) return "t";
    if (inX && Math.abs(my - (y + h)) <= tolerance) return "b";

    return null;
}

function dist(x1: number, y1: number, x2: number, y2: number) {
    return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
}

function onMouseMove(e: MouseEvent) {
    const { x, y } = getMousePos(e);

    if (isDragging.value) {
        handleDrag(x, y);
    } else {
        updateCursor(x, y);
    }
}

function startDrag(e: MouseEvent) {
    const { x, y } = getMousePos(e);
    const handle = getHandleAt(x, y);

    initialHitbox.value = { ...hitbox.value };
    dragStart.value = { x, y };

    if (handle) {
        isDragging.value = true;
        dragMode.value = "resize";
        dragCorner.value = handle;
    } else if (
        x >= hitbox.value.x &&
        x <= hitbox.value.x + hitbox.value.width &&
        y >= hitbox.value.y &&
        y <= hitbox.value.y + hitbox.value.height
    ) {
        isDragging.value = true;
        dragMode.value = "move";
    }
}

function handleDrag(currX: number, currY: number) {
    const dx = Math.round(currX - dragStart.value.x);
    const dy = Math.round(currY - dragStart.value.y);

    if (dragMode.value === "move") {
        hitbox.value.x = initialHitbox.value.x + dx;
        hitbox.value.y = initialHitbox.value.y + dy;
    } else if (dragMode.value === "resize") {
        // Logique de redimensionnement identique, mais simplifiée
        let { x, y, width: w, height: h } = initialHitbox.value;
        const c = dragCorner.value;

        if (c?.includes("r")) w += dx;
        if (c?.includes("l")) {
            x += dx;
            w -= dx;
        }
        if (c?.includes("b")) h += dy;
        if (c?.includes("t")) {
            y += dy;
            h -= dy;
        }

        // Minimum Constraints
        if (w < 1) {
            w = 1;
            if (c?.includes("l"))
                x = initialHitbox.value.x + initialHitbox.value.width - 1;
        }
        if (h < 1) {
            h = 1;
            if (c?.includes("t"))
                y = initialHitbox.value.y + initialHitbox.value.height - 1;
        }

        hitbox.value = { x, y, width: w, height: h };
    }
    requestDraw();
}

function endDrag() {
    isDragging.value = false;
    dragMode.value = null;
    dragCorner.value = null;
    // On force l'arrondi entier à la fin du drag
    hitbox.value.x = Math.round(hitbox.value.x);
    hitbox.value.y = Math.round(hitbox.value.y);
    hitbox.value.width = Math.round(hitbox.value.width);
    hitbox.value.height = Math.round(hitbox.value.height);
    requestDraw();
}

function updateCursor(x: number, y: number) {
    if (!canvasRef.value) return;
    const handle = getHandleAt(x, y);
    if (handle) {
        const cursorMap: Record<string, string> = {
            tl: "nwse-resize",
            br: "nwse-resize",
            tr: "nesw-resize",
            bl: "nesw-resize",
            t: "ns-resize",
            b: "ns-resize",
            l: "ew-resize",
            r: "ew-resize",
        };
        canvasRef.value.style.cursor = cursorMap[handle];
    } else if (
        x >= hitbox.value.x &&
        x <= hitbox.value.x + hitbox.value.width &&
        y >= hitbox.value.y &&
        y <= hitbox.value.y + hitbox.value.height
    ) {
        canvasRef.value.style.cursor = "move";
    } else {
        canvasRef.value.style.cursor = "default";
    }
}

// --- Zoom & Keyboard Controls ---

function handleWheel(e: WheelEvent) {
    if (e.ctrlKey || e.metaKey) {
        if (e.deltaY < 0) zoomIn();
        else zoomOut();
    }
}

function zoomIn() {
    zoom.value = Math.min(zoom.value + 0.5, 10);
}
function zoomOut() {
    zoom.value = Math.max(zoom.value - 0.5, 0.5);
}
function resetZoom() {
    zoom.value = 2;
}

function handleKeyDown(e: KeyboardEvent) {
    const step = e.shiftKey ? 10 : 1;
    switch (e.key) {
        case "ArrowUp":
            hitbox.value.y -= step;
            break;
        case "ArrowDown":
            hitbox.value.y += step;
            break;
        case "ArrowLeft":
            hitbox.value.x -= step;
            break;
        case "ArrowRight":
            hitbox.value.x += step;
            break;
        default:
            return;
    }
    requestDraw();
}

// --- Actions ---

async function saveHitbox() {
    await spriteService.saveHitbox(props.sprite.animationId, hitbox.value);
    emit("saved", hitbox.value);
    emit("close");
}

async function deleteHitbox() {
    if (!confirm("Voulez-vous vraiment supprimer la hitbox ?")) return;
    await spriteService.deleteHitbox(props.sprite.animationId);
    emit("saved", null);
    emit("close");
}

function resetHitbox() {
    hitbox.value = {
        x: props.sprite.hitboxX ?? 0,
        y: props.sprite.hitboxY ?? 0,
        width: props.sprite.hitboxWidth ?? 32,
        height: props.sprite.hitboxHeight ?? 32,
    };
    requestDraw();
}
</script>

<style scoped>
/* Reset box-sizing */
*,
*::before,
*::after {
    box-sizing: border-box;
}

.hitbox-editor-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.7);
    backdrop-filter: blur(4px);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
}

.hitbox-editor {
    background: #0f172a; /* Slate 900 */
    border: 1px solid #334155;
    border-radius: 16px;
    width: 90vw;
    max-width: 1100px;
    height: 85vh;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
    color: #f8fafc;
}

/* HEADER */
.editor-header {
    background: #1e293b;
    padding: 1rem 1.5rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #334155;
}

.header-title {
    display: flex;
    align-items: center;
    gap: 0.75rem;
}

.header-title h3 {
    margin: 0;
    font-size: 1.25rem;
    font-weight: 600;
}

.header-actions {
    display: flex;
    align-items: center;
    gap: 1rem;
}

.zoom-info {
    font-family: monospace;
    background: #0f172a;
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
    color: #94a3b8;
    font-size: 0.875rem;
}

.close-btn {
    background: transparent;
    border: none;
    color: #94a3b8;
    font-size: 1.5rem;
    cursor: pointer;
    transition: color 0.2s;
    line-height: 1;
}
.close-btn:hover {
    color: #fff;
}

/* LAYOUT */
.editor-layout {
    display: grid;
    grid-template-columns: 1fr 320px;
    flex: 1;
    overflow: hidden;
}

/* CANVAS AREA */
.canvas-wrapper {
    position: relative;
    background: #020617; /* Très sombre */
    overflow: hidden;
    display: flex;
    flex-direction: column;
}

.canvas-scroller {
    flex: 1;
    overflow: auto;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 2rem;
    /* Custom Scrollbar */
    scrollbar-width: thin;
    scrollbar-color: #475569 #0f172a;
}

.hitbox-canvas {
    box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
    border: 1px solid #334155;
}

.canvas-toolbar {
    position: absolute;
    bottom: 1.5rem;
    left: 50%;
    transform: translateX(-50%);
    background: #1e293b;
    border: 1px solid #475569;
    border-radius: 999px;
    padding: 0.25rem;
    display: flex;
    gap: 0.25rem;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.3);
}

.canvas-toolbar button {
    background: transparent;
    border: none;
    color: #e2e8f0;
    padding: 0.5rem 1rem;
    cursor: pointer;
    font-weight: 600;
    border-radius: 999px;
    transition: background 0.2s;
}

.canvas-toolbar button:hover {
    background: #334155;
}

/* SIDEBAR */
.sidebar {
    background: #1e293b;
    border-left: 1px solid #334155;
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 2rem;
    overflow-y: auto;
}

.form-section h4 {
    color: #94a3b8;
    text-transform: uppercase;
    font-size: 0.75rem;
    letter-spacing: 0.05em;
    margin: 0 0 1rem 0;
    font-weight: 700;
}

.input-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
}

.input-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.input-group label {
    font-size: 0.875rem;
    color: #cbd5e1;
}

.input-group input {
    background: #0f172a;
    border: 1px solid #334155;
    color: white;
    padding: 0.6rem;
    border-radius: 6px;
    font-size: 1rem;
    font-family: monospace;
    transition: border-color 0.2s;
}

.input-group input:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.info-box {
    background: #334155;
    padding: 1rem;
    border-radius: 8px;
    color: #cbd5e1;
    font-size: 0.875rem;
    line-height: 1.5;
}

.sidebar-footer {
    margin-top: auto;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.btn-row {
    display: flex;
    gap: 0.75rem;
}

.btn {
    padding: 0.75rem 1rem;
    border: none;
    border-radius: 8px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    font-size: 0.95rem;
}

.btn-primary {
    background: #3b82f6;
    color: white;
    width: 100%;
}
.btn-primary:hover {
    background: #2563eb;
}

.btn-secondary {
    background: #475569;
    color: white;
    flex: 1;
}
.btn-secondary:hover {
    background: #64748b;
}

.btn-danger {
    background: rgba(239, 68, 68, 0.2);
    color: #ef4444;
    flex: 1;
}
.btn-danger:hover {
    background: rgba(239, 68, 68, 0.3);
}
</style>
