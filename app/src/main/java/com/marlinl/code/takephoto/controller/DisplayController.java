package com.marlinl.code.takephoto.controller;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.marlinl.code.takephoto.view.AutoFitTextureView;

import java.io.File;

/**
 * Created by MarlinL on 2015/6/8.
 */
public class DisplayController extends Fragment implements View.OnClickListener {


    //转屏幕预览 和JPEG的朝向一直
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    private static  final String TAG = "Camera2";

    private static final int STATE_PREVIEW = 0;

    private static final int STATE_WAITING_LOCK = 1;

    private static final int STATE_WAITING_PRECAPTURE = 2;

    private static  final int STATE_WAITING_NON_PRECAPTURE = 3;

    private  static final int STATE_PICTURE_TAKEN = 4;
    //相机当前的id
    private String cameraID;
    //界面
    private AutoFitTextureView textureView;
    //相机session
    private CameraCaptureSession cameraCaptureSession;
    //相机设备
    private CameraDevice cameraDevice;
    //状态
    private Size previewSize;
    //显示的builder
    private CaptureRequest.Builder previewBuilder;
    //拍照的builder
    private  CaptureRequest.Builder captureBuilder;
    //建立handler
    private HandlerThread backgroundThread;
    //自己的handler
    private Handler backgroundHandler;
    //保存的图片
    private ImageReader imageReader;
    //当前状态
    private int state = STATE_PREVIEW;

    private File file;


    private final TextureView.SurfaceTextureListener surfaceTextureListener
            = new TextureView.SurfaceTextureListener(){
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };


    /**
     * 切换摄像头
     */
    private final CameraDevice.StateCallback cameraStateCallback
            = new CameraDevice.StateCallback(){
        @Override
        public void onOpened(CameraDevice cameraDevice) {

        }

        @Override
        public void onClosed(CameraDevice camera) {
            super.onClosed(camera);
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {

        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {

        }
    };

    /**
     * 回调对象 保存图片
     */
    private final  ImageReader.OnImageAvailableListener onImageAvailableListener
            = new ImageReader.OnImageAvailableListener(){
        @Override
        public void onImageAvailable(ImageReader imageReader) {

        }
    };

    private void setUpCameraOutputs(int width, int height){
        Activity activity = getActivity();

    }

    private  void openCamera(int width, int height){

    }

    @Override
    public void onClick(View view) {

    }
}
