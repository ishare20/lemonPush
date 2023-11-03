<script setup lang="ts">
import { defineEmits } from 'vue';
const emits = defineEmits(['download']);
defineProps<{ files: string[] }>();
const onDownload = (file: string) => {
    emits('download', file);
};
</script>

<template>
    <div>
        <h2 class="text-lg font-bold mb-2">FileList</h2>
        <ul class="border border-gray-300 p-4">
            <li v-for="f in files" :key="f" class="flex justify-between items-center mb-2">
                <span class="text-lg font-bold mb-2">{{ f }}</span>
                <button
                    @click="onDownload(f)"
                    class="bg-blue-500 hover:bg-blue-500 text-white font-bold py-1 px-2 rounded focus:outline-none focus:shadow-outline"
                >
                    download
                </button>
            </li>
        </ul>
    </div>
</template>

<style scoped></style>
