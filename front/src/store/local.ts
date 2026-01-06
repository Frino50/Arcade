import { reactive } from "vue";

export interface LocalState {
    pseudo: string;
    token: string;
    theme: string;
}

const storedState = JSON.parse(localStorage.getItem("localState") || "{}");

const state = reactive<LocalState>({
    pseudo: storedState.pseudo || "",
    token: storedState.token || "",
    theme: storedState.theme || "cyan",
});

export const localStore = new Proxy(state, {
    set(target, prop: keyof LocalState, value) {
        target[prop] = value;
        localStorage.setItem("localState", JSON.stringify(target));
        return true;
    },
}) as LocalState;
