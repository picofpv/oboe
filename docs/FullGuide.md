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

Data only flows through a stream when the stream is in the Started state. To
move a stream between states, use one of the functions that request a state
transition:

    Result result;
    result = stream->requestStart();
    result = stream->requestStop();
    result = stream->requestPause();
    result = stream->requestFlush();

Note that you can only request pause or flush on an output stream:

These functions are asynchronous, and the state change doesn't happen
immediately. When you request a state change, the stream moves toone of the
corresponding transient states:

*   Starting
*   Pausing
*   Flushing
*   Stopping
*   Closing

The state diagram below shows the stable states as rounded rectangles, and the transient states as dotted rectangles.
Though it's not shown, you can call `close()` from any state

![Oboe Lifecycle](images/oboe-lifecycle.png)

Oboe doesn't provide callbacks to alert you to state changes. One special
function,
`AudioStream::waitForStateChange()` can be used to wait for a state change.
Note that most apps will not need to call `waitForStateChange()` and can just
request state changes whenever they are needed.

The function does not detect a state change on its own, and does not wait for a
specific state. It waits until the current state
is *different* than `inputState`, which you specify.

For example, after requesting to pause, a stream should immediately enter
the transient state Pausing, and arrive sometime later at the Paused state - though there's no guarantee it will.
Since you can't wait for the Paused state, use `waitForStateChange()` to wait for *any state
other than Pausing*. Here's how that's done:

```
StreamState inputState = StreamState::Pausing;
StreamState nextState = StreamState::Uninitialized;
int64_t timeoutNanos = 100 * kNanosPerMillisecond;
result = stream->requestPause();
result = stream->waitForStateChange(inputState, &nextState, timeoutNanos);
```


If the stream's state is not Pausing (the `inputState`, which we assumed was the
current state at call time), the function returns immediately. Otherwise, it
blocks until the state is no longer Pausing or the timeout expires. When the
function returns, the parameter `nextState` shows the current state of the
stream.

You can use this same technique after calling request start, stop, or flush,
using the corresponding transient state as the inputState. Do not call
`waitForStateChange()` after calling `AudioStream::close()` since the underlying stream resources
will be deleted as soon as it closes. And do not call `close()`
while `waitForStateChange()` is running in another thread.

### Reading and writing to an audio stream

There are two ways to move data in or out of a stream.
1) Read from or write directly to the stream.
2) Specify a callback object that will get called when the stream is ready.

The callback technique offers the lowest latency performance because the callback code can run in a high priority thread.
Also, attempting to open a low latency output stream without an audio callback (with the intent to use writes)
may result in a non low latency stream.

The read/write technique may be easier when you do not need low latency. Or, when doing both input and output, it is common to use a callback for output and then just do a non-blocking read from the input stream. Then you have both the input and output data available in one high priority thread.

After the stream is started you can read or write to it using the methods
`AudioStream::read(buffer, numFrames, timeoutNanos)`
and
`AudioStream::write(buffer, numFrames, timeoutNanos)`.

For a blocking read or write that transfers the specified number of frames, set timeoutNanos greater than zero. For a non-blocking call, set timeoutNanos to zero. In this case the result is the actual number of frames transferred.

When you read input, you should verify the correct number of
frames was read. If not, the buffer might contain unknown data that could cause an
audio glitch. You can pad the buffer with zeros to create a
silent dropout:

    Result result = stream.read(audioData, numFrames, timeout);
    if (result < 0) {
        // Error!
    }
    if (result != numFrames) {
        // pad the buffer with zeros
        memset(static_cast<sample_type*>(audioData) + result * samplesPerFrame, 0,
               (numFrames - result) * stream.getBytesPerFrame());
    }

You can prime the stream's buffer before starting the stream by writing data or silence into it. This must be done in a non-blocking call with timeoutNanos set to zero.

The data in the buffer must match the data format returned by `stream.getDataFormat()`.

### Closing an audio stream

When you are finished using a stream, close it:

    stream->close();

Do not close a stream while it is being written to or read from another thread as this will cause your app to crash. After you close a stream you should not call any of its methods except for quering it properties.

### Disconnected audio stream

An audio stream can become disconnected at any time if one of these events happens:

*   The associated audio device is no longer connected (for example when headphones are unplugged).
*   An error occurs internally.
*   An audio device is no longer the primary audio device.

When a stream is disconnected, it has the state "Disconnected" and calls to `write()` or other functions will return `Result::ErrorDisconnected`.  When a stream is disconnected, all you can do is close it.

If you need to be informed when an audio device is disconnected, write a class
which extends `AudioStreamCallback` and then register your class using `builder.setCallback(yourCallbackClass)`.
If you register a callback, then it will automatically close the stream in a separate thread if the stream is disconnected.
Note that registering this callback will enable callbacks for both data and errors. So `onAudioReady()` will be called. See the "high priority callback" section below.

Your callback can implement the following methods (called in a separate thread): 

* `onErrorBeforeClose(stream, error)` - called when the stream has been disconnected but not yet closed,
  so you can still reference the underlying stream (e.g.`getXRunCount()`).
You can also inform any other threads that may be calling the stream to stop doing so.
Do not delete the stream or modify its stream state in this callback.
* `onErrorAfterClose(stream, error)` - called when the stream has been stopped and closed by Oboe so the stream cannot be used and calling getState() will return closed. 
During this callback, stream properties (those requested by the builder) can be queried, as well as frames written and read.
The stream can be deleted at the end of this method (as long as it not referenced in other threads).
Methods that reference the underlying stream should not be called (e.g. `getTimestamp()`, `getXRunCount()`, `read()`, `write()`, etc.).
Opening a seperate stream is also a valid use of this callback, especially if the error received is `Error::Disconnected`. 
However, it is important to note that the new audio device may have vastly different properties than the stream that was disconnected.


## Optimizing performance

You can optimize the performance of an audio application by using special high-priority threads.

### Using a high priority callback

If your app reads or writes audio data from an ordinary thread, it may be preempted or experience timing jitter. This can cause audio glitches.
Using larger buffers might guard against such glitches, but a large buffer also introduces longer audio latency.
For applications that require low latency, an audio stream can use an asynchronous callback function to transfer data to and from your app.
The callback runs in a high-priority thread that has better performance.

Your code can access the callback mechanism by implementing the virtual class
`AudioStreamCallback`. The stream periodically executes `onAudioReady()` (the
callback function) to acquire the data for its next burst.

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
            // register the callback
            streamBuilder.setCallback(this);
        }
    private:
        // application data
        Oscillator* oscillator_;
    }


Note that the callback must be registered on the stream with `setCallback`. Any
application-specific data (such as `oscillator_` in this case)
can be included within the class itself.

The callback function should not perform a read or write on the stream that invoked it. If the callback belongs to an input stream, your code should process the data that is supplied in the audioData buffer (specified as the second argument). If the callback belongs to an output stream, your code should place data into the buffer.

It is possible to process more than one stream in the callback. You can use one stream as the master, and pass pointers to other streams in the class's private data. Register a callback for the master stream. Then use non-blocking I/O on the other streams.  Here is an example of a round-trip callback that passes an input stream to an output stream. The master calling stream is the output
stream. The input stream is included in the class.

The callback does a non-blocking read from the input stream placing the data into the buffer of the output stream.

    class AudioEngine : AudioStreamCallback {
    public:

        oboe_data_callback_result_t AudioEngine::onAudioReady(
                AudioStream *oboeStream,
                void *audioData,
                int32_t numFrames) {
            const int64_t timeoutNanos = 0; // for a non-blocking read
            auto result = recordingStream->read(audioData, numFrames, timeoutNanos);
            // result has type ResultWithValue<int32_t>, which for convenience is coerced
            // to a Result type when compared with another Result.
            if (result == Result::OK) {
                if (result.value() < numFrames) {
                    // replace the missing data with silence
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


Note that in this example it is assumed the input and output streams have the same number of channels, format and sample rate. The format of the streams can be mismatched - as long as the code handles the translations properly.

#### Callback do's and don'ts 
You should never perform an operation which could block inside `onAudioReady`. Examples of blocking operations include:

- allocate memory using, for example, malloc() or new
- file operations such as opening, closing, reading or writing
- network operations such as streaming
- use mutexes or other synchronization primitives
- sleep
- stop or close the stream
- Call read() or write() on the stream which invoked it

The following methods are OK to call:

- AudioStream::get*()
- oboe::convertResultToText()

### Setting performance mode

Every AudioStream has a *performance mode* which has a large effect on your app's behavior. There are three modes:

* `PerformanceMode::None` is the default mode. It uses a basic stream that balances latency and power savings.
* `PerformanceMode::LowLatency` uses smaller buffers and an optimized data path for reduced latency.
* `PerformanceMode::PowerSaving` uses larger internal buffers and a data path that trades off latency for lower power.

You can select the performance mode by calling `setPerformanceMode()`,
and discover the current mode by calling `getPerformanceMode()`.

If low latency is more important than power savings in your application, use `PerformanceMode::LowLatency`.
This is useful for apps that are very interactive, such as games or keyboard synthesizers.

If saving power is more important than low latency in your application, use `PerformanceMode::PowerSaving`.
This is typical for apps that play back previously generated music, such as streaming audio or MIDI file players.

In the current version of Oboe, in order to achieve the lowest possible latency you must use the `PerformanceMode::LowLatency` performance mode along with a high-priority callback. Follow this example:

```
// Create a callback object
MyOboeStreamCallback myCallback;

// Create a stream builder
AudioStreamBuilder builder;
builder.setCallback(myCallback);
builder.setPerformanceMode(PerformanceMode::LowLatency);

// Use it to create the stream
AudioStream *stream;
builder.openStream(&stream);
```

## 线程安全性说明

The Oboe API is not completely [thread safe](https://en.wikipedia.org/wiki/Thread_safety).
You cannot call some of the Oboe functions concurrently from more than one thread at a time.
This is because Oboe avoids using mutexes, which can cause thread preemption and glitches.

To be safe, don't call `waitForStateChange()` or read or write to the same stream from two different threads. Similarly, don't close a stream in one thread while reading or writing to it in another thread.

Calls that return stream settings, like `AudioStream::getSampleRate()` and `AudioStream::getChannelCount()`, are thread safe.

These calls are also thread safe:

* `convertToText()`
* `AudioStream::get*()` except for `getTimestamp()` and `getState()`

<b>Note:</b> When a stream uses a callback function, it's safe to read/write from the callback thread while also closing the stream
from the thread in which it is running.


## Code samples

Code samples are available in the [samples folder](../samples).

## 已知问题

The following methods are defined, but will return `Result::ErrorUnimplemented` for OpenSLES streams:

* `getFramesRead()`
* `getFramesWritten()`
* `getTimestamp()`

Additionally, `setDeviceId()` will not be respected by OpenSLES streams.
