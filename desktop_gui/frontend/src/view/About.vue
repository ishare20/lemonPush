<template>
    <div>
        <n-h3>柠檬Push v1.0.5</n-h3>
        <n-collapse :default-expanded-names="['1']">
            <n-collapse-item title="软件简介" name="1">
                <div>同一WiFi环境下手机高效推送文本到电脑剪切板的工具，移动端支持Android、iOS，电脑端支持Windows、Mac、Linux平台</div>
            </n-collapse-item>
            <n-collapse-item title="项目地址" name="2">
                <n-a @click="openUrl('https://github.com/ishare20/lemonPush')">Github</n-a>
            </n-collapse-item>
            <n-collapse-item title="常见问题" name="3">
                <div>
                    电脑无法接收手机剪切板推送，需要配置允许应用通过防火墙
                </div>
            </n-collapse-item>
            <n-collapse-item title="版本更新" name="4">
                <div><n-a href="https://t.me/+ZVIwHSBOg1o5NzFl" target="_blank">电报群</n-a>、微信订阅号发布更新版本</div>
                <n-image style="margin-top: 12px;" width="60%" src="https://ishare20.net/files/images/wxdy.png" />
            </n-collapse-item>
            <n-collapse-item title="建议反馈" name="5">
                <div><n-a @click="openUrl('https://support.qq.com/products/405982')">https://support.qq.com/products/405982</n-a></div>
            </n-collapse-item>

            <n-collapse-item title="开发者作品" name="6">
                <div><n-a @click="openUrl('https://sibtools.app/')">https://sibtools.app/</n-a></div>
            </n-collapse-item>
            <n-collapse-item title="支持开发者" name="7">
                柠檬Push如你对有所帮助，欢迎star、PR、feedback、share、donate<n-a
                    @click="openUrl('https://sibtools.app/lemon_push/')">项目</n-a>支持开发者
                <n-image style="margin-top: 12px;" width="60%" src="https://ishare20.net/files/images/zw.jpg" />
            </n-collapse-item>
        </n-collapse>
    </div>
</template>

<script lang="ts" setup>
import * as runtime from '../../wailsjs/runtime/runtime';
const openUrl = (url: string) => {
    runtime.BrowserOpenURL(url)
}
</script>

<style></style>