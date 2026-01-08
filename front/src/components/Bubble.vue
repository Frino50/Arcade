<template>
    <canvas ref="bubbleCanvas" class="bubble-canvas"></canvas>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue";

const bubbleCanvas = ref<HTMLCanvasElement | null>(null);

function hexToRgba(hex: string, alpha = 1) {
    hex = hex.replace("#", "");
    const big = parseInt(hex, 16);
    const r = (big >> 16) & 255;
    const g = (big >> 8) & 255;
    const b = big & 255;
    return `rgba(${r},${g},${b},${alpha})`;
}

class Bubble {
    x: number;
    y: number;
    vx: number;
    vy: number;
    radius: number;

    constructor(x: number, y: number, vx: number, vy: number, radius: number) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
    }

    update(width: number, height: number) {
        this.x += this.vx;
        this.y += this.vy;

        if (this.x - this.radius < 0 || this.x + this.radius > width)
            this.vx *= -1;
        if (this.y - this.radius < 0 || this.y + this.radius > height)
            this.vy *= -1;
    }
}

class Grid {
    size: number;
    cells: Map<string, Bubble[]>;

    constructor(size: number) {
        this.size = size;
        this.cells = new Map();
    }

    key(x: number, y: number) {
        return `${x},${y}`;
    }

    clear() {
        this.cells.clear();
    }

    add(b: Bubble) {
        const gx = Math.floor(b.x / this.size);
        const gy = Math.floor(b.y / this.size);

        const k = this.key(gx, gy);
        if (!this.cells.has(k)) this.cells.set(k, []);
        this.cells.get(k)!.push(b);
    }

    neighbors(b: Bubble) {
        const gx = Math.floor(b.x / this.size);
        const gy = Math.floor(b.y / this.size);

        const list: Bubble[] = [];

        for (let dx = -1; dx <= 1; dx++) {
            for (let dy = -1; dy <= 1; dy++) {
                const k = this.key(gx + dx, gy + dy);
                if (this.cells.has(k)) {
                    list.push(...this.cells.get(k)!);
                }
            }
        }
        return list;
    }
}

function resolveCollision(b1: Bubble, b2: Bubble) {
    const dx = b2.x - b1.x;
    const dy = b2.y - b1.y;
    const dist = Math.sqrt(dx * dx + dy * dy);
    const minDist = b1.radius + b2.radius;

    if (dist === 0 || dist >= minDist) return;

    const nx = dx / dist;
    const ny = dy / dist;
    const tx = -ny;
    const ty = nx;

    const dpTan1 = b1.vx * tx + b1.vy * ty;
    const dpTan2 = b2.vx * tx + b2.vy * ty;

    const dpNorm1 = b1.vx * nx + b1.vy * ny;
    const dpNorm2 = b2.vx * nx + b2.vy * ny;

    b1.vx = tx * dpTan1 + nx * dpNorm2;
    b1.vy = ty * dpTan1 + ny * dpNorm2;
    b2.vx = tx * dpTan2 + nx * dpNorm1;
    b2.vy = ty * dpTan2 + ny * dpNorm1;

    const overlap = 0.5 * (minDist - dist);
    b1.x -= nx * overlap;
    b1.y -= ny * overlap;
    b2.x += nx * overlap;
    b2.y += ny * overlap;
}

function createSprite(radius: number, color: string, colorLight: string) {
    const shadow = radius / 2;

    const size = radius * 2 + shadow * 2;

    const canvas = document.createElement("canvas");
    canvas.width = canvas.height = size;

    const ctx = canvas.getContext("2d")!;

    const cx = size / 2;
    const cy = size / 2;

    const gradient = ctx.createRadialGradient(
        cx - radius / 3,
        cy - radius / 3,
        radius * 0.1,
        cx,
        cy,
        radius
    );

    gradient.addColorStop(0, "rgba(255,255,255,0.8)");
    gradient.addColorStop(0.3, colorLight);
    gradient.addColorStop(1, color);

    ctx.fillStyle = gradient;

    ctx.shadowColor = color;
    ctx.shadowBlur = shadow;

    ctx.beginPath();
    ctx.arc(cx, cy, radius, 0, Math.PI * 2);
    ctx.fill();

    return canvas;
}

let bubbles: Bubble[] = [];
let sprites: HTMLCanvasElement[] = [];
let animationFrameId: number;
let observer: MutationObserver | null = null;

onMounted(() => {
    const canvas = bubbleCanvas.value!;
    const ctx = canvas.getContext("2d")!;

    const resize = () => {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
    };
    window.addEventListener("resize", resize);
    resize();

    function regenerateSprites() {
        const accentHex = getComputedStyle(document.documentElement)
            .getPropertyValue("--futurist-accent")
            .trim();

        const accentLightHex = getComputedStyle(document.documentElement)
            .getPropertyValue("--futurist-accent-light")
            .trim();

        const color = hexToRgba(accentHex, 0.2);
        const colorLight = hexToRgba(accentLightHex, 0.5);

        sprites = bubbles.map((b) => createSprite(b.radius, color, colorLight));
    }

    const num = 50;
    for (let i = 0; i < num; i++) {
        const radius = Math.random() * 20 + 10;
        const x = Math.random() * (canvas.width - radius * 2) + radius;
        const y = Math.random() * (canvas.height - radius * 2) + radius;
        const vx = (Math.random() - 0.5) * 2;
        const vy = (Math.random() - 0.5) * 2;

        bubbles.push(new Bubble(x, y, vx, vy, radius));
    }

    regenerateSprites();

    observer = new MutationObserver(() => regenerateSprites());
    observer.observe(document.documentElement, {
        attributes: true,
        attributeFilter: ["class"],
    });

    const grid = new Grid(60);

    const animate = () => {
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        grid.clear();
        for (const b of bubbles) grid.add(b);

        for (const b of bubbles) {
            b.update(canvas.width, canvas.height);

            const neighbors = grid.neighbors(b);
            for (const other of neighbors) {
                if (b !== other) resolveCollision(b, other);
            }

            const sprite = sprites[bubbles.indexOf(b)];
            ctx.drawImage(
                sprite,
                b.x - sprite.width / 2,
                b.y - sprite.height / 2,
                sprite.width,
                sprite.height
            );
        }

        animationFrameId = requestAnimationFrame(animate);
    };

    animate();

    onBeforeUnmount(() => {
        cancelAnimationFrame(animationFrameId);
        window.removeEventListener("resize", resize);
        observer?.disconnect();
    });
});
</script>

<style scoped>
.bubble-canvas {
    position: fixed;
    inset: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: -1;
}
</style>
