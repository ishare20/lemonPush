<script setup lang="ts">
import { LemonPushClient } from '@/api';
import Clipboard from '@/components/Clipboard.vue';
import FileList from '@/components/FileList.vue';
import Settings from '@/components/Settings.vue';
import Toast from '@/components/Toast.vue';
import Upload from '@/components/Upload.vue';
import { onMounted, onUnmounted, ref } from 'vue';

// 先从localStoage 中获取 baseUrl
const baseUrl = ref(localStorage.getItem('baseUrl') || '');

const client = new LemonPushClient(baseUrl.value);

let clipedText = ref('');

let files = ref([] as string[]);

const getFiles = async () => {
    const _files = await client.getFiles();
    console.log(_files);
    files.value = _files;
};

const handleFileUpload = async (event: Event) => {
    console.log(event);
    // @ts-ignore
    const file = event.target.files[0];
    await client.upload(file);
    await getFiles();
};

const upload = ref<any>(null);

const openFileInput = () => {
    upload.value.fileInput.click();
};

const download = async (file: string) => {
    await client.download(file);
};

const setClipboard = async () => {
    try {
        const clipedText = await navigator.clipboard.readText();
        await client.setClipboard(clipedText);
    } catch (error: any) {
        alert(error.message);
    }
};

const getClipboard = async () => {
    const clipText = await client.getClipboard();
    unsecuredCopyToClipboard(clipText);
    showToast('Copied to clipboard');
};


const unsecuredCopyToClipboard = (text: string) => {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    try {
        document.execCommand('copy');
    } catch (err) {
        console.error('Unable to copy to clipboard', err);
    }
    document.body.removeChild(textArea);
};

const page = ref(['clipboard', 'fileList', 'upload', 'settings']);

const currentPage = ref('clipboard');

const onSwipeLeft = () => {
    const index = page.value.indexOf(currentPage.value);
    if (index < page.value.length - 1) {
        currentPage.value = page.value[index + 1];
    }
};
const onSwipeRight = () => {
    const index = page.value.indexOf(currentPage.value);
    if (index > 0) {
        currentPage.value = page.value[index - 1];
    }
};

const startX = ref(0);
const startY = ref(0);
const threshold = 50;

const handleTouchStart = (event: any) => {
    startX.value = event.touches[0].clientX;
    startY.value = event.touches[0].clientY;
};

const handleTouchEnd = (event: any) => {
    const deltaX = event.changedTouches[0].clientX - startX.value;
    const deltaY = event.changedTouches[0].clientY - startY.value;

    if (Math.abs(deltaX) > Math.abs(deltaY)) {
        if (deltaX > threshold) {
            onSwipeRight();
        } else if (deltaX < -threshold) {
            onSwipeLeft();
        }
    }
};

const inputBaseUrl = (title: string) => {
    //  获取当前页面的url
    const defaultUrl = `${window.location.protocol}//${window.location.host}`;
    return prompt(title, defaultUrl) as string;
};
const toast = ref<any>(null);

const showToast = (message: string) => {
    toast.value.showToast(message);
};

const getQueryVariable = (variable: string) => {
    var query = window.location.search.substring(1);
    var vars = query.split('&');
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split('=');
        if (pair[0] == variable) {
            return pair[1];
        }
    }
    return false;
};

onMounted(async () => {
    if (!baseUrl.value) {
        baseUrl.value = inputBaseUrl('Please input baseUrl:');
        if (!baseUrl.value) {
            window.location.reload();
        } else {
            localStorage.setItem('baseUrl', baseUrl.value);
            await getFiles();
        }
    } else {
        await getFiles();
    }
    window.addEventListener('touchstart', handleTouchStart);
    window.addEventListener('touchend', handleTouchEnd);
});

onUnmounted(() => {
    window.removeEventListener('touchstart', handleTouchStart);
    window.removeEventListener('touchend', handleTouchEnd);
});

window.onload = () => {
    clipedText.value = getQueryVariable('clipedText') || '';
    console.log(clipedText.value);
};
</script>

<template>
    <Toast ref="toast" />
    <!-- 左右滚动切换页面 -->
    <div v-on:swipe.left="onSwipeLeft" v-if="currentPage === 'clipboard'">
        <Clipboard @set-clipboard="setClipboard" @get-clipboard="getClipboard" />
    </div>
    <div v-if="currentPage === 'fileList'" class="p-4 border rounded">
        <FileList :files="files" @download="download" />
    </div>
    <div v-else-if="currentPage === 'upload'" class="p-4 border rounded">
        <Upload
            ref="upload"
            @handle-file-upload="handleFileUpload"
            @open-file-input="openFileInput"
        />
    </div>
    <div v-else-if="currentPage === 'settings'" class="p-4 border rounded">
        <h1 class="text-2xl mb-4">Settings</h1>
        <Settings :base-url="baseUrl" @show-toast="showToast" />
    </div>
</template>

<style scoped></style>
