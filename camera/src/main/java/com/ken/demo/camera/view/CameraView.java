package com.ken.demo.camera.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.audiofx.EnvironmentalReverb;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.os.EnvironmentCompat;

import com.ken.demo.camera.util.FileUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CameraView extends TextureView {

    private static final Size PREVIEW_SIZE = new Size(648, 480);
    private static final String TAG = "CameraView";
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private File saveDir;

    private Activity activity;
    private CameraManager cameraManager = null;

    private HandlerThread cameraThread = null;
    private Handler cameraHandler = null;

    private CameraDevice mCameraDevice = null;
    private CameraCaptureSession previewSession = null;
    private CaptureRequest.Builder previewBuilder = null;

    private String frontCameraId;
    private String backCameraId;
    private String cameraId;


    private Map<Long, File> imgMap = new HashMap<>();
    private Map<Long, String> frameMap = new HashMap<>();

    private ImageReader imageReader = null;
    private long count = 0;
    private ImageReader.OnImageAvailableListener onImageAvailableListener = reader -> {
        Image image = reader.acquireNextImage();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        image.close();
        File file = FileUtil.writeFile(data, saveDir, count);
        Log.d(TAG, "image reader :" + count);
        imgMap.put(count, file);
        count++;
    };

    private Handler mainHandler = new Handler();

    protected TextureView.SurfaceTextureListener listener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            setAspectRatio(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // TODO: 2019/9/22 发送数据
        }
    };

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            createPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            super.onClosed(camera);
            mCameraDevice = null;
        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            Log.d(TAG, "onCaptureStarted: " + frameNumber);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Log.d(TAG, "onCaptureCompleted: " + result.getFrameNumber());

            long frame = result.getFrameNumber();
//            if (imgMap.containsKey(result.getFrameNumber())) {
//                imgMap.remove(result.getFrameNumber());
//            }
            frameMap.put(frame, "frame");
            mainHandler.postDelayed(() -> {
                if (imgMap.containsKey(frame)) {
                    Log.d(TAG, "run: delete" + frame);
                    File file = imgMap.get(frame);
                    file.renameTo(new File(file.getParentFile(), file.getName() + "---frame" + frame));
                    imgMap.remove(frame);
                    frameMap.remove(frame);
                }
            }, 100);
        }
    };


    public CameraView(Context context) {
        super(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    public void onResume(Activity activity) {
        this.activity = activity;
        if (this.isAvailable()) {
            openCamera();
        } else {
            this.setSurfaceTextureListener(listener);
        }
    }

    public void onPause() {
        closeCamera();
        Log.d(TAG, "onPause: img size=================================================" + imgMap.size());
        Log.d(TAG, "onPause: frame size=================================================" + frameMap.size());
        for (Long number : imgMap.keySet()) {
            if (frameMap.containsKey(number)) {
                Log.d(TAG, "run: delete" + number);
                File file = imgMap.get(number);
                file.renameTo(new File(file.getParentFile(), file.getName() + "---frame" + number + ".yuv"));
                frameMap.remove(number);
            }
        }
        Log.d(TAG, "onPause: img size=================================================" + imgMap.size());
        Log.d(TAG, "onPause: frame size=================================================" + frameMap.size());
    }

    public void onStart() {
        cameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        startCameraThread();
        File root = new File(Environment.getExternalStorageDirectory(), "CameraDemo");
        saveDir = new File(root, getFileDir());
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
    }

    private String getFileDir() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
        return sdf.format(new Date());
    }

    public void onDestroy() {
        stopCameraThread();
    }

    private void startCameraThread() {
        cameraThread = new HandlerThread("Camera Thread");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
    }

    private void stopCameraThread() {
        if (cameraHandler != null) {
            cameraHandler.removeCallbacksAndMessages(null);
        }
        if (cameraThread != null) {
            cameraThread.quitSafely();
            try {
                cameraThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cameraHandler = null;
            cameraThread = null;
        }
    }

    private void setupCamera() {
        try {
            String[] idList = cameraManager.getCameraIdList();
            for (String id : idList) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null) {
                    if (facing.intValue() == CameraCharacteristics.LENS_FACING_BACK) {
                        backCameraId = id;
                    } else if (facing.intValue() == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                        frontCameraId = id;
                    }
                }
            }
            cameraId = frontCameraId;
            if (TextUtils.isEmpty(cameraId)) {
                cameraId = backCameraId;
            }
            if (TextUtils.isEmpty(cameraId)) {
                mainHandler.post(() -> {
                    Toast.makeText(getContext(), "can't find camera", Toast.LENGTH_SHORT).show();
                });
                activity.finish();
            }
            imageReader = ImageReader.newInstance(PREVIEW_SIZE.getWidth(), PREVIEW_SIZE.getHeight(), ImageFormat.YUV_420_888, 3);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, cameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        setupCamera();
        try {
            cameraManager.openCamera(cameraId, stateCallback, cameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createPreview() {
        if (mCameraDevice != null) {
            try {
                previewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                SurfaceTexture surfaceTexture = this.getSurfaceTexture();
                surfaceTexture.setDefaultBufferSize(PREVIEW_SIZE.getWidth(), PREVIEW_SIZE.getHeight());
                Surface surface = new Surface(surfaceTexture);

                previewBuilder.addTarget(surface);
                previewBuilder.addTarget(imageReader.getSurface());

                mCameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                        new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                previewSession = session;
                                if (previewSession != null) {
                                    try {
                                        previewSession.setRepeatingRequest(previewBuilder.build(), captureCallback, cameraHandler);
                                    } catch (CameraAccessException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                mainHandler.post(() -> {
                                    Toast.makeText(getContext(), "create session failed", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }, cameraHandler);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeCamera() {
        if (previewSession != null) {
            previewSession.close();
            previewSession = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }


}

