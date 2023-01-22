
const { createApp, ref, onMounted, computed, watch } = Vue

const STATUS_ONLINE = "Online"
const STATUS_OFFLINE = "Offline"

const _SEC_IN_MIN = 60
const _SEC_IN_HR = _SEC_IN_MIN * 60
const _SEC_IN_DAY = _SEC_IN_HR * 24

function makeTimeStr(secs)
{
    let days = Math.floor(secs / _SEC_IN_DAY)
    secs %= _SEC_IN_DAY
    let hrs = Math.floor(secs / _SEC_IN_HR)
    secs %= _SEC_IN_HR
    let mins = Math.floor(secs / _SEC_IN_MIN)
    secs %= _SEC_IN_MIN

    return `${days} days, ${hrs} hrs, ${mins} mins, ${secs} secs`
}

// TODO poll backend status occasionaly

const app = createApp({
    setup() {
        
        const bot_status = ref("Unknown")
        const bot_uptime = ref(0)
        const server_uptime = ref(0)
        const time_offset = ref(0)

        const bot_uptime_str = computed(() =>
        {
            if(bot_status.value == STATUS_ONLINE)
            {
                return makeTimeStr(bot_uptime.value + time_offset.value)
            }
            else
            {
                return "Offline"
            }
        })

        const server_uptime_str = computed(() =>
        {
            return makeTimeStr(server_uptime.value + time_offset.value)
        })

        function updateStatus(response)
        {
            bot_status.value = response.data.status
            bot_uptime.value = response.data.bot_uptime
            server_uptime.value = response.data.server_uptime
            time_offset.value = 0
        }

        onMounted(() => {
            axios.get('/bot')
                .then((response) =>
                {
                    updateStatus(response)
                })
                .catch((error) =>
                {
                    console.log(error)
                })

            setInterval(() => 
            {
                ++time_offset.value;
            }, 1000)
        })

        function bot_shutdown()
        {
            console.log("Stopping bot...")
            axios.post('/bot/stop')
                .then(function (response)
                {
                    console.log("Stopped")
                    updateStatus(response)
                })
                .catch(function (error)
                {
                    console.log(error)
                    bot_status.value = "ERROR"
                })
            
        }

        function bot_startup()
        {
            console.log("Starting bot...")
            axios.post('/bot/start')
                .then(function (response)
                {
                    console.log("Started")
                    updateStatus(response)
                })
                .catch(function (error)
                {
                    console.log(error)
                    bot_status.value = "ERROR"
                })
        }

        return {
            bot_status,
            bot_uptime_str,
            server_uptime,
            server_uptime_str,
            bot_startup,
            bot_shutdown,
            STATUS_OFFLINE,
            STATUS_ONLINE
        }
    },

})

// Setting delimiters so that it won't conflict with Jinja
app.config.compilerOptions.delimiters = [ '[[', ']]' ]

app.mount('#app')
