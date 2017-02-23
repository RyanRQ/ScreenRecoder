package com.ryan.screenrecoder;

import android.content.Intent;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private MediaProjectionManager mediaProjectionManager;
    private Button button_start;
    private Button button_end;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button_start = ((Button) findViewById(R.id.button_start));
        button_end = ((Button) findViewById(R.id.button_end));
        button_start.setOnClickListener(this);
        button_end.setOnClickListener(this);
        mediaProjectionManager= (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_end:
                break;
            case R.id.button_start:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MediaProjection mediaProjection=mediaProjectionManager.getMediaProjection(resultCode,data);

    }
}
