<template>
    <div>
        <n-tabs type="line" animated>
            <n-tab-pane name="Android" tab="Android">
                下载柠檬Push安卓App，安装完成后扫【连接】功能中生成的二维码匹配即可使用
                <br />
                <br />
                App下载发布页<br /><n-a @click="openUrl('https://sibtools.app/lemon_push/docs/download')"  >
                    https://sibtools.app/lemon_push/docs/download
                </n-a>
            </n-tab-pane>
            <n-tab-pane name="iOS" tab="iOS">
                iOS无客户端，使用快捷指令实现，获取快捷指令后修改 IP 地址为电脑的 IP 地址即可使用

                <ul>
                    <li><n-a
                            @click="showCode('https://www.icloud.com/shortcuts/e4c0de6a7a7d4a52a8e82f108e509475')">推送剪切板</n-a>
                    </li>
                    <li><n-a
                            @click="showCode('https://www.icloud.com/shortcuts/3dc68dccca8c4818982cfbbd5ae89c44')">获取剪切板</n-a>
                    </li>
                    <li><n-a
                            @click="showCode('https://www.icloud.com/shortcuts/8d94bfed26364c629bca3780090b43ed')">上传文件</n-a>
                    </li>
                    <li><n-a
                            @click="showCode('https://www.icloud.com/shortcuts/29e401c2992b4bbd8c0d204f541a5c07')">下载文件</n-a>
                    </li>
                </ul>


            </n-tab-pane>
            <n-tab-pane name="others" tab="其他">
                柠檬Push原理是将电脑剪切板接口转为http服务对局域网提供服务，所以任意支持发起http请求的终端都可以向本机推送或获取剪切板。
                http请求地址：http://本电脑IP:端口/
                <div v-html="result"></div>
            </n-tab-pane>
        </n-tabs>
        <n-modal v-model:show="showModal" preset="card" :style="bodyStyle" title="扫码获取快捷指令" size="huge"
            @close="closeQrCodeDialog" :bordered="false">
            <div style="display: flex;justify-content: center;">
                <qrcode-vue :value="qrcode_text" :size="250" level="H" />
            </div>
        </n-modal>
    </div>
</template>
  
<script lang="ts" setup>
import { ref } from 'vue'
import MarkdownIt from 'markdown-it';
import QrcodeVue from 'qrcode.vue'
import * as runtime from '../../wailsjs/runtime/runtime';
const qrcode_text = ref('https://sibtools.app/lemon_push/docs/download')
const showModal = ref(false)
const markdown = new MarkdownIt()
const source = "## 接口说明\n" +
    "### 写入电脑剪切板\n" +
    "`/set_clipboard?text=内容`\n" +
    "\n" +
    "返回json\n" +
    "```\n" +
    "{\n" +
    "    \"code\":\"0\",\n" +
    "    \"data\":\"ok\"\n" +
    "}\n" +
    "```\n" +
    "### 获取电脑剪切板\n" +
    "`/get_clipboard`\n" +
    "\n" +
    "返回json\n" +
    "```\n" +
    "{\n" +
    "    \"code\":\"0\",\n" +
    "    \"data\":\"电脑剪切板内容\"\n" +
    "}\n" +
    "```\n" +
    "### 上传文件\n" +
    "文件保存在目录`./_lemon_`\n" +
    "\n" +
    "`/upload`\n" +
    "\n" +
    "请求示例\n" +
    "\n" +
    "`curl --location --request POST 'http://localhost:14756/upload' \\\n" +
    "--form 'file=@\"/E:/Downloads/__UNI__F0B72F8_0809143049.apk\"'`\n" +
    "\n" +
    "### 下载文件\n" +
    "\n" +
    "`/download`\n" +
    "\n" +
    "请求示例\n" +
    "\n" +
    "`curl --location --request GET 'http://localhost:14756/download?filename=__UNI__F0B72F8_0809143049.apk'`"

const result = markdown.render(source);
const showCode = (text: string) => {
    showModal.value = true
    qrcode_text.value = text
}
const bodyStyle = {
    width: '600px'
}
const closeQrCodeDialog = () => {
    showModal.value = false
}
const openUrl = (url: string) => {
    runtime.BrowserOpenURL(url)
}
</script>