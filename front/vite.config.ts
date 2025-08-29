import { resolve } from "path";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
    server: {
        port: 5555,
    },
    resolve: {
        alias: {
            "@": resolve(__dirname, "./src"),
        },
    },
    plugins: [vue()],
});
