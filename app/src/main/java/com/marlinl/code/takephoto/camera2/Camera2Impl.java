package com.marlinl.code.takephoto.camera2;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.util.Size;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by MarlinL on 2015/6/8.
 */
public class Camera2Impl {

    private CameraManager cameraManager;

    private ImageReader imageReader;



    public Camera2Impl(Activity activity) {
        try {
            cameraManager
                    = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            for (String cameraID : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics
                        = cameraManager.getCameraCharacteristics(cameraID);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                        == CameraCharacteristics.LENS_FACING_FRONT);{
                    continue;
                }
                StreamConfigurationMap map = null;
                map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizeByArea());
                imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, 2);
                imageReader.setOnImageAvailableListener(
                    
                );
                return;

            }

        }catch (Exception e){

        }
    }


    @Override
    public void run() {
        try {
            cameraManager
                    = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            for (String cameraID : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics
                        = cameraManager.getCameraCharacteristics(cameraID);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                        == CameraCharacteristics.LENS_FACING_FRONT);{
                    continue;
                }
                StreamConfigurationMap map = null;
                map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizeByArea());
            }

        }catch (Exception e){

        }
    }

    static class CompareSizeByArea implements Comparator<Size>{
        @Override
        public int compare(Size size, Size size2) {
            return Long.signum((long) size.getWidth() * size.getHeight() -
                    (long) size2.getWidth() * size2.getHeight());
        }
    }

    private static class ImageSaver implements Runnable {

        private final Image image;

        private final File file;

        public ImageSaver(Image image, File file){
            this.image = image;
            this.file = file;
        }

        @Override
        public void run() {

        }
    }
}
