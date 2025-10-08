import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import piniaPluginPersistedstate from "pinia-plugin-persistedstate";
import { loading } from "@/directives/loading.ts";
import { clickOutside } from "@/directives/clickOutside.ts";

import "./style.css";
import "./color.css";

const app = createApp(App);
const pinia = createPinia();
app.use(pinia);
pinia.use(piniaPluginPersistedstate);
app.use(router);
app.directive("loading", loading);
app.directive("clickOutside", clickOutside);
app.mount("#app");
