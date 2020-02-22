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

## 我为音频流请求了 `PerformanceMode::LowLatency` 低延迟音频流, 但为什么没有成功 ?
通常如果你调用 `builder.setPerformanceMode(PerformanceMode::LowLatency)` 并且不指定其他流属性，您将获得 `LowLatency` 流. 而没有成功的最常见原因是：

- 您正在打开输出流，但未指定 **callback** 回调.
- You requested a **sample** rate which does not match the audio device's native sample rate. For playback streams, this means the audio data you write into the stream must be resampled before it's sent to the audio device. For recording streams, the  audio data must be resampled before you can read it. In both cases the resampling process (performed by the Android audio framework) adds latency and therefore providing a `LowLatency` stream is not possible. To avoid the resampler on API 26 and below you can specify a default value for the sample rate [as detailed here](https://github.com/google/oboe/blob/master/docs/GettingStarted.md#obtaining-optimal-latency).  Or you can use the [new resampler](https://google.github.io/oboe/reference/classoboe_1_1_audio_stream_builder.html#af7d24a9ec975d430732151e5ee0d1027) in Oboe, which allows the lower level code to run at the optimal rate and provide lower latency.
- If you request **AudioFormat::Float on an Input** stream before Android 9.0 then you will **not** get a FAST track. You need to either request AudioFormat::Int16 or [enable format conversion by Oboe](https://google.github.io/oboe/reference/classoboe_1_1_audio_stream_builder.html#a7ec5f427cd6fe55cb1ce536ff0cbb4d2).
- The audio **device** does not support `LowLatency` streams, for example Bluetooth. 
- You requested a **channel count** which is not supported natively by the audio device. On most devices and Android API levels it is possible to obtain a `LowLatency` stream for both mono and stereo, however, there are a few exceptions, some of which are listed [here](https://github.com/google/oboe/blob/master/docs/AndroidAudioHistory.md). 
- The **maximum number** of `LowLatency` streams has been reached. This could be by your app, or by other apps. This is often caused by opening multiple playback streams for different "tracks". To avoid this open a single audio stream and perform 
your own mixing in the app. 
- You are on Android 7.0 or below and are receiving `PerformanceMode::None`. The ability to query the performance mode of a stream was added in Android 7.1 (Nougat MR1). Low latency streams (aka FAST tracks) _are available_ on Android 7.0 and below but there is no programmatic way of knowing whether yours is one. [Question on StackOverflow](https://stackoverflow.com/questions/56828501/does-opensl-es-support-performancemodelowlatency/5683499)

## My question isn't listed, where can I ask it?
Please ask questions on [Stack Overflow](https://stackoverflow.com/questions/ask) with the [Oboe tag](https://stackoverflow.com/tags/oboe) or in the GitHub Issues tab.
