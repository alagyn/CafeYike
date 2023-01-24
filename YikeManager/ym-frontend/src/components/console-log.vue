<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import type { Ref } from 'vue'
import axios from 'axios'

const console_logs: Ref<Array<string>> = ref([])

const console_log_str = computed(() => {
    return console_logs.value.join("")
})

function updateLogs(force: boolean) {
    axios.get(`/bot/logs/?force=${force}`)
        .then(response => {
            console_logs.value = response.data
        })
        .catch(error => {
            if (error.response.status != 304) {
                console.log("Unknown Error Occurred")
                console.log(error)
            }
        })
}

const show_log = ref(false)
const log_btn_str = ref("Show Log")
function toggleLog() {
    show_log.value = !show_log.value
    if (show_log.value) {
        log_btn_str.value = "Hide Log"
    }
    else {
        log_btn_str.value = "Show Log"
    }
}

onMounted(() => {
    updateLogs(true)
})

</script>

<template>
    <div class="container ym-container">
        Console Logs:
        <button class="btn btn-primary" @click="updateLogs(false)">Refresh</button>
        <button class="btn btn-secondary" @click="toggleLog()">{{ log_btn_str }}</button>
        <pre v-if="show_log" class="mono"><code>{{ console_log_str }}</code></pre>
    </div>

</template>

