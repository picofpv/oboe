package com.google.sample.oboe.hellooboe;

import android.os.Bundle;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MotionEventCompat;

public class anotherAct extends AppCompatActivity {

    private static final String TAG = anotherAct.class.getName();

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case (MotionEvent.ACTION_DOWN):
                PlaybackEngine.setToneOn(true);
                break;
            case (MotionEvent.ACTION_UP):
                PlaybackEngine.setToneOn(false);
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        PlaybackEngine.setAudioApi(2);  // "Unspecified", "OpenSL ES", "AAudio"
        PlaybackEngine.setAudioDeviceId(2);
        PlaybackEngine.setChannelCount(0);
        PlaybackEngine.setBufferSizeInBursts(1);

    }

    @Override
    protected void onResume() {
        super.onResume();
        PlaybackEngine.create(this);    // 按照默认值设置并创建音频引擎
    }

    @Override
    protected void onPause() {
        PlaybackEngine.delete();
        super.onPause();
    }
}
