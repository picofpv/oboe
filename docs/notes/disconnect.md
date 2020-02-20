[Oboe Docs Home](README.md)

# 技术说明: 断开音频流

当 Oboe 使用 **OpenSL ES**, 并且耳机已插入或拔出, 则OpenSL ES将自动在设备之间切换。
这很方便，但是会引起问题，因为新设备可能具有不同的突发大小和不同的延迟。

当 Oboe 使用 **AAudio**, 且耳机已插入或拔出， 则该流不再可用，并变为“断开连接”。
然后，将通过以下两种方式之一通知该应用。

1) 如果应用程序使用回调，则将调用AudioStreamCallback对象。
它将启动一个线程，该线程将调用onErrorBeforeClose（）。
随后 它停止并关闭流。
之后 将调用onErrorAfterClose（）。
应用程序可以选择使用onErrorAfterClose（）方法重新打开流。
这是一连串组合操作，你可以在这个流程里处理可能出现的问题。

2) 如果应用程序正在使用read（）/write（）调用，则在断开连接时它们将返回错误。
该应用程序会 stop() 和 close() 音频流。
然后，应用程序可以选择重新打开流。

## 不能正确断开连接的解决方法

在某些版本的Android上，断开连接消息无法到达AAudio，并且该应用程序无法
知道设备已更改。在[OboeTester](https://github.com/google/oboe/tree/master/apps/OboeTester/docs) 的“测试断开连接”选项中
有一段示例代码，你可以拿来主义，这段代码可以用来诊断这个问题。

作为一种解决方法，您可以侦听Java Intent.HEADSET_PLUG，当插入、拔下耳机时会触发该事件。

    // 插入或拔出耳机时，接收广播的Intent。
    public class PluginBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 如果未断开连接，请关闭流。
        }
    }
    
    private BroadcastReceiver mPluginReceiver = new PluginBroadcastReceiver();
    
您可以在应用 Resume 时注册Intent，而在其 Pause 时注销。
    
    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        this.registerReceiver(mPluginReceiver, filter);
    }

    @Override
    public void onPause() {
        this.unregisterReceiver(mPluginReceiver);
        super.onPause();
    }
