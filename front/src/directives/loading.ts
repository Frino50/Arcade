import { h, render } from "vue";
import LoadingOverlay from "@/components/LoadingOverlay.vue";

interface HTMLElementWithLoading extends HTMLElement {
    _loadingInstance?: any;
}

export const loading = {
    mounted(el: HTMLElementWithLoading, binding: any) {
        if (getComputedStyle(el).position === "static") {
            el.style.position = "relative";
        }

        const container = document.createElement("div");
        container.style.position = "absolute";
        container.style.top = "0";
        container.style.left = "0";
        container.style.width = "100%";
        container.style.height = "100%";
        container.style.display = "flex";
        container.style.justifyContent = "center";
        container.style.alignItems = "center";
        container.style.pointerEvents = "none";
        container.style.borderRadius = "inherit";
        el.appendChild(container);

        const vnode = h(LoadingOverlay, { loading: binding.value });
        render(vnode, container);

        el._loadingInstance = { vnode, container };
    },

    updated(el: HTMLElementWithLoading, binding: any) {
        if (el._loadingInstance) {
            const vnode = h(LoadingOverlay, { loading: binding.value });
            render(vnode, el._loadingInstance.container);
        }
    },

    unmounted(el: HTMLElementWithLoading) {
        if (el._loadingInstance) {
            render(null, el._loadingInstance.container);
            el._loadingInstance.container.remove();
            delete el._loadingInstance;
        }
    },
};
