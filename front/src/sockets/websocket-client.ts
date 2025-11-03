import { nextTick } from "vue";
import { Client, IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useLocalStore } from "@/store/local.ts";
import Message from "@/models/message.ts";

export function initialisation(): Client {
    const localstore = useLocalStore();
    const socket = new SockJS("http://202.15.200.35:8085/ws");

    return new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        connectHeaders: localstore.token
            ? { Authorization: `Bearer ${localstore.token}` }
            : {},
    });
}

export function connexionChat(
    messages: Message[],
    messagesContainer: HTMLDivElement | null
): Client {
    const stompClient = initialisation();

    stompClient.onConnect = () => {
        stompClient.subscribe("/topic/chat", async (msg: IMessage) => {
            const newMsg = JSON.parse(msg.body);
            messages.push(newMsg);
            await scrollToBottom(messagesContainer);
        });
    };

    stompClient.activate();

    return stompClient; // on retourne le client pour gérer la déconnexion dans le composant
}

async function scrollToBottom(messagesContainer: HTMLDivElement | null) {
    await nextTick();
    if (messagesContainer) {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
}
