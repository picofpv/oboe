# Changelog

**此更新日志已弃用**. See the [Oboe 发行页面](https://github.com/google/oboe/releases) 有关每个发行版的内容。

## [1.0.0](https://github.com/google/oboe/releases/tag/1.0.0)
#### 2nd October 2018
首次正式版发布

**API changes**
- [去掉 `AudioStream::setNativeFormat`](https://github.com/google/oboe/pull/213/commits/0e8af6a65efef55ec180f8ce76e699adcee5f413)
- [去掉 `AudioStream::isPlaying`](https://github.com/google/oboe/pull/213/commits/6437f5aa224330fbdf77ecc161cc868be663a974).
- [添加 `AudioStream::getTimestamp(clockid_t)`](https://github.com/google/oboe/pull/213/commits/ab695c116e5f196e57560a86efa3c982360838d3).
- 弃用 `AudioStream::getTimestamp(clockid_t, int64_t, int64_t)`. Same commit as above.
- [添加 Android P 功能](https://github.com/google/oboe/commit/c30bbe603c256f92cdf2876c3122bc5be24b5e3e)

**Other changes**
- Add [API reference](https://google.github.io/oboe/)
- Add unit tests

## 0.11
#### 13th June 2018
更改 `AudioStream` 方法的返回类型为 `ResultWithValue` 在适当情况下. [详细资料](https://github.com/google/oboe/pull/109).

## 0.10
#### 18th January 2018
Add support for input (recording) streams

## 0.9
#### 18th October 2017
初始开发人员预览版本
