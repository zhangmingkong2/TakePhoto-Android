package com.marlinl.code.takephoto;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;

import com.marlinl.code.takephoto.controller.DisplayController;

/**
 * Created by MarlinL on 2015/6/7.
 */
public class CameraMain extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);
        if (null == savedInstanceState){
            getFragmentManager().beginTransaction()
                    .replace(R.id.container,new DisplayController())
                    .commit();
        }
    }
}
