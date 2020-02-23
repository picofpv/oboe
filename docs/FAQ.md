# 经常问的问题 (FAQ)

## 我可以将音频数据从Java写入Oboe吗 ?

Oboe是使用 C++ 编写的使用Android NDK的本机库。 要将数据从 Java 移到 C++ ，可以使用 [JNI](https://developer.android.com/training/articles/perf-jni). 
而如果您真的打算使用 Java 去生成音频，更好的实践是直接去使用 [Java 的 AudioTrack class](https://developer.android.com/reference/android/media/AudioTrack). 可以使用`AudioTrack.Builder` 方法以低延迟创建 [`setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)`](https://developer.android.com/reference/android/media/AudioTrack#PERFORMANCE_MODE_LOW_LATENCY).

您可以像使用Oboe一样动态调整流的延迟，方法是使用 [`setBufferSizeInFrames(int)`](https://developer.android.com/reference/android/media/AudioTrack.html#setBufferSizeInFrames(int))
另外，您可以将阻塞写入与Java AudioTrack一起使用，并且仍然获得低延迟流。
Oboe需要回调来获得低延迟流，并且不适用于Java。

注意 [`AudioTrack.PERFORMANCE_MODE_LOW_LATENCY`](https://developer.android.com/reference/android/media/AudioTrack#PERFORMANCE_MODE_LOW_LATENCY) 是在 API 26 时添加的功能, 而 API 24 或 25 要用 [`AudioAttributes.FLAG_LOW_LATENCY`](https://developer.android.com/reference/kotlin/android/media/AudioAttributes#flag_low_latency). 现已弃用，但仍可与更高版本的API一起使用。

(话说回来,你用 Oboe 不就是为了低延迟么?用 java 的话,延迟又高回去了)

## 我可以使用 Oboe 播放压缩的音频文件么，例如MP3或AAC ?
Oboe 仅适用于 PCM 数据. 它不包括任何提取或解码类。 然而 [RhythmGame sample](https://github.com/google/oboe/tree/master/samples/RhythmGame) 这个示例里 包括了 NDK 和 FFmpeg 的提取器。

有关在应用程序中使用 FFmpeg 的更多信息 [查看这篇文章](https://medium.com/@donturner/using-ffmpeg-for-faster-audio-decoding-967894e94e71).

## Android Studio 找不到 Oboe 符号，我该如何解决?
首先，请确保您的项目成功构建。 主要要做的是确保 Oboe 的 include 路径在项目的 `CMakeLists.txt` 文件里被正确的设置了. [完整说明在这里](https://github.com/google/oboe/blob/master/docs/GettingStarted.md#2-update-cmakeliststxt).

如果仍不能解决问题，请尝试以下操作：

1) 通过 File->Invalidate Caches / Restart 使 Android Studio 缓存无效 
2) 删除内容 `$HOME/Library/Caches/AndroidStudio<version>`

我们已经收到了有关此情况的几份报告，并且渴望了解根本原因。如果您遇到这种情况，请向您的 Android Studio 版本提出问题，我们将进行进一步调查。

## 我想把音频流设置为 `PerformanceMode::LowLatency` 获得低延迟, 为什么失败了?
通常如果你调用 `builder.setPerformanceMode(PerformanceMode::LowLatency)` 并且不指定其他流属性，您将获得 `LowLatency` 流. 而没有成功的最常见原因是：

1. 您正在打开输出流，但未指定 **callback** 回调.

2. 您要求一个 **sample rate** 与音频设备的本机采样率不匹配。对于播放流，这意味着您写入流中的音频数据必须先重新采样，然后再发送到音频设备。对于录制流，必须先对音频数据进行重新采样才能读取。 在这两种情况下，重新采样过程（由Android音频框架执行）都会增加延迟，因此无法提供“ LowLatency”流。 为避免使用API​​ 26及以下版本的重新采样器，您可以为采样率指定默认值 [详细说明](https://github.com/google/oboe/blob/master/docs/GettingStarted.md#obtaining-optimal-latency).  或者您可以使用 Oboe 自带的 [新的重采样器](https://google.github.io/oboe/reference/classoboe_1_1_audio_stream_builder.html#af7d24a9ec975d430732151e5ee0d1027)  , 这允许较低级别的代码以最佳速率运行并提供较低的延迟。

3. 如果您在Android 9.0之前的 input 流上请求 **AudioFormat::Float** （这个是在9.0之后才加入的），那么您将 **不会** 获得 FAST track。 您需要请求 AudioFormat::Int16 格式，或者 [启用Oboe的格式转换](https://google.github.io/oboe/reference/classoboe_1_1_audio_stream_builder.html#a7ec5f427cd6fe55cb1ce536ff0cbb4d2).

4. **音频设备**不支持**低延迟**流，例如蓝牙设备。

5. 您请求了音频设备本身不支持的 **通道数**。 在大多数设备和 Android API 级别上，可以为单声道和立体声获取 **LowLatency** 流，但是，有一些例外，其中有些例外 [比如这样](https://github.com/google/oboe/blob/master/docs/AndroidAudioHistory.md). 

6. **LowLatency** 流的 **最大数量** 已达到。 这通常是由于为不同的 **tracks** 打开多个播放流而引起的。为避免这种情况，请只打开一个音频流，然后在应用程序中执行自己的混音。

7. 您使用的是Android 7.0或更低版本，并且正在接收 **PerformanceMode::None**。在Android 7.1（Nougat MR1）中添加了查询流的性能模式的功能。在Android 7.0及更低版本上，低延迟流（又名FAST track）可用，但是没有编程的方式来知道你的这个是否属于低延迟流。[在StackOverflow的提问和答案](https://stackoverflow.com/questions/56828501/does-opensl-es-support-performancemodelowlatency/5683499)

## 我的问题未列出，请问哪里？
请在 [Stack Overflow](https://stackoverflow.com/questions/ask) 使用 [Oboe tag](https://stackoverflow.com/tags/oboe)提问，或在GitHub Issues 里提问。
