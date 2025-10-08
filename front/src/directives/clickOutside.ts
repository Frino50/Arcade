import { DirectiveBinding } from "vue";

interface ClickOutsideElement extends HTMLElement {
    __ClickOutsideHandler__?: (event: MouseEvent) => void;
}

export const clickOutside = {
    mounted(el: ClickOutsideElement, binding: DirectiveBinding) {
        el.__ClickOutsideHandler__ = (event: MouseEvent) => {
            if (!(el === event.target || el.contains(event.target as Node))) {
                binding.value();
            }
        };
        document.addEventListener("click", el.__ClickOutsideHandler__);
    },
    unmounted(el: ClickOutsideElement) {
        if (el.__ClickOutsideHandler__) {
            document.removeEventListener("click", el.__ClickOutsideHandler__);
            delete el.__ClickOutsideHandler__;
        }
    },
};
