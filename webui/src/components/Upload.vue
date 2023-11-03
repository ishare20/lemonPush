<script setup lang="ts">
import { defineEmits, ref } from 'vue';
const emits = defineEmits(['handleFileUpload', 'openFileInput']);
const onHandleFileUpload = (e: any) => {
    emits('handleFileUpload', e);
};
const onOpenFileInput = () => {
    emits('openFileInput');
};

const fileInput = ref<any>(null);

defineExpose({
    fileInput,
});
</script>

<template>
    <div class="max-w-md mx-auto bg-white p-8 rounded shadow-md">
        <h1 class="text-3xl font-bold mb-4">Upload</h1>
        <input type="file" ref="fileInput" style="display: none" @change="onHandleFileUpload" />
        <label
            for="fileInput"
            @click="onOpenFileInput"
            class="bg-blue-500 text-white py-2 px-4 rounded cursor-pointer"
            >Choose File</label
        >
        <br />
    </div>
</template>

<style scoped></style>
