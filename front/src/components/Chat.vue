<template>
    <div class="chat">
        <div class="messages" ref="messagesContainer" @scroll="onScroll">
            <div v-for="msg in messages" :key="msg.id" class="message">
                <strong>{{ msg.player }}:</strong> {{ msg.content }}
                <small>{{ formatTime(msg.timestamp) }}</small>
            </div>
        </div>

        <form @submit.prevent="sendMessage">
            <input v-model="text" placeholder="Écris un message..." />
            <button type="submit">Envoyer</button>
        </form>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from "vue";
import SockJS from "sockjs-client";
import { Client, IMessage } from "@stomp/stompjs";
import { useLocalStore } from "@/store/local.ts";
import messageService from "@/services/messageService.ts";

const text = ref("");
const messages = ref<
    { id: number; player: string; content: string; timestamp: string }[]
>([]);

let stompClient: Client;
const localstore = useLocalStore();
const messagesContainer = ref<HTMLDivElement | null>(null);

const page = ref(0);
const size = 20;
const loading = ref(false);
let reachedEnd = false;

function connect() {
    const socket = new SockJS("http://202.15.200.35:8085/ws");
    stompClient = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        connectHeaders: localstore.token
            ? { Authorization: `Bearer ${localstore.token}` }
            : {},
    });

    stompClient.onConnect = () => {
        console.log("Connecté au chat STOMP");

        stompClient.subscribe("/topic/chat", (msg: IMessage) => {
            const newMsg = JSON.parse(msg.body);
            messages.value.push(newMsg);
            scrollToBottom();
        });
    };

    stompClient.activate();
}

function disconnect() {
    stompClient?.deactivate();
}

async function loadMessages() {
    if (loading.value || reachedEnd) return;
    loading.value = true;

    if (!messagesContainer.value) return;

    const container = messagesContainer.value;
    const scrollHeightBefore = container.scrollHeight;

    const res = await messageService.loadMessages(page.value, size);
    const newMessages = res.data.content;

    if (newMessages.length === 0) {
        reachedEnd = true;
    } else {
        messages.value = [...newMessages.reverse(), ...messages.value];

        await nextTick();

        const scrollHeightAfter = container.scrollHeight;
        container.scrollTop = scrollHeightAfter - scrollHeightBefore;

        page.value++;
    }

    loading.value = false;
}

async function sendMessage() {
    if (!text.value.trim() || !stompClient || !stompClient.connected) return;
    await messageService.sendMessage(text.value);
    text.value = "";
}

function formatTime(ts: string) {
    const date = new Date(ts);
    return date.toLocaleString("fr-FR", {
        day: "2-digit",
        month: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
    });
}

function scrollToBottom() {
    nextTick(() => {
        if (messagesContainer.value) {
            messagesContainer.value.scrollTop =
                messagesContainer.value.scrollHeight;
        }
    });
}

function onScroll() {
    if (messagesContainer.value && messagesContainer.value.scrollTop === 0) {
        loadMessages();
    }
}

onMounted(async () => {
    await loadMessages();
    connect();
    scrollToBottom();
});

onBeforeUnmount(() => {
    disconnect();
});
</script>

<style scoped>
.chat {
    background-color: var(--marron-fonce);
    border-radius: 8px;
    padding: 15px;
    width: 30rem;
}

.messages {
    height: 28.5rem;
    overflow-y: auto;
    padding: 10px;
    border-radius: 6px;
    background-color: #3c3a32;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.message {
    padding: 8px 12px;
    background-color: var(--marron-clair);
    border-radius: 12px;
    word-wrap: break-word;
    color: var(--marron-back);
}

.message strong {
    color: #4caf50;
}

.message small {
    display: block;
    text-align: right;
    font-size: 0.7em;
}

form {
    display: flex;
    gap: 10px;
    margin-top: 15px;
}

input {
    flex: 1;
    padding: 10px 15px;
    border-radius: 20px;
    border: none;
    background-color: var(--marron-clair);
    color: var(--marron-back);
    outline: none;
}

button {
    background-color: #4caf50;
    border: none;
    padding: 10px 20px;
    border-radius: 20px;
    cursor: pointer;
    transition: background-color 0.3s ease;
}

button:hover {
    background-color: #45a049;
}
</style>
