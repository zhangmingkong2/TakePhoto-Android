package com.marlinl.code.takephoto.controller;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.marlinl.code.takephoto.R;
import com.marlinl.code.takephoto.util.HttpRequest;
import com.marlinl.code.takephoto.view.AutoFitTextureView;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
    //后台服务线程
    private HandlerThread backgroundThread;
    //后台服务handler
    private Handler backgroundHandler;
    //保存的图片
    private ImageReader imageReader;
    //当前状态
    private int state = STATE_PREVIEW;
    //摄像头锁
    private Semaphore cameraOpenCloseLock = new Semaphore(1);

    private CaptureRequest previewRequest;

    private CaptureRequest.Builder previewRequestBuilder;


    private File file;





    /**
     * 唤醒摄像头
     */
    private final CameraDevice.StateCallback cameraStateCallback
            = new CameraDevice.StateCallback(){
        @Override
        public void onOpened(CameraDevice cd) {
            cameraOpenCloseLock.release();
            cameraDevice = cd;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            cameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }
    };

    /**
     * 开启摄像头
     */
    private  void createCameraPreviewSession () {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);
            previewRequestBuilder
                    = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback(){
                        @Override
                        public void onConfigured(CameraCaptureSession ccs) {
                            if (null == cameraDevice){
                                return;
                            }
                            //开启摄像头会话
                            cameraCaptureSession =ccs;
                            try{
                                //自动对焦
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                //连续播出
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                previewRequest = previewRequestBuilder.build();
                                cameraCaptureSession.setRepeatingRequest(previewRequest,
                                        captureCallback,backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            showToast("Fail");
                        }
                    },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback captureCallback
            = new CameraCaptureSession.CaptureCallback() {
        private void process (CaptureResult result) {
            switch (state) {
                case STATE_PREVIEW:{
                    break;
                }
                case STATE_WAITING_LOCK:{
                    int afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED){
                            captureStillPicture();
                        } else {
                            runPrecaptureSequece();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        state = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (null == aeState || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        state = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }

            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            process(result);
        }

        //TODO
    };

    private void runPrecaptureSequece() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            state = STATE_WAITING_PRECAPTURE;
            cameraCaptureSession.capture(previewRequestBuilder.build(),
                    captureCallback,backgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void captureStillPicture () {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == cameraDevice) {
                return;
            }
            final CaptureRequest.Builder captureBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));
            CameraCaptureSession.CaptureCallback captureCallback1
                    = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    showToast("Saved"+file);
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            state = STATE_PREVIEW;
            cameraCaptureSession.setRepeatingRequest(previewRequest,
                    captureCallback,backgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Handler messageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Activity activity = getActivity();
            if (activity != null) {
                Toast.makeText(activity,(String)msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void showToast(String text) {
        Message message = Message.obtain();
        message.obj = text;
        messageHandler.sendMessage(message);
    }

    /**
     * 回调对象
     */
    private final  ImageReader.OnImageAvailableListener onImageAvailableListener
            = new ImageReader.OnImageAvailableListener(){
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            backgroundHandler.post(new ImageSaver(imageReader.acquireLatestImage(),file));
        }
    };

    /**
     * 设置摄像头输出的
     * @param width
     * @param height
     */
    private void setUpCameraOutputs(int width, int height){
        Activity activity = getActivity();
        CameraManager cameraManager
                = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics
                        = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                        == CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size largest = Collections.max(Arrays.asList(streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizeByArea());
                imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, 2);
                imageReader.setOnImageAvailableListener(
                        onImageAvailableListener,backgroundHandler
                );
                previewSize = chooseOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class),
                        width, height, largest);
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(
                            previewSize.getWidth(), previewSize.getHeight()
                    );
                } else {
                    textureView.setAspectRatio(
                            previewSize.getHeight(), previewSize.getWidth()
                    );
                }
                cameraID = cameraId;
                Log.e(TAG, cameraId);

            }

        }  catch (CameraAccessException e) {
            e.printStackTrace();
        }catch (NullPointerException e) {
            new ErrorDialog().show(getFragmentManager(), "dialog");
        }
    }

    /**
     * 旋转设置
     * @param viewWidth
     * @param viewHeight
     */
    private void configTransform (int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == textureView || null == previewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0,0,viewWidth,viewHeight);
        RectF bufferRect = new RectF(0,0,previewSize.getHeight(),previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        textureView.setTransform(matrix);
    }



    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio){
        List<Size> li = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices){
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height){
                li.add(option);
            }
        }
        if (li.size() > 0){
            return Collections.min(li, new CompareSizeByArea());
        } else {
            return  choices[0];
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private  void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size size, Size size2) {
            return Long.signum((long) size.getWidth() * size.getHeight() -
                    (long) size2.getWidth() * size2.getHeight());
        }
    }

    /**
     * 开启摄像头
     * @param width
     * @param height
     */
    private  void openCamera(int width, int height){
        setUpCameraOutputs(width, height);
        configTransform(width, height);
        Activity activity = getActivity();
        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)){
                throw new RuntimeException("time out waiting to lock camera opening");
            }
            cameraManager.openCamera(cameraID, cameraStateCallback, backgroundHandler );
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * 关闭摄像头
     */
    private  void closeCamera(){
        try {
            cameraOpenCloseLock.acquire();
            if (null != cameraCaptureSession) {
                cameraCaptureSession.close();
                cameraCaptureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    private void takePhoto () {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            state = STATE_WAITING_LOCK;
            cameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(),
                    captureCallback,backgroundHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * 初始化控制器
     * @return
     */
    public static DisplayController newInstance(){
        DisplayController displayController = new DisplayController();
        displayController.setRetainInstance(true);
        return displayController;
    }

    /**
     * 加载界面
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return  inflater.inflate(R.layout.fragment_camera, container, false);
    }

    /**
     * 界面加载完成
     * @param view
     * @param savedInstanceState
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        /**
         * 拍照
         */
        view.findViewById(R.id.action).setOnClickListener(this);
        textureView = (AutoFitTextureView)view.findViewById(R.id.cameraDisplay);
    }

    /**
     * 加载完活动
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String filename = System.currentTimeMillis()+".jpg";
        file = new File(getActivity().getExternalFilesDir(null),filename);
    }

    /**
     * 恢复&&开启
     */
    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(),textureView.getHeight());
        }else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    /**
     * 暂停
     */
    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.action : {
                break;
            }
        }
    }

    /**
     * 调用surface 开启摄像头
     */
    private final TextureView.SurfaceTextureListener surfaceTextureListener
            = new TextureView.SurfaceTextureListener(){
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
            openCamera(i,i2);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
            configTransform(i,i2);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    public static class ErrorDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return super.onCreateDialog(savedInstanceState);
        }
    }
    private static  class ImageSaver implements Runnable {

        private final Image image;

        private final File file;

        public ImageSaver(Image image,File file){
            this.image = image;
            this.file =file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream outputStream = null;
            try {
                outputStream =new FileOutputStream(file);
                outputStream.write(bytes);
                HttpRequest.doPost("http://127.0.0.1/TestServices/upload",file);
            }catch (Exception e) {
                e.printStackTrace();
            } finally {
                image.close();
                if (null != outputStream) {
                    try {
                        outputStream.close();
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
