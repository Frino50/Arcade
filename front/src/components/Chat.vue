<template>
    <div class="chat" v-loading="isLoading">
        <div class="messages" ref="messagesContainer" @scroll="onScroll">
            <div v-for="msg in messages" :key="msg.id" class="message">
                <strong>{{ msg.player }}: </strong>
                <span>{{ parseEmojis(msg.content) }}</span>
                <small>{{ formatTime(msg.timestamp) }}</small>
            </div>
        </div>

        <form @submit.prevent="sendMessage" class="input-container">
            <input v-model="text" placeholder="Écris un message..." />

            <div class="emoji-btn" @click="showEmojiMenu = !showEmojiMenu">
                {{ Emoji.COOL }}
            </div>

            <button type="submit">Envoyer</button>

            <div v-if="showEmojiMenu" class="emoji-menu">
                <span
                    v-for="(emoji, shortcut) in emojiMap"
                    :key="shortcut"
                    class="emoji-item"
                    @click="selectEmoji(shortcut)"
                >
                    {{ emoji }}
                </span>
            </div>
        </form>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from "vue";
import SockJS from "sockjs-client";
import { Client, IMessage } from "@stomp/stompjs";
import { useLocalStore } from "@/store/local.ts";
import messageService from "@/services/messageService.ts";
import { Emoji, emojiMap } from "@/models/enums/emoji.ts";

const text = ref("");
const messages = ref<
    { id: number; player: string; content: string; timestamp: string }[]
>([]);
const showEmojiMenu = ref(false);
const isLoading = ref(false);

let stompClient: Client;
const localstore = useLocalStore();
const messagesContainer = ref<HTMLDivElement | null>(null);

const page = ref(0);
const size = 20;
const loading = ref(false);
let reachedEnd = false;

function parseEmojis(text: string): string {
    let parsedText = text;
    for (const [shortcut, emoji] of Object.entries(emojiMap)) {
        parsedText = parsedText.split(shortcut).join(emoji);
    }
    return parsedText;
}

function selectEmoji(shortcut: string) {
    text.value += shortcut + " ";
    showEmojiMenu.value = false;
}

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
    try {
        isLoading.value = true;
        await loadMessages();
        connect();
        scrollToBottom();
    } finally {
        isLoading.value = false;
    }
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

.input-container {
    display: flex;
    align-items: center;
    gap: 5px;
    margin-top: 15px;
    position: relative;
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
    border: none;
    padding: 10px 15px;
    border-radius: 20px;
    cursor: pointer;
    background-color: #4caf50;
    color: white;
}

button:hover {
    background-color: #45a049;
}

.emoji-btn {
    cursor: pointer;
    width: 2rem;
    height: 2rem;
    display: flex;
    justify-content: center;
    align-items: center;
}

.emoji-btn:hover {
    background-color: #45a049;
    border-radius: 8px;
}

.emoji-menu {
    position: absolute;
    bottom: 50px;
    left: 13rem;
    background-color: #3c3a32;
    border: 1px solid #555;
    border-radius: 8px;
    padding: 5px;
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
    max-width: 300px;
    z-index: 100;
}

.emoji-item {
    cursor: pointer;
    font-size: 1.2rem;
    padding: 3px;
    border-radius: 4px;
}

.emoji-item:hover {
    background-color: #4caf50;
}
</style>
