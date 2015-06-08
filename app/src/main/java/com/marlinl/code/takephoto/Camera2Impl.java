package com.marlinl.code.takephoto;

import android.app.Fragment;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
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
 * Created by MarlinL on 2015/6/7.
 */
public class Camera2Impl extends Fragment implements View.OnClickListener {

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

    private String cameraID;

    private AutoFitTextureView textureView;

    private CameraCaptureSession cameraCaptureSession;

    private  CameraDevice cameraDevice;

    private Size previewSize;

    private HandlerThread backgroundThread;

    private Handler backgroundHandler;

    private ImageReader imageReader;

    private File file;

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

    private final  ImageReader.OnImageAvailableListener onImageAvailableListener
            = new ImageReader.OnImageAvailableListener(){
        @Override
        public void onImageAvailable(ImageReader imageReader) {

        }
    };

    @Override
    public void onClick(View view) {

    }
}
