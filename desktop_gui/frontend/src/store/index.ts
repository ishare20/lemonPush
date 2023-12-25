import { createStore } from 'vuex'


export default createStore({
    state: {
        status: false,
        logs: [],
        config: {
            ip: '',
            port: 14756,
            auto_open_url: true,
            folder: './_lemon_/'
        }
    },
    mutations: {
        updateStatus(state, playload) {
            state.status = playload
        },
        updateLogs(state, playload) {
            state.logs = playload
        },
        updateConfig(state, playload) {
            state.config = playload
        }
    }
})