<script setup lang="ts">
import { ref } from 'vue';
const emits = defineEmits(['showToast']);

defineProps<{ baseUrl: string }>();
const editing = ref(false);
const newBaseUrl = ref('');
const editBaseUrl = () => {
    editing.value = !editing.value;
};
const confirmEdit = () => {
    editing.value = false;
    localStorage.setItem('baseUrl', newBaseUrl.value);
    emits('showToast', 'Saved, please refresh the page');
};
</script>

<template>
    <p @click="editBaseUrl" v-if="!editing">{{ baseUrl }}</p>

    <form v-if="editing">
        <div class="relative">
            <input
                type="text"
                v-model="newBaseUrl"
                class="block w-full p-4 pl-10 text-sm text-gray-900 border border-gray-300 rounded-lg bg-gray-50 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500"
                required
            />
            <button
                type="submit"
                @click="confirmEdit"
                class="text-white absolute right-2.5 bottom-2.5 bg-blue-700 hover:bg-blue-800 focus:ring-4 focus:outline-none focus:ring-blue-300 font-medium rounded-lg text-sm px-4 py-2 dark:bg-blue-600 dark:hover:bg-blue-700 dark:focus:ring-blue-800"
            >
                Confirm
            </button>
        </div>
    </form>
</template>

<style scoped>
li {
    list-style: none;
}
</style>
