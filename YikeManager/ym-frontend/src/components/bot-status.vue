<script setup lang="ts">
import axios, { type AxiosResponse } from 'axios'
import { ref, onMounted, computed } from 'vue'

const STATUS_ONLINE = "Online"
const STATUS_OFFLINE = "Offline"

const _SEC_IN_MIN = 60
const _SEC_IN_HR = _SEC_IN_MIN * 60
const _SEC_IN_DAY = _SEC_IN_HR * 24


function makeTimeStr(secs: number) {
    let days = Math.floor(secs / _SEC_IN_DAY)
    secs %= _SEC_IN_DAY
    let hrs = Math.floor(secs / _SEC_IN_HR)
    secs %= _SEC_IN_HR
    let mins = Math.floor(secs / _SEC_IN_MIN)
    secs %= _SEC_IN_MIN

    return `${days} days, ${hrs} hrs, ${mins} mins, ${secs} secs`
}

const bot_status = ref("Unknown")
const bot_uptime = ref(0)
const server_uptime = ref(0)
const time_offset = ref(0)
const bot_time_msg = ref("Unknown")

const bot_uptime_str = computed(() => {
    return makeTimeStr(bot_uptime.value + time_offset.value)
})

const server_uptime_str = computed(() => {
    return makeTimeStr(server_uptime.value + time_offset.value)
})

function updateStatus(response: any) {
    bot_status.value = response.data.status
    bot_uptime.value = response.data.bot_uptime
    server_uptime.value = response.data.server_uptime
    time_offset.value = 0
    if (bot_status.value == STATUS_ONLINE) {
        bot_time_msg.value = "Bot uptime"
    }
    else {
        bot_time_msg.value = "Bot downtime"
    }

}

const bot_exec = ref("Unknown")

onMounted(() => {
    axios.get('/bot')
        .then((response: any) => {
            updateStatus(response)
        })
        .catch((error: any) => {
            console.log(error)
        })

    setInterval(() => {
        ++time_offset.value;
    }, 1000)

    axios.get("/bot/exec")
        .then((response: AxiosResponse<any, any>) => {
            bot_exec.value = response.data
        })
})

function bot_shutdown() {
    console.log("Stopping bot...")
    axios.post('/bot/stop')
        .then(function (response) {
            console.log("Stopped")
            updateStatus(response)
        })
        .catch(function (error) {
            console.log(error)
            bot_status.value = "ERROR"
        })

}

function bot_startup() {
    console.log("Starting bot...")
    axios.post('/bot/start')
        .then(function (response) {
            console.log("Started")
            updateStatus(response)
        })
        .catch(function (error) {
            console.log(error)
            bot_status.value = "ERROR"
        })
}


</script>

<template>
    <div class="container container-sm ym-container">
        <table class="table-sm status-table">
            <tbody class="text-nowrap">
                <tr>
                    <td>Server Uptime :</td>
                    <td><code class="mono">{{ server_uptime_str }}</code></td>
                </tr>
                <tr>
                    <td>Bot Exec :</td>
                    <td><code class="mono">{{ bot_exec }}</code></td>
                </tr>
                <tr>
                    <td>Bot Status :</td>
                    <td><code class="mono">{{ bot_status }}</code></td>
                </tr>
                <tr>
                    <td>{{ bot_time_msg }} :</td>
                    <td><code class="mono">{{ bot_uptime_str }}</code></td>
                </tr>
            </tbody>
        </table>

        <br>

        <button v-if="bot_status == STATUS_ONLINE" @click="bot_shutdown" class="btn btn-danger">
            Shutdown
        </button>
        <button v-else @click="bot_startup" class="btn btn-primary">
            Startup
        </button>
    </div>
</template>