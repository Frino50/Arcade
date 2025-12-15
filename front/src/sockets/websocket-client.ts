import { Client, IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useLocalStore } from "@/store/local";

let stompClient: Client | null = null;

function createClient(): Client {
    const localstore = useLocalStore();
    // Attention : Assure-toi que l'URL est correcte (http vs ws)
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

export function connexionChat(onMessageReceived: (msg: any) => void): Client {
    const client = getStompClient();
    const localstore = useLocalStore();

    const subscribeToTopic = () => {
        client.subscribe(
            "/topic/chat",
            (msg: IMessage) => {
                const newMsg = JSON.parse(msg.body);
                onMessageReceived(newMsg);
            },
            {
                Authorization: `Bearer ${localstore.token}`,
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
