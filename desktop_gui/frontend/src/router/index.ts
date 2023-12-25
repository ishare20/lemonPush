import { createRouter, createWebHistory } from 'vue-router';

// 导入组件
import Home from '../view/Home.vue';
import About from '../view/About.vue';
import Guide from '../view/Guide.vue';


const routes = [
    {
        name: 'index',
        path: '/', component: Home
    },
    {
        name: 'home',
        path: '/home', component: Home
    },
    {
        name: 'about',
        path: '/about',
        component: About
    }
    ,
    {
        name: 'guide',
        path: '/guide',
        component: Guide
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes,
})

export default router;