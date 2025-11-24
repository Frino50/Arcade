import { nextTick } from "vue";
import { Client, IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useLocalStore } from "@/store/local.ts";
import Message from "@/models/message.ts";

let stompClient: Client | null = null;

export function getStompClient(): Client {
    if (stompClient) {
        return stompClient;
    }

    const localstore = useLocalStore();
    const socket = new SockJS(
        "http://202.15.200.35:+" + import.meta.env.VITE_API_BASE_URL + "/ws"
    );

    stompClient = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        connectHeaders: localstore.token
            ? { Authorization: `Bearer ${localstore.token}` }
            : {},
    });

    return stompClient;
}

export function connexionChat(
    messages: Message[],
    messagesContainer: HTMLDivElement | null
): Client {
    const client = getStompClient();

    client.onConnect = () => {
        client.subscribe("/topic/chat", async (msg: IMessage) => {
            const newMsg = JSON.parse(msg.body);
            messages.push(newMsg);
            await scrollToBottom(messagesContainer);
        });
    };

    client.activate();

    return client;
}

async function scrollToBottom(messagesContainer: HTMLDivElement | null) {
    await nextTick();
    if (messagesContainer) {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
}
