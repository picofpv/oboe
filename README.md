# Oboe [![Build Status](https://travis-ci.org/google/oboe.svg?branch=master)](https://travis-ci.org/google/oboe)

[![Oboe视频介绍](docs/images/getting-started-video.jpg)](https://www.bilibili.com/video/av35706771?from=search&seid=509003659581018982)

Oboe是一个 C++ 库，可轻松在 Android 上构建高性能音频应用程序。 创建它主要是为了使开发人员可以定位简化的API,它可以跨多个API级别工作，甚至支持API 16(Jelly Bean，Android 4.1).

## 特性
- 兼容API 16+ - 可以在99％的Android设备上运行
- 可选择音频API (API 16+可用OpenSL ES，API 27+可用 AAudio) 这将在目标Android设备上提供最佳的音频性能
- 自动延迟调整
- 现代C ++允许您编写简洁，优雅的代码
- [使用Oboe框架的应用程序 列表](docs/AppsUsingOboe.md)

## 使用前的要求
要构建Oboe，您需要一个支持C ++ 14和Android头文件的编译器。 获得这些内容的最简单方法是下载Android NDK r17或更高版本。 可以使用Android Studio的SDK管理器进行安装, 或通过 [直接下载](https://developer.android.com/ndk/downloads/).

## API文档
- [入门指南](docs/GettingStarted.md)
- [完整指南Oboe](docs/FullGuide.md)
- [API参考](https://google.github.io/oboe/reference)
- [技术说明](docs/notes/)
  - [用 Oboe 实现声音效果](docs/notes/effects.md)
  - [断开音频流](docs/notes/disconnect.md)
- [Android版本的音频功能/错误的历史记录](docs/AndroidAudioHistory.md)
- [经常问的问题](docs/FAQ.md) (FAQ)
- [我们的路线图](https://github.com/google/oboe/milestones) - 通过在第一个评论中点赞来对功能/问题进行投票。

## 测试
- [**OboeTester** app也包含在本项目中，用于测量延迟的应用, 小故障, etc.](https://github.com/google/oboe/tree/master/apps/OboeTester/docs)
- [Oboe单元测试](https://github.com/google/oboe/tree/master/tests)

## 视频
- [Oboe 入门](https://www.youtube.com/playlist?list=PLWz5rJ2EKKc_duWv9IPNvx9YBudNMmLSa)
- [低延迟音频 - 因为你的耳朵值得](https://www.youtube.com/watch?v=8vOf_fDtur4) (Android Dev Summit '18)
- [带有100个振荡器合成器的实时音频](https://www.youtube.com/watch?v=J04iPJBkAKs) (DroidCon Berlin '18)
- [在Android上取胜](https://www.youtube.com/watch?v=tWBojmBpS74) - How to optimize an Android audio app. (ADC '18)
- [Android上的实时处理](https://youtu.be/hY9BrS2uX-c) (ADC '19)

## 范例代码
可以在以下位置找到示例应用 [samples 目录](samples). Also check out the [Rhythm Game codelab](https://codelabs.developers.google.com/codelabs/musicalgame-using-oboe/index.html#0).

### 第三方示例代码
- [Ableton Link integration demo](https://github.com/jbloit/AndroidLinkAudio) (author: jbloit)

## 贡献
我们很想收到您的 pull requests. Before we can though, please read the [contributing](CONTRIBUTING.md) guidelines.

## 版本历史
View the [releases page](../../releases).

## License
[LICENSE](LICENSE)

