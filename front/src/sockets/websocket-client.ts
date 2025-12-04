import { Client, IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useLocalStore } from "@/store/local";
import { nextTick } from "vue";
import Message from "@/models/message";

let stompClient: Client | null = null;

function createClient(): Client {
    const localstore = useLocalStore();
    const socket = new SockJS(
        "http://202.15.200.35:" + import.meta.env.VITE_BACK_URL + "/ws"
    );

    return new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        connectHeaders: localstore.token
            ? { Authorization: `Bearer ${localstore.token}` }
            : {},
    });
}

export function getStompClient(): Client {
    if (!stompClient) {
        stompClient = createClient();
    }
    return stompClient;
}

export function connexionChat(
    messages: Message[],
    messagesContainer: HTMLDivElement | null
): Client {
    const client = getStompClient();
    const localstore = useLocalStore();

    if (client.connected) return client;

    client.onConnect = () => {
        client.subscribe(
            "/topic/chat",
            async (msg: IMessage) => {
                const newMsg = JSON.parse(msg.body);
                messages.push(newMsg);
                await scrollToBottom(messagesContainer);
            },
            {
                Authorization: `Bearer ${localstore.token}`,
            }
        );
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
