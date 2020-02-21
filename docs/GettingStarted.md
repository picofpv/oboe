# 1. 入门
> 开始使用Oboe的最简单方法是通过向现有 Android Studio 项目添加一些步骤来从源代码构建它。

## 创建具有 Native 支持的 Android 应用
+ 创建一个新项目: `File > New > New Project`
+ 选择项目类型时，选择 Native C++
+ 完成配置项目

## 将 Oboe 添加到您的项目

### 1. 克隆github 上的 Oboe 库
首先克隆 Oboe 库的[最新稳定版本](https://github.com/google/oboe/releases/) , 例如:

    git clone -b 1.3-stable https://github.com/google/oboe

**记下 Oboe 克隆的路径-您很快将需要它**

如果您使用 git 作为版本控制系统， 考虑将 Oboe 添加为[submodule](https://gist.github.com/gitaarik/8735255)  (在你的
 cpp 目录下)

    git submodule add https://github.com/google/oboe

![20200221234241.png](https://raw.githubusercontent.com/picofpv/picMarkdown/master/picGo/20200221234241.png)

这样可以更轻松地将 Oboe 的更新集成到您的应用中(虽然这样添加 submodule 会增加很多无用的示例代码）。

### 2. 更新 CMakeLists.txt
打开您应用的 `CMakeLists.txt`. 这文件可以在 `External Build Files` 在Android的项目视图下面找到. 如果您没有 `CMakeLists.txt` 文件，您将需要 [为您的项目添加 C++ 支持](https://developer.android.com/studio/projects/add-native-code).

![CMakeLists.txt location in Android Studio](images/cmakelists-location-in-as.png "CMakeLists.txt location in Android Studio")

现在，将以下命令添加到 `CMakeLists.txt` 文件里. **记住要用刚才我要你记下的 Oboe 路径去更新 `**PATH TO OBOE**` **:

    # 设置 Oboe 目录的路径。
    set (OBOE_DIR ***PATH TO OBOE***)

    # 将Oboe库添加为项目中的子目录。
    # 指定 add_subdirectory ，告诉 CMake 去在此目录中查找 Oboe 的 CMake 文件以编译 Oboe 源文件。
    # ./oboe 这个是指定编译后的二进制文件的存储位置（一开始clone后是没有./oboe的）
    add_subdirectory (${OBOE_DIR} ./oboe)

    # 指定 Oboe 的 headers 文件路径。
    # This allows targets compiled with this CMake (application code)
    # 添加 Oboe headers 以顺利访问其 API。
    include_directories (${OBOE_DIR}/include)


在同一文件中找到 [`target_link_libraries`](https://cmake.org/cmake/help/latest/command/target_link_libraries.html) 命令段.
添加 `oboe` 到应用程序库所依赖的库列表中。例如：

    target_link_libraries(native-lib oboe)

这是一个完整的 `CMakeLists.txt` 文件例子:

    cmake_minimum_required(VERSION 3.4.1)

    # Build 我们自己的原生库
    add_library (native-lib SHARED native-lib.cpp )

    # Build Oboe 库（这个库 clone 到 cpp 目录下）
    set (OBOE_DIR ./oboe)
    add_subdirectory (${OBOE_DIR} ./oboe)

    # 使 Oboe 的公共头文件可用，方便我们在应用程序里调用
    include_directories (${OBOE_DIR}/include)

    # 指定我们的本机库所依赖的库, 也包括 Oboe（这里指定了3个，一个自己的库，一个log库，一个oboe库）
    target_link_libraries (native-lib log oboe)


现在，通过菜单 `Build->Refresh Linked C++ Projects` 命令，使 Android Studio 为 Oboe 库编制索引。

![20200221233904.png](https://raw.githubusercontent.com/picofpv/picMarkdown/master/picGo/20200221233904.png)

验证您的项目正确构建。 如果您有任何建筑问题，请 [在这里报告](issues/new).

# 2. 使用 Oboe
将 Oboe 添加到项目中后，即可开始使用 Oboe 的功能。在 Oboe 中，最简单，可能也是最常见的事情是创建音频流。

## 创建音频流
引入 Oboe 的头文件:

    #include <oboe/Oboe.h>
    
音频流通过使用 `AudioStreamBuilder` 构建器来创建. 具体就像这样:

    oboe::AudioStreamBuilder builder;

使用构建器的set方法设置音频流的一些属性 (您可以在  [完整指南](FullGuide.md) 中阅读有关这些属性的更多信息):

```c++
    builder.setDirection(oboe::Direction::Output);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(oboe::ChannelCount::Mono);
```

构建器的set方法返回指向构建器的指针。这样就可以轻松地将它们链接起来：

```c++
    oboe::AudioStreamBuilder builder;
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setCallback(myCallback)
        ->setFormat(oboe::AudioFormat::Float);
```

定义一个 `AudioStreamCallback` 类，在音频流饿了，需要新数据时提供数据。

```c++
    class MyCallback : public oboe::AudioStreamCallback {
    public:
        oboe::DataCallbackResult
        onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
            
            // 我们请求了 AudioFormat::Float 这里我们假设我们已经有数据了。
            // 对于生产代码，请始终检查数据格式
            // 音频流有且会强制把数据转换为适当的类型。
            auto *outputData = static_cast<float *>(audioData);
	    
            // 在这段范例里，我们的音频生成器生成白噪声，也就是生成一些以零为中心的随机数，振幅为0.2f，振幅太大会很吵
            const float amplitude = 0.2f;
            for (int i = 0; i < numFrames; ++i){
                outputData[i] = ((float)drand48() - 0.5f) * 2 * amplitude;
            }
	    
            return oboe::DataCallbackResult::Continue;
        }
    };
```

您可以在示例中找到有关如何使用数字合成和预录音频播放声音的示例。 [code samples](../samples).

在某个地方声明您的回调，使其在使用时不会被删除。

    MyCallback myCallback;

然后，将此 Callback 类提供给构建器：

    builder.setCallback(&myCallback);
    
声明一个 ManagedStream。确保在适当的范围内声明了它 (例如声明为管理类的成员). 避免将其声明为全局变量。
```c++
oboe::ManagedStream managedStream;
```
open 音频流：

    oboe::Result result = builder.openManagedStream(managedStream);

检查 result 以确保音频流已成功打开。 如果出错，result 会返回错误信息，Oboe有一种方便的方法可以将错误信息转换为可读的字符串，称为 `oboe::convertToText` ，如下：

    if (result != oboe::Result::OK) {
        LOGE("Failed to create stream. Error: %s", oboe::convertToText(result));
    }

请注意，此示例代码使用了 [logging macros from here](https://github.com/googlesamples/android-audio-high-performance/blob/master/debug-utils/logging_macros.h).

## 播放音频
检查创建的音频流的属性。如果未指定 channelCount, sampleRate, 或 format 那么你需要查询流以查看这些属性的具体内容。 The **format** 属性将决定 `audioData` 类型 in the `AudioStreamCallback::onAudioReady` callback. 如果您确实指定了这三个属性中的任何一个，那么您将得到您所要求的。

    oboe::AudioFormat format = stream->getFormat();
    LOGI("AudioStream format is %s", oboe::convertToText(format));

现在，通过 requestStart() 开始播放。

    managedStream->requestStart();

此时，您应该开始接收回调。

要停止接收回调，停止播放，需要通过 requestStop()
    
    managedStream->requestStop();

## 关闭 音频流
在不使用流时，请务必关闭流，以免占用其他应用程序可能要用的音频资源。使用 `SharingMode::Exclusive` 独占模式时尤其如此 ， 因为您可能会阻止其他应用获取低延迟的音频流。反之，如果你就是觉得自己最重要，就占着吧。

可以显式的去关闭流，用 close()：

    stream->close();

`close()` is a blocking call 这也将阻止音频流。

超出范围时，音频流也可以自动关闭：

	{
		ManagedStream mStream;
		AudioStreamBuilder().build(mStream);
		mStream->requestStart();
	} // 超出此范围的mStream已自动关闭
	
当应用不再播放音频时,最好让 `ManagedStream` 对象超出范围 (或被明确删除) .
对于仅在前台播放或录制音频的应用 , 通常在 [`Activity.onPause()`](https://developer.android.com/guide/components/activities/activity-lifecycle#onpause) 情况下停止播放。 

## 重新配置 音频流
为了更改音频流的配置， 只需再次调用 `openManagedStream()` （前面在打开音频流时用过）. 现有音频流会被关闭，销毁，并且新建一个音频流，并且
填充 `managedStream`.
```c++
// 在运行时使用一些其他属性修改构建器。
builder.setDeviceId(MY_DEVICE_ID);
// 使用一些其他配置重新打开音频流
// 旧的 ManagedStream 将自动关闭并删除
builder.openManagedStream(managedStream);
```
The `ManagedStream` takes care of its own closure and destruction. If used in an
automatic allocation context (such as a member of a class), the stream does not
need to be closed or deleted manually. Make sure that the object which is responsible for
the `ManagedStream` (its enclosing class) goes out of scope whenever the app is no longer
playing or recording audio, such as when `Activity.onPause()` is called.


## 简短的完整示例

下列类是 `ManagedStream` 的完整实现， 用于播放正弦波.
Creating the class (e.g. through the JNI bridge) 
创建并打开一个 Oboe Stream 以渲染音频，之后销毁，停止并关闭该Stream。

```c++
#include <oboe/Oboe.h>
#include <math.h>

class OboeSinePlayer: public oboe::AudioStreamCallback {
public:

    OboeSinePlayer() {
        oboe::AudioStreamBuilder builder;
        // 为了方便起见，构建器的设置方法可以链接在一起。
        builder.setSharingMode(oboe::SharingMode::Exclusive)
          ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
          ->setChannelCount(kChannelCount)
          ->setSampleRate(kSampleRate)
          ->setFormat(oboe::AudioFormat::Float)
          ->setCallback(this)
          ->openManagedStream(outStream);
        // 通常，在查询一些流信息以及用户的一些输入后启动流
        outStream->requestStart();
    }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) override {
        float *floatData = (float *) audioData;
        for (int i = 0; i < numFrames; ++i) {
            float sampleValue = kAmplitude * sinf(mPhase);
            for (int j = 0; j < kChannelCount; j++) {
                floatData[i * kChannelCount + j] = sampleValue;
            }
            mPhase += mPhaseIncrement;
            if (mPhase >= kTwoPi) mPhase -= kTwoPi;
        }
        return oboe::DataCallbackResult::Continue;
    }

private:
    oboe::ManagedStream outStream;
    // 音频流参数
    static int constexpr kChannelCount = 2;
    static int constexpr kSampleRate = 48000;
    // 正弦波参数，这些可以是实例变量，以便在运行时进行修改
    static float constexpr kAmplitude = 0.5f;
    static float constexpr kFrequency = 440;
    static float constexpr kPI = M_PI;
    static float constexpr kTwoPi = kPI * 2;
    static double constexpr mPhaseIncrement = kFrequency * kTwoPi / (double) kSampleRate;
    // Keeps track of where the wave is
    float mPhase = 0.0;
};
```
请注意，为简化起见，此实现在运行时计算正弦值，而不是预先计算它们。
此外，最佳实践方式是写一个单独的回调类，而不是把管理流和回调放在一起在同一类中定义。
此类也会在构建时自动启动流。
而通常，是在启动流之前查询流以获取信息（例如，突发大小），并在用户输入时启动流。
有关如何使用 `ManagedStream` 的更多示例，请看 [samples](https://github.com/google/oboe/tree/master/samples) 目录.

## 获得最佳延迟

Oboe库的目标之一是在最广泛的硬件配置上提供低延迟的音频流。
使用 AAudio 打开流时，除非应用手动请求特定速率，否则将自动选择最佳速率。 framePerBurst 也由 AAudio 自行决定.

但是 OpenSL ES 无法自动确定这些值。 因此，应用程序应该使用 Java 查询它们，然后将它们通过 JNI 传递给 Oboe。这种方式将用于较旧设备上的 OpenSL ES 流。
这是一个代码示例，显示了如何设置这些默认值。

**MainActivity.java**
```java
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
        AudioManager myAudioMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        String sampleRateStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
	    int defaultSampleRate = Integer.parseInt(sampleRateStr);
	    String framesPerBurstStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
	    int defaultFramesPerBurst = Integer.parseInt(framesPerBurstStr);

	    native_setDefaultStreamValues(defaultSampleRate, defaultFramesPerBurst);
	}
```

**jni-bridge.cpp**

```c++
	JNIEXPORT void JNICALL
	Java_com_google_sample_oboe_hellooboe_MainActivity_native_1setDefaultStreamValues(JNIEnv *env,
	                                                                                  jclass type,
	                                                                                  jint sampleRate,
	                                                                                  jint framesPerBurst) {
	    oboe::DefaultStreamValues::SampleRate = (int32_t) sampleRate;
	    oboe::DefaultStreamValues::FramesPerBurst = (int32_t) framesPerBurst;
	}
```

请注意，来自 Java 的值适用于内置音频设备。而外围设备（例如蓝牙）可能需要更大容量的 framesPerBurst.

# 更多信息
- [Code 范例](https://github.com/google/oboe/tree/master/samples)
- [Full guide to Oboe](FullGuide.md)
