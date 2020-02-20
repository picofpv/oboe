Android audio 的历史记载
===

各种Android版本的重要音频功能，错误，修复和解决方法的列表。

### 10.0 Q - API 29
- Fixed: 设置传统输入流的容量<4096可以防止使用FAST路径。 https://github.com/google/oboe/issues/183
- Add InputPreset:语音性能低延迟录音。

### 9.0 Pie - API 28 (August 6, 2018)
- AAudio 增加对 setUsage(), setSessionId(), setContentType(), setInputPreset() 几种设置的支持，可用于 builders.
- Regression bug: [AAudio] 没有为MMAP流触发耳机断开事件。https://github.com/google/oboe/issues/252
- AAudio 具有LOW_LATENCY的输入流将使用INT16打开FAST路径，并在需要时将数据转换为FLOAT。See: https://github.com/google/oboe/issues/276

### 8.1 Oreo MR1 - API 27
- Oboe 默认情况下使用AAudio API。
- AAudio MMAP 在Pixel设备上启用了数据路径。 PerformanceMode :: Exclusive支持。
- Fixed: [AAudio] RefBase问题
- Fixed: 请求立体声记录流可能导致次优延迟。

### 8.0 Oreo - API 26 (August 21, 2017)
- [AAudio API introduced](https://developer.android.com/ndk/guides/audio/aaudio/aaudio)
- Bug: RefBase问题导致流关闭后崩溃。这就是为什么不建议在8.0中使用AAudio API的原因。Oboe将在8.0（API 26)及更早版本的里使用OpenSL ES。
  https://github.com/google/oboe/issues/40
- Bug: 请求立体声记录流可能导致次优延迟。 [细节](https://issuetracker.google.com/issues/68666622)

### 7.1 Nougat MR1 - API 25
- OpenSL添加了对PerformanceMode的设置和查询的支持。

### 7.0 Nougat - API 24 (August 22, 2016)
- OpenSL 方法 `acquireJavaProxy` 添加, 允许获取与播放关联的Java AudioTrack对象 (允许欠载计数).

### 6.0 Marshmallow - API 23 (October 5, 2015)
- 支持浮点录制。 但是它不允许FAST“低延迟”路径。
- [引入MIDI API](https://developer.android.com/reference/android/media/midi/package-summary)
- API 23模拟器上的声音输出中断

### 5.0 Lollipop - API 21 (November 12, 2014)
- 支持浮点播放。





