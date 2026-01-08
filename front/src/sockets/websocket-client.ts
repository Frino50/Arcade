import { Client, IMessage } from "@stomp/stompjs";
import { localStore } from "@/store/local.ts";
import SockJS from "sockjs-client";

let stompClient: Client | null = null;

function createClient(): Client {
    const socket = new SockJS(
        "http://202.15.200.35:" + import.meta.env.VITE_BACK_URL + "/ws"
    );

    return new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        connectHeaders: localStore.token
            ? { Authorization: `Bearer ${localStore.token}` }
            : {},
    });
}

export function getStompClient(): Client {
    if (!stompClient) {
        stompClient = createClient();
    }
    return stompClient;
}

export function connexionChat(onMessageReceived: (msg: any) => void): Client {
    const client = getStompClient();

    const subscribeToTopic = () => {
        client.subscribe(
            "/topic/chat",
            (msg: IMessage) => {
                const newMsg = JSON.parse(msg.body);
                onMessageReceived(newMsg);
            },
            {
                Authorization: `Bearer ${localStore.token}`,
            }
        );
    };

    if (client.connected) {
        subscribeToTopic();
    } else {
        client.onConnect = () => {
            subscribeToTopic();
        };
        client.activate();
    }

    return client;
}
