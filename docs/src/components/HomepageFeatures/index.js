import React from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';

const FeatureList = [
  {
    title: '推送文本到电脑剪切板',
    Svg: require('@site/static/img/preferences_rafiki.svg').default,
    description: (
      <>
        在同一WiFi网络下，手机一键推送文本到电脑剪切板
      </>
    ),
  },
  {
    title: '链接自动打开',
    Svg: require('@site/static/img/online_page-amico.svg').default,
    description: (
      <>
        推送文本如包含链接，自动使用默认浏览器打开
      </>
    ),
  },
  {
    title: '多平台支持',
    Svg: require('@site/static/img/video_game_developer-bro.svg').default,
    description: (
      <>
        电脑端支持Windows、Mac、Linux
      </>
    ),
  },
];

function Feature({ Svg, title, description }) {
  return (
    <div className={clsx('col col--4')}>
      {<div className="text--center">
        <Svg className={styles.featureSvg} role="img" />
      </div>}
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
