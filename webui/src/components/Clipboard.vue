<script setup lang="ts">
import { defineEmits } from 'vue';
const emits = defineEmits(['setClipboard', 'getClipboard']);
const onSetClipboard = () => {
    emits('setClipboard');
};
const onGetClipboard = () => {
    emits('getClipboard');
};

const isIos = () => {
    const userAgent = window.navigator.userAgent.toLowerCase();
    return /iphone|ipad|ipod/.test(userAgent);
};
</script>

<template>
    <div class="max-w-md mx-auto bg-white p-8 rounded shadow-md">
        <h1 class="text-3xl font-bold mb-4">Clipboard</h1>

        <div class="flex space-x-4">
            <button
                @click="onSetClipboard"
                class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline"
            >
                Set clipboard
            </button>
            <button
                @click="onGetClipboard"
                class="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline"
            >
                Get clipboard
            </button>
        </div>
    </div>
</template>

<style scoped></style>
