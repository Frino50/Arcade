import { reactive, readonly } from "vue";

type ToastMessage = {
    text: string;
    type: "success" | "error" | "info";
};

const state = reactive({
    messages: [] as ToastMessage[],
});

export function useToast() {
    function show(
        message: string,
        type: "success" | "error" | "info" = "info"
    ) {
        state.messages.push({ text: message, type });
        setTimeout(() => state.messages.shift(), 5000);
    }

    function removeToast(index: number) {
        state.messages.splice(index, 1);
    }

    return { show, messages: readonly(state.messages), removeToast };
}
