<template>
  <n-message-provider>
    <n-layout has-sider content-style="height:100vh;">
      <n-layout-sider bordered width="148">
        <n-menu :options="menuOptions" />
      </n-layout-sider>
      <n-layout>
        <!--      <n-layout-header bordered>头部</n-layout-header>-->
        <n-layout-content bordered content-style="padding: 24px;">
          <router-view></router-view>
        </n-layout-content>
      </n-layout>
    </n-layout>
  </n-message-provider>
</template>

<script lang="ts">
import { defineComponent, h, Component } from 'vue'
import { NIcon } from 'naive-ui'
import type { MenuOption } from 'naive-ui'
import { RouterLink } from 'vue-router'
import {
  SettingsOutline as SettingsIcon,
  InformationCircleOutline as InfoIcon,
  BookOutline as GuideIcon,
  LogOutOutline as HomeIcon,
  DesktopOutline as DesktopIcon,
  PhonePortraitOutline as PhoneIcon,
} from '@vicons/ionicons5'

function renderIcon(icon: Component) {
  return () => h(NIcon, null, { default: () => h(icon) })
}

const menuOptions: MenuOption[] = [
  {
    label: () =>
      h(
        RouterLink,
        {
          to: {
            name: 'home',
          }
        },
        { default: () => '连接' }
      ),
    key: 'home',
    icon: renderIcon(DesktopIcon)
  },
  {
    label: () =>
      h(
        RouterLink,
        {
          to: {
            path: '/guide',
          }
        },
        { default: () => '教程' }
      ),
    key: 'settings',
    icon: renderIcon(GuideIcon)
  }
  ,
  {
    label: () =>
      h(
        RouterLink,
        {
          to: {
            path: '/about',
          }
        },
        { default: () => '关于' }
      ),
    key: 'about',
    icon: renderIcon(InfoIcon)
  }
]
export default defineComponent({
  setup() {
    return {
      menuOptions
    }
  }
})
</script>
