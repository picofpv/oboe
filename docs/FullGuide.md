# Oboe 完整指南
Oboe是一个 C++ 库，可轻松在 Android 上构建高性能音频应用程序。应用程序通过将数据读取和写入到流里，来与Oboe通信。

## 音频流

Oboe 在应用程序与 Android 设备上的音频输入和输出之间移动音频数据。 您的应用使用回调函数或通过读取和写入 **Audio Stream** 来传入和传出数据， 由类 AudioStream 表示。读/写调用可以是阻塞的也可以是非阻塞的。

音频流流由以下定义：

*   The **audio device** ：是流中数据的源或接收器（大体可以看作是麦克风、喇叭、耳机、蓝牙什么的）
*   The **sharing mode** ：确定一个流是否具有对音频设备的独占访问权限，否则该音频设备可能会在多个流之间共享。
*   The **format** ：audio stream 的格式，包括数据格式、采样率、每帧的样本数量。

### Audio device

无论是录音还是播放，每个音频流都依附于单个音频设备。

音频设备是充当连续数字音频数据流的源或的硬件接口或虚拟终结点。不要把内置麦克风或耳机这样的 **audio device**  跟正在运行您应用的手机或智能手表这样的 *Android device* 混淆了。

在API 23及更高版本上，您可以使用 `AudioManager` 的 [getDevices()](https://developer.android.com/reference/android/media/AudioManager.html#getDevices(int)) 方法 来发现您 Android 设备上可用的音频设备。 该方法可以返回每个音频设备的 [type](https://developer.android.com/reference/android/media/AudioDeviceInfo.html) 信息

每个音频设备在 Android 设备上都有唯一的 ID 。您可以使用 ID 将音频流绑定到特定的音频设备。但是，在大多数情况下，您可以让 Oboe 选择默认的主要设备，而不必自己指定一个。

附加到流的音频设备确定该流是输入还是输出。 流只能在一个方向上移动数据。定义流时，还可以设置其方向。当您打开流时，Android会检查以确保音频设备和流方向一致。

### 分享模式 Sharing mode

流具有共享模式：

*   `SharingMode::Exclusive` (在API 26+以上可用) 表示流对音频设备上的端点具有独占访问权；该端点此时不能被再任何其他音频流使用。如果专有端点已在使用中，流可能无法访问它。独占模式音频流因为绕过了混音器步骤，可以提供尽可能低的延迟，但他们也更有可能断开连接。您应该在不再需要独占流时立即关闭它们， 以便其他应用可以访问该端点。 不是所有音频设备都能作为独占的端点。 当使用独占流时，由于它们使用不同的端点，因此仍然可以听到其他应用程序发出的系统声音。

![Oboe 独占模式示意图](images/oboe-sharing-mode-exclusive.jpg)

*   `SharingMode::Shared` （共享模式）允许 Oboe 流共享端点。操作系统将混合分配给音频设备上同一终结点的所有共享流。

![Oboe 共享模式示意图](images/oboe-sharing-mode-shared.jpg)


您可以在创建流时显式请求共享模式，尽管不能保证会收到共享模式。 默认情况下，共享模式设置为 `Shared`.独占模式你必须手动设置。

### 音频格式 Audio format

通过流传递的数据具有通常的数字音频属性，在定义流时必须指定这些属性。包括如下：

*   样本格式 Sample format
*   每帧样本数量 Samples per frame
*   采样率 Sample rate

Oboe 允许这些样本格式：

| 音频格式 | C 语言数据类型 | 备注|
| :------------ | :---------- | :---- |
| I16 | int16_t | common 16-bit samples, [Q0.15 format](https://source.android.com/devices/audio/data_formats#androidFormats) -32768 到 +32767|
| Float | float | 范围从 -1.0 to +1.0 |

Oboe 可能会自行执行样本转换。例如，如果某个应用程序正在写入 AudioFormat::Float 数据，但 HAL 会使用 AudioFormat::I16， Oboe 可能会自动转换样本。 转换可以在任何方向发生。 如果您的应用处理音频输入，明智的做法是验证输入格式，并在必要时准备转换数据，如以下示例所示：

```c++
    AudioFormat dataFormat = stream->getDataFormat();
    //... later
    if (dataFormat == AudioFormat::I16) {
         convertFloatToPcm16(...)
    }
```

## 创建音频流

Oboe 库遵循 [生成器设计模式](https://en.wikipedia.org/wiki/Builder_pattern) 并提供了 `AudioStreamBuilder` 这个生成器类。

### 使用 AudioStreamBuilder 设置音频流的配置。

使用与流参数相对应的构建器函数,有如下设置项可用:

```c++
    AudioStreamBuilder streamBuilder;

    streamBuilder.setDeviceId(deviceId);
    streamBuilder.setDirection(direction);
    streamBuilder.setSharingMode(shareMode);
    streamBuilder.setSampleRate(sampleRate);
    streamBuilder.setChannelCount(channelCount);
    streamBuilder.setFormat(format);
    streamBuilder.setPerformanceMode(perfMode);
```

请注意，如果你设置错了，这些方法也不会向你报告错误， 例如未定义的常数或值超出范围。直到打开流的时候才会对设置进行检查。

* 如果您未指定device Id，默认为主要输出设备。
* 如果未指定流方向，则默认为输出流。
* 对于所有参数，您可以显式设置一个值，或者啥也不设置，直接让系统将其设置为最佳值 `kUnspecified`.

为了安全起见，请在创建音频流后检查其状态，有如下3个步骤：

### 1. 打开流 Open the Stream

当你配置完 `AudioStreamBuilder` 后 , 调用 `openStream()` 方法来打开流:

```c++
    // 打开流，如果出问题了，就打印错误
    Result result = streamBuilder.openStream(&stream_);
    if (result != OK){
        __android_log_print(ANDROID_LOG_ERROR,
                            "AudioEngine",
                            "Error opening stream %s",
                            convertToText(result));
    }
```

### 2. 验证流配置和其他属性

打开流后，应验证流的配置。

确保以下属性被设置了。 不过，如果未指定这些属性，Oboe 仍将设置默认值，并应由适当的访问者请求。

* 用来产生数据的回调 **callback**
* 每个回调的帧数 ？ **framesPerCallback**
* 采样率 **sampleRate**
* 通道数 **channelCount**
* 数据格式 **format**
* 方向 **direction**

以下属性可能会因基础流结构而更改 *即使明确设定* 因此，应始终由适当的访问者查询。
属性设置将取决于设备的性能。

* 帧缓冲容量 **bufferCapacityInFrames**
* 共享模式 **sharingMode** (Exclusive 提供最低的延迟)
* 性能模式 **performanceMode**

以下属性仅由基础流设置。它们不能由应用程序设置，但应由适当的访问者查询。

* 突发帧数 **framesPerBurst**

以下属性稍有不同

* **deviceId** （指定设备ID）要在 AAudio (API level >=28) 才有效, 当使用 OpenSL ES API 时设置这个没有用. 即便不设置也没事, 并且就算在 OpenSL ES 里设置了设备 ID 也不会抛出错误。
不管你设了啥，OpenSL ES 仍将使用默认设备。

* mAudioApi 是个只属于生成器的属性, 虽然 AudioStream::getAudioApi() 可用于查询流使用的 API 是哪种（AAudio or OpenSL ES）。
The property set in the builder is not guaranteed, and in general, the API should be chosen by Oboe to allow for best performance and stability considerations. 
由于Oboe被设计为尽可能在两个API之间保持统一，因此通常不需要此属性。

* mBufferSizeInFrames （帧中缓冲区大小） 只能在已经打开的流上设置 (这与建造者该于事前设置的特征相反), 因为它取决于运行时行为。
使用的实际大小可能不是要求的大小。Oboe 或基础 API 会把大小限制在0和缓冲区容量之间。
还可以进一步限制缩小以减少特定设备上的毛刺。
使用 OpenSL ES 回调时不支持此功能。

流的许多属性可能会变化 (不管你是否明确的设置了它们) ，这取决于音频设备和 Android 设备的性能。
如果需要了解这些值，则必须在流打开后使用访问器查询它们。
另外，流被授予的基础参数对于了解是否未指定它们很有用。
作为良好的防御性编程，最好应在使用流之前检查流的一系列配置情况。

有一些函数可以检索与每个构建器设置相对应的流设置：

| AudioStreamBuilder **set()** 方法 | AudioStream **get()** 方法 |
| :------------------------ | :----------------- |
| `setCallback()` |  `getCallback()` |
| `setDirection()` | `getDirection()` |
| `setSharingMode()` | `getSharingMode()` |
| `setPerformanceMode()` | `getPerformanceMode()` |
| `setSampleRate()` | `getSampleRate()` |
| `setChannelCount()` | `getChannelCount()` |
| `setFormat()` | `getFormat()` |
| `setBufferCapacityInFrames()` | `getBufferCapacityInFrames()` |
| `setFramesPerCallback()` | `getFramesPerCallback()` |
|  --  | `getFramesPerBurst()` |
| `setDeviceId()` (在 OpenSL ES 无效) | `getDeviceId()` |
| `setAudioApi()` (主要用于调试) | `getAudioApi()` |

API 28 中添加了以下 AudioStreamBuilder 字段，以指定有关设备音频流的其他信息。
当前，它们对流的影响很小，但是设置它们可以帮助应用程序与其他服务更好地交互。

有关更多信息，请参见： [用法/内容类型](https://source.android.com/devices/audio/attributes).
设备可以使用 InputPreset 处理输入流（例如，增益控制）。默认情况下，它设置为 VoiceRecognition ，该功能针对低延迟进行了优化。

* `setUsage(oboe::Usage usage)`  - 用于创建流。
* `setContentType(oboe::ContentType contentType)` - 流所载内容的类型。
* `setInputPreset(oboe::InputPreset inputPreset)` - 音频输入的录制配置。
* `setSessionId(SessionId sessionId)` - 分配 SessionID 以连接到 Java Audio Effects API。

## 使用音频流

### 使用状态转换

Oboe 流通常处于以下五个稳定状态之一（本节末尾描述了错误状态 Disconnected ）：

*   Open
*   Started
*   Paused
*   Flushed
*   Stopped

仅当数据流处于 **Started** 状态时，数据才流经该数据流。要在状态之间移动流，请使用下列状态转换函数：

```c++
    Result result;
    result = stream->requestStart();
    result = stream->requestStop();
    result = stream->requestPause();
    result = stream->requestFlush();
```

> 请注意，您只能在输出流上请求暂停或刷新：

这些函数是异步的，并且状态更改不会立即发生。
当您请求状态更改时，流将移至相应的瞬态之一：

*   Starting
*   Pausing
*   Flushing
*   Stopping
*   Closing

下面的示意图里的圆角矩形表示稳定态，虚线矩形表示瞬态。
虽然没有显示, 但您可以从任何状态调用 `close()`

![Oboe 的生命周期](images/oboe-lifecycle.png)

Oboe 不提供回调来提醒您状态更改。只有一种特殊情况, `AudioStream::waitForStateChange()` 可用于等待状态更改。
请注意，大多数应用程序无需调用`waitForStateChange（）`，只在它们需要时更改状态。

该函数不会自行检测状态变化, 并且不等待特定状态。 它会等到当前状态与您指定的状态不同时才触发。

例如，在请求暂停后，流应立即进入过渡状态 “Pausing”，并在稍后到达 “Pausing” 状态-尽管无法保证会。
由于您不能等待暂停状态, use `waitForStateChange()` to wait for *除暂停外的任何状态* . 以下使用方式：

```c++
StreamState inputState = StreamState::Pausing;
StreamState nextState = StreamState::Uninitialized;
int64_t timeoutNanos = 100 * kNanosPerMillisecond;
result = stream->requestPause();
result = stream->waitForStateChange(inputState, &nextState, timeoutNanos);
```

如果流的状态不是 “暂停” (the `inputState`, 我们假设是
调用时的当前状态), 该函数立即返回. 除此以外, 它
阻止，直到状态不再为“暂停”或超时到期为止。 函数返回时, 参数 `nextState` 显示流的当前状态。

您可以在调用请求开始后使用相同的技术, 停止或清空,使用相应的瞬态作为 `inputState` 。 Do not call
`waitForStateChange()` after calling `AudioStream::close()` since the underlying stream resources
will be deleted as soon as it closes. And do not call `close()`
while `waitForStateChange()` is running in another thread.

### 读取和写入音频流

有两种方法可以将数据移入或移出流。
1) 从流中直接 `读取` 或 `写入` 流。
2) 为已准备好的流指定回调对象。 (这个可能更常用,因为延迟最低)

回调技术提供了最低的延迟性能，因为回调代码可以在高优先级的线程中运行。
因此, 尝试在没有音频回调的情况下打开低延迟输出流(比如使用写操作) 可能会导致非低延迟流。

当您不需要低延迟时，读/写技术可能会更容易。 或者，在同时进行输入和输出时，通常使用回调进行输出，然后仅从输入流中进行非阻塞读取。然后，您可以在一个高优先级线程中获得输入和输出数据。

流启动后，您可以使用以下方法对其进行读写
`AudioStream::read(buffer, numFrames, timeoutNanos)`
和
`AudioStream::write(buffer, numFrames, timeoutNanos)`.

对于传输指定数量的帧的阻塞读取或写入，请将timeoutNanos设置为大于零。对于非阻塞呼叫，请将timeoutNanos设置为零。在这种情况下，结果是传输的实际帧数。

读取输入时，应验证读取的帧数正确。
否则，缓冲区可能包含未知数据，可能会导致音频故障。
您可以使用零填充缓冲区以创建无声区：

```c++
    Result result = stream.read(audioData, numFrames, timeout);
    if (result < 0) {
        // Error!
    }
    if (result != numFrames) {
        // 用零填充缓冲区
        // memset,memcopy,都是常用的操作
        memset(static_cast<sample_type*>(audioData) + result * samplesPerFrame, 0 , (numFrames - result) * stream.getBytesPerFrame());
    }
```

您可以在启动流之前通过向其写入数据或保持静默来填充流的缓冲区。这必须在timeoutNanos设置为零的非阻塞调用中完成。

缓冲区中的数据必须与 `stream.getDataFormat()` 返回的数据格式匹配。

### close 关闭音频流

使用完流后，应该将其关闭：

    stream->close();

正在将流写入,或从另一个线程读取时请不要关闭流，因为这将导致您的应用程序崩溃。
而且关闭流后，除了查询其属性外，不应调用其任何方法。

### Disconnected 断开音频流

如果发生以下事件之一，则音频流可以随时断开连接：

*   关联的音频设备不再连接时 (例如拔下耳机时).
*   内部发生错误时。
*   音频设备不再是主要音频设备时。

当一个流断开连接时，它的状态为 `Disconnected`，并且对 `write()` 或其他函数的调用将返回 `Result::ErrorDisconnected`。当流断开连接时，您只能关闭它。

如果您需要在音频设备断开连接时收到通知， 写一个扩展自 `AudioStreamCallback` 的类，然后使用 `builder.setCallback(yourCallbackClass)` 注册您的类。
如果您注册了回调， 那么如果流断开连接，它将自动在单独的线程中关闭流。
请注意，注册此回调将同时启用对 普通数据和错误的回调。所以 `onAudioReady()` 也将被调用。请参阅下面的 `高优先级回调` 部分。

您的回调可以实现以下方法（在单独的线程中调用）：

* `onErrorBeforeClose(stream, error)` - 当流已断开但尚未关闭时调用
  因此您仍然可以引用 underlying 流 (e.g.`getXRunCount()`).
您还可以通知可能正在调用该流的任何其他线程停止这样做。
不要在此回调中删除流或修改其流状态。
* `onErrorAfterClose(stream, error)` - 当流已被 Oboe 停止并关闭时调用，因此该流无法使用，并且调用 `getState()` 将返回 closed。
在此回调期间，可以查询流属性（由构建器请求的属性）以及写入和读取的帧。
可以在此方法结束时删除该流（只要在其他线程中未引用该流）。
引用底层流的方法不应该被调用（例如，getTimestamp（），getXRunCount（），read（），write（）等）。
打开单独的流也是此回调的有效用法，特别是如果收到的错误是 `Error::Disconnected`。
但是，重要的是要注意，新的音频设备可能具有与断开连接的流完全不同的属性。


## 优化性能

您可以使用特殊的高优先级线程来优化音频应用程序的性能。

### 使用高优先级 callback

如果您的应用从普通线程读取或写入音频数据，则它可能会被抢占或出现时序抖动。这可能会导致音频故障。
使用较大的缓冲区可能会防止此类故障，但是较大的缓冲区也会引入较长的音频延迟。
对于要求低延迟的应用程序，音频流可以使用异步回调 callback 函数在应用程序之间传输数据。
callback 回调在具有更高性能的高优先级线程中运行。

您的代码可以通过实现虚拟类 `AudioStreamCallback` 来访问回调机制。
流会定期去执行 callback 里的 `onAudioReady()` (回调函数) 为下一次突发获取数据。

```c++
    class AudioEngine : AudioStreamCallback {
    public:
        DataCallbackResult AudioEngine::onAudioReady(
                AudioStream *oboeStream,
                void *audioData,
                int32_t numFrames){
            oscillator_->render(static_cast<float *>(audioData), numFrames);
            return DataCallbackResult::Continue;
        }

        bool AudioEngine::start() {
            ...
            // 注册回调
            streamBuilder.setCallback(this);
        }
    private:
        // 应用数据
        Oscillator* oscillator_;
    }
```

注意，回调必须使用 `setCallback` 在流上注册。 任何特定于应用程序的数据（在上面这段示例里是 `oscillator_` ）都可以包含在类本身内。

回调函数不应在调用它的流上执行读取或写入操作。如果回调属于输入流， 您的代码应处理 `audioData` 缓冲区中提供的数据 (指定第二个参数). 如果回调属于输出流， 您的代码应将数据放入缓冲区。

在回调中可以处理多个流。您可以使用一个流作为主流， 并在类的私有数据中传递指向其他流的指针。注册主流的回调。然后在其他流上使用非阻塞 I/O。 这里有个将输入流传递到输出流的往返回调的示例。主调用流是输出
流。输入流包含在该类中。

回调从输入流中进行非阻塞读取，将数据放入输出流的缓冲区中。(形成一个耳反的循环效果)

```c++
    class AudioEngine : AudioStreamCallback {
    public:
        oboe_data_callback_result_t AudioEngine::onAudioReady(
                AudioStream *oboeStream,
                void *audioData,
                int32_t numFrames) {
            const int64_t timeoutNanos = 0; // 非阻塞读取
            auto result = recordingStream->read(audioData, numFrames, timeoutNanos);
            // result has type ResultWithValue<int32_t>, which for convenience is coerced
            // to a Result type when compared with another Result.
            if (result == Result::OK) {
                if (result.value() < numFrames) {
                    // 用无声来替换丢失的数据
                    memset(static_cast<sample_type*>(audioData) + result.value() * samplesPerFrame, 0,
                        (numFrames - result.value()) * oboeStream->getBytesPerFrame());
                }
                return DataCallbackResult::Continue;
            }
            return DataCallbackResult::Stop;
        }
        bool AudioEngine::start() {
            ...
            streamBuilder.setCallback(this);
        }
        void setRecordingStream(AudioStream *stream) {
          recordingStream = stream;
        }
    private:
        AudioStream *recordingStream;
    }
```

注意，在此示例中，假定输入和输出流具有相同数量的通道，格式和采样率。流的格式可能不匹配-只要代码正确处理翻译即可。

#### callback 回调的注意事项
您永远不要执行可能会阻塞 `onAudioReady` 内部的操作，一定要保证流畅。阻止操作的示例包括：

- 使用分配内存, 例如, `malloc()` or `new`
- 文件操作，例如打开、关闭、读、写文件
- 网络操作，例如流媒体
- 使用互斥或​​其他同步 primitives
- `sleep`
- 停止或关闭流
- 在调用它的流上调用 read() 或 write() 

可以调用以下方法：

- AudioStream::get*()
- oboe::convertResultToText()

### 设定性能模式

每个 AudioStream 都有一个 **performance mode** 属性,这会对您应用的行为产生重大影响。共有三种模式：

* `PerformanceMode::None` 是默认模式。它使用基本流来平衡延迟和节能。
* `PerformanceMode::LowLatency` 使用较小的缓冲区和优化的数据路径以减少延迟。
* `PerformanceMode::PowerSaving` 使用较大的内部缓冲区和数据路径来权衡延迟以降低功耗。

您可以通过调用 `setPerformanceMode()` 选择性能模式 ,
并通过调用 `getPerformanceMode()` 了解当前性能模式 .

如果在应用程序中低延迟比节电更为重要， 采用 `PerformanceMode::LowLatency`.
这对于互动性强的应用程序很有用，例如游戏或键盘合成器。

如果在您的应用程序中节能比低延迟更重要， 采用 `PerformanceMode::PowerSaving`.
这对于播放先前生成的音乐的应用程序来说很典型，例如流音频或MIDI文件播放器。

在 Oboe 的当前版本中，为了获得尽可能低的延迟，您必须使用 `PerformanceMode::LowLatency` 性能模式 along with a high-priority callback. Follow this example:

```c++
// 创建一个回调对象
MyOboeStreamCallback myCallback;

// 创建一个流构建器
AudioStreamBuilder builder;
builder.setCallback(myCallback);
builder.setPerformanceMode(PerformanceMode::LowLatency);

// 用它来创建流
AudioStream *stream;
builder.openStream(&stream);
```

## 线程安全性说明

Oboe API 并不是完全 [线程安全](https://en.wikipedia.org/wiki/Thread_safety) 的.
您不能一次从一个以上的线程中同时调用某些Oboe函数。
这是因为 Oboe 避免使用互斥锁， 这可能导致线程抢占和故障。

为了安全起见，请勿调用 `waitForStateChange()` 或从两个不同的线程读取或写入操作同一个流。 同样，请勿在一个线程正读取或写入流时在另一个线程中把这个流关闭。

调用返回流设置， 例如 `AudioStream::getSampleRate()` 和 `AudioStream::getChannelCount()`, 是线程安全的。

这些调用也是线程安全的：

* `convertToText()`
* `AudioStream::get*()` 除了 `getTimestamp()` 和 `getState()`

<b>Note:</b> 当流使用回调函数时，可以安全地从回调线程进行读取/写入，同时还关闭流
从它运行所在的线程开始。


## 代码示例

代码示例位于 [samples 目录](../samples).

## 已知问题

The following methods are defined, but will return `Result::ErrorUnimplemented` for OpenSLES streams:

* `getFramesRead()`
* `getFramesWritten()`
* `getTimestamp()`

Additionally, `setDeviceId()` will not be respected by OpenSLES streams.
