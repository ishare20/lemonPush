// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: '柠檬Push',
  tagline: '高效推送文本至电脑剪切板',
  favicon: 'img/favicon.ico',

  // Set the production url of your site here
  url: 'https://ishare20.net',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/sibtools/lemon_push/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'facebook', // Usually your GitHub org/user name.
  projectName: 'docusaurus', // Usually your repo name.

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internalization, you can use this field to set useful
  // metadata like html lang. For example, if your site is Chinese, you may want
  // to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'zh-Hans',
    locales: ['zh-Hans'],
  },
  //网站访问统计代码，用于统计网站访问量，自己部署可去掉
  scripts: [
    {
      src: "https://sdk.51.la/js-sdk-pro.min.js?id=K9LPlcjqF4puqNw3&ck=K9LPlcjqF4puqNw3",
      async: true
    }
  ],

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          // editUrl:
          // 'https://github.com/facebook/docusaurus/tree/main/packages/create-docusaurus/templates/shared/',
        },
        blog: {
          showReadingTime: true,
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/facebook/docusaurus/tree/main/packages/create-docusaurus/templates/shared/',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      // Replace with your project's social card
      // image: 'img/docusaurus-social-card.jpg',
      navbar: {
        title: '柠檬Push',
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'tutorialSidebar',
            position: 'left',
            label: '教程',
          },
          { to: 'docs/download', label: '下载', position: 'left' },
          { to: 'https://github.com/ishare20/lemonPush', label: 'Github', position: 'left' },
          {
            href: 'hhttps://ishare20.net/sibtools/',
            label: '小而美tools',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: '文档',
            items: [
              {
                label: '教程',
                to: '/docs/intro',
              },
              {
                label: '路线图',
                href: 'https://support.qq.com/products/405982/roadmap',
              },

            ],
          },
          {
            title: '反馈',
            items: [
              {
                label: '兔小槽',
                href: 'https://support.qq.com/products/405982'
              },
              {
                label: 'Telegram',
                href: 'https://t.me/+ZVIwHSBOg1o5NzFl',
              },
            ],
          },
          {
            title: '更多',
            items: [
              {
                label: '小而美的工具们',
                href: 'https://ishare20.net/sibtools/',
              },
              {
                label: '微信订阅号',
                href: 'https://ishare20.net/files/images/wxdy.png',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} 柠檬Push. Built with Docusaurus.`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
      },
    }),
};

module.exports = config;
