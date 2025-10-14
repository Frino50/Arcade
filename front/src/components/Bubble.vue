<template>
    <canvas ref="bubbleCanvas" class="bubble-canvas"></canvas>
</template>
<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";

const bubbleCanvas = ref<HTMLCanvasElement | null>(null);

// Convertit un hex en rgba
function hexToRgba(hex: string, alpha = 1) {
    hex = hex.replace("#", "");
    const bigint = parseInt(hex, 16);
    const r = (bigint >> 16) & 255;
    const g = (bigint >> 8) & 255;
    const b = bigint & 255;
    return `rgba(${r},${g},${b},${alpha})`;
}

class Bubble {
    x: number;
    y: number;
    vx: number;
    vy: number;
    radius: number;
    color: string;
    colorLight: string;

    constructor(
        x: number,
        y: number,
        vx: number,
        vy: number,
        radius: number,
        color: string,
        colorLight: string
    ) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.color = color;
        this.colorLight = colorLight;
    }

    draw(ctx: CanvasRenderingContext2D) {
        const gradient = ctx.createRadialGradient(
            this.x - this.radius / 3,
            this.y - this.radius / 3,
            this.radius * 0.1,
            this.x,
            this.y,
            this.radius
        );
        gradient.addColorStop(0, "rgba(255,255,255,0.8)"); // reflet
        gradient.addColorStop(0.3, this.colorLight); // couleur semi-transparente
        gradient.addColorStop(1, this.color); // bord transparent

        ctx.beginPath();
        ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
        ctx.fillStyle = gradient;
        ctx.fill();

        // halo lumineux
        ctx.shadowColor = this.color;
        ctx.shadowBlur = this.radius / 2;
        ctx.fill();

        ctx.shadowBlur = 0; // reset shadow
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

    _key(x: number, y: number) {
        return `${x},${y}`;
    }

    clear() {
        this.cells.clear();
    }

    add(bubble: Bubble) {
        const gx = Math.floor(bubble.x / this.size);
        const gy = Math.floor(bubble.y / this.size);
        const key = this._key(gx, gy);
        if (!this.cells.has(key)) this.cells.set(key, []);
        this.cells.get(key)!.push(bubble);
    }

    getNeighbors(bubble: Bubble) {
        const gx = Math.floor(bubble.x / this.size);
        const gy = Math.floor(bubble.y / this.size);
        const neighbors: Bubble[] = [];
        for (let dx = -1; dx <= 1; dx++) {
            for (let dy = -1; dy <= 1; dy++) {
                const key = this._key(gx + dx, gy + dy);
                if (this.cells.has(key))
                    neighbors.push(...this.cells.get(key)!);
            }
        }
        return neighbors;
    }
}

function resolveCollision(b1: Bubble, b2: Bubble) {
    const dx = b2.x - b1.x;
    const dy = b2.y - b1.y;
    const dist = Math.sqrt(dx * dx + dy * dy);
    if (dist === 0) return;

    const nx = dx / dist;
    const ny = dy / dist;
    const tx = -ny;
    const ty = nx;

    const dpTan1 = b1.vx * tx + b1.vy * ty;
    const dpTan2 = b2.vx * tx + b2.vy * ty;

    const dpNorm1 = b1.vx * nx + b1.vy * ny;
    const m1 = b2.vx * nx + b2.vy * ny;
    const m2 = dpNorm1;

    b1.vx = tx * dpTan1 + nx * m1;
    b1.vy = ty * dpTan1 + ny * m1;
    b2.vx = tx * dpTan2 + nx * m2;
    b2.vy = ty * dpTan2 + ny * m2;

    const overlap = 0.5 * (b1.radius + b2.radius - dist);
    b1.x -= nx * overlap;
    b1.y -= ny * overlap;
    b2.x += nx * overlap;
    b2.y += ny * overlap;
}

let bubbles: Bubble[] = [];
let animationFrameId: number;

onMounted(() => {
    const canvas = bubbleCanvas.value!;
    const ctx = canvas.getContext("2d")!;

    const resizeCanvas = () => {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
    };
    window.addEventListener("resize", resizeCanvas);
    resizeCanvas();

    // récupération dynamique des couleurs du thème
    const accentHex = getComputedStyle(document.documentElement)
        .getPropertyValue("--futurist-accent")
        .trim();
    const accentLightHex = getComputedStyle(document.documentElement)
        .getPropertyValue("--futurist-accent-light")
        .trim();

    const accent = hexToRgba(accentHex, 0.2);
    const accentLight = hexToRgba(accentLightHex, 0.5);

    const numBubbles = 50;
    for (let i = 0; i < numBubbles; i++) {
        const radius = Math.random() * 20 + 10;
        const x = Math.random() * (canvas.width - 2 * radius) + radius;
        const y = Math.random() * (canvas.height - 2 * radius) + radius;
        const vx = (Math.random() - 0.5) * 2;
        const vy = (Math.random() - 0.5) * 2;
        bubbles.push(new Bubble(x, y, vx, vy, radius, accent, accentLight));
    }

    const grid = new Grid(50);

    const animate = () => {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        grid.clear();

        for (const bubble of bubbles) {
            bubble.update(canvas.width, canvas.height);
            grid.add(bubble);
        }

        for (const bubble of bubbles) {
            const neighbors = grid.getNeighbors(bubble);
            for (const other of neighbors) {
                if (other === bubble) continue;
                const dx = other.x - bubble.x;
                const dy = other.y - bubble.y;
                const dist = dx * dx + dy * dy;
                const minDist = (bubble.radius + other.radius) ** 2;
                if (dist < minDist) resolveCollision(bubble, other);
            }
            bubble.draw(ctx);
        }

        animationFrameId = requestAnimationFrame(animate);
    };

    animate();

    onBeforeUnmount(() => {
        cancelAnimationFrame(animationFrameId);
        window.removeEventListener("resize", resizeCanvas);
    });
});
</script>

<style scoped>
.bubble-canvas {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: -1;
}
</style>
