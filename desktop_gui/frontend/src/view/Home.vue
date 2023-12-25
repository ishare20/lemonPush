<template>
  <div>
    <n-form label-placement="left">
      <div>启动{{ status ? '成功' : '失败' }}，首次使用请选择IP生成二维码匹配，电脑IP如无变动无需重新匹配</div>

      <n-form-item style="margin-top: 16px;">
        监听端口：<div style="font-size:20px;">{{ config.port }}</div>

        <n-button type="info" style="margin-left: 16px;" @click="showPort">
          修改端口
        </n-button>
        <n-button type="primary" style="margin-left: 16px;" @click="genQRCode">
          生成二维码
        </n-button>

        <div style="margin-left: 16px;">
          <n-popover trigger="click">
            <template #trigger>
              <n-button color="#8a2be2">查看电脑IP</n-button>
            </template>
            <div>
              <div style="display: block;margin-top: 8px;" v-for="option in options">
                <n-button strong secondary type="info" @click="copyText(option.value)">
                  {{ option.value }}</n-button>
              </div>
            </div>
          </n-popover>
        </div>

        <n-button type="warning" style="margin-left: 16px;" @click="hideApp">
          隐藏到托盘图标
        </n-button>



      </n-form-item>
      <n-form-item label="自动打开文本中的链接：" label-align="left">
        <n-switch v-model:value="config.auto_open_url" @update:value="saveConfig(1)" />
      </n-form-item>
      <n-modal v-model:show="showPortModal" class="custom-card" preset="card" :style="bodyStyle" title="修改监听端口"
        size="huge" :bordered="false">
        <n-form-item label="监听端口：" label-placement="left">
          <n-input-number v-model:value="config.port" :show-button="false" :update-value-on-input="false" :min="1024"
            :max="65535" placeholder="端口范围1024到65535" />
          <n-button type="primary" style="margin-left: 8px;" @click="saveConfig(0)">
            保存
          </n-button>
        </n-form-item>
      </n-modal>
    </n-form>
    <div style="display: flex;width: 100%;flex-direction: column;">
      <div style="display: flex;justify-content: space-between;">接收日志： <n-button @click="clearLogs" strong secondary
          type="info">清空日志</n-button></div>
      <div style="margin-top: 12px;width: 80%;">
        <n-list bordered>
          <n-list-item v-for="log in logs">
            <n-thing>
              <div style="word-wrap: break-word;">{{ log }}</div>
            </n-thing>
            <template #suffix>
              <n-button @click="copyText(log)">复制</n-button>
            </template>
          </n-list-item>
        </n-list>
      </div>
    </div>
    <n-modal v-model:show="showModal" class="custom-card" preset="card" :style="bodyStyle" title="二维码生成" size="huge"
      @close="closeQrCodeDialog" :bordered="false">
      <n-form-item label="IP地址：" label-placement="left">
        <n-select :options="options" @update:value="selectIP" placeholder="请选择电脑所在网络的IP地址" />
        <n-button type="primary" style="margin-left: 8px;" @click="reFreshIp">
          刷新
        </n-button>
      </n-form-item>
      <n-form-item v-if="show_qrcode">
        <qrcode-vue :value="qrcode_text" :size="300" level="H" />
      </n-form-item>
    </n-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { SelectOption, useMessage } from 'naive-ui'
import { GetIPList, InitListener, LoadConfig, SaveConfig } from "../../wailsjs/go/main/App";
import { computed } from 'vue';
import * as runtime from '../../wailsjs/runtime/runtime';
import QrcodeVue from 'qrcode.vue'
import { useStore } from 'vuex';
const showModal = ref(false)
const showPortModal = ref(false)
const bodyStyle = {
  width: '600px'
}
const store = useStore();
const logs = computed(() => store.state.logs);
const message = useMessage()

const config = computed(() => store.state.config)


const hideApp = () => {
  runtime.WindowHide()
}

const status = computed(() => store.state.status);

const options = ref<any[]>([]);
const qrcode_text = ref('柠檬Push，请先选择IP')
const show_qrcode = ref(false)
const loadConfig = () => {
  if (!status.value) {
    message.loading(
      '加载配置中'
    )
    LoadConfig().then((res) => {
      store.commit('updateConfig', res)
      message.loading(
        '启动服务中'
      )
      InitListener(res).then((res: any) => {
        console.log(res);
        message.destroyAll()
        if (res.code === 1) {
          store.commit('updateStatus', true)
        } else {
          store.commit('updateStatus', false)
          message.error('启动出错：' + res.msg)
        }
      }).catch(e => {
        console.log(e);
      })
    }).catch(e => {
      console.log(e);
    })
    runtime.EventsOn('showLogs', (log: any) => {
      store.state.logs.unshift(log)
    });
  }
}
const selectIP = (value: string, option: SelectOption) => {
  show_qrcode.value = true
  let config = store.state.config
  config.ip = value
  qrcode_text.value = config.ip + ':' + config.port
}

const closeQrCodeDialog = () => {
  show_qrcode.value = false
}
const reFreshIp = () => {
  GetIPList().then(res => {
    message.destroyAll()
    options.value = toOptions(res)
  }).catch(e => {
    message.error(
      '获取电脑IP失败' + e.toString()
    )
  })
}

const clearLogs = () => {
  //store.state.logs =[]
  store.commit('updateLogs', [])
}

const genQRCode = () => {
  reFreshIp()
  showModal.value = true
}

const copyText = (text: string) => {
  navigator.clipboard.writeText(text).then(res => {
    message.success('复制成功')
  })
}

const toOptions = (list: string[]) => {
  return list.map(item => {
    return {
      label: item,
      value: item
    }
  })
}

const showPort = () => {
  showPortModal.value = true
}

const saveConfig = (e: any) => {
  let config = store.state.config
  SaveConfig(config).then((result: any) => {
    if (result.code === 1) {
      if (e === 0) {
        message.success(
          '修改成功，重启后生效'
        )
      } else {
        message.success(
          '修改成功'
        )
      }

      showPortModal.value = false
    } else {
      message.error(result.msg)
    }
  })
}
loadConfig()
reFreshIp()


</script>