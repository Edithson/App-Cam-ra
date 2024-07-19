package com.edithson.appcamera1;

//////////////////////////////

import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.icu.text.SimpleDateFormat;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import android.graphics.ImageFormat;
import android.graphics.drawable.Drawable;

import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    //variables techniques
    private CameraManager cameraManager;

    private boolean isCameraFront = false;
    private String cameraId;
    private CameraManager manager;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final int REQUEST_CODE_WRITE_SETTINGS = 123;
    private int originalBrightness;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension = new Size(1080, 1440); // Default 3:4

    private Button flashButton, switwhCmrBtn;
    public enum FlashMode {
        OFF,
        ON,
        AUTO
    }
    private FlashMode flashMode = FlashMode.OFF;

    //variables de vue

    private static final String TAG = "MainActivity";
    private TextView timerText;

    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private float zoomLevel = 1f;
    private float maximumZoomLevel;
    private ScaleGestureDetector scaleGestureDetector;

    //########################
    //boutons d'action
    ImageView start_recorder, end_recorder, image_capture, switchCameraButton;

    //les variables d'option
    //menu option
    LinearLayout menu_option;
    ImageView flash;
    ImageView minuteur;
    ImageView filtre;
    ImageView taile;
    ImageView option;

    //menu flash
    LinearLayout menu_flash;
    ImageView flash_off;
    ImageView flash_auto;
    ImageView flash_on;

    //menu minuteur
    LinearLayout menu_minuteur;
    ImageView minuteur_0;
    ImageView minuteur_1;
    ImageView minuteur_2;
    ImageView minuteur_3;

    //menu taile
    LinearLayout menu_taile;
    ImageView taile_1;
    ImageView taile_2;
    ImageView taile_3;
    ImageView taile_4;

    //autres
    ImageView rotation;
    //fin de variables d'option

    //Les variables de captures
    private String mode = "photo";
    private int mode_flash = 2; //0=off, 1=on, 2=auto
    private int mode_taille = 1; //1=3:4, 2=1:1, 3=9:16, 4=9:22
    private int mode_min = 0; //0=0s, 2=2s, 5=5s, 10=10s
    private HorizontalScrollView horizontalScrollView;
    private LinearLayout linearLayout;
    private Button btn_photo;
    private Button btn_video;
    private Button btn_slowMotion;
    private Button btn_portrait;
    private ArrayList<Button> tab_btn = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ajuster la luminosité de la fenêtre pour cette activité
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = 1.0f; // 1.0f pour la luminosité maximale
        getWindow().setAttributes(layoutParams);

        // Empêcher l'écran de s'éteindre
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initialisation des vues
        textureView = findViewById(R.id.texture);

        horizontalScrollView = findViewById(R.id.horizontal_scroll_view);
        textureView.setSurfaceTextureListener(textureListener);

        ImageView brightImageView = findViewById(R.id.filterView);

        // Créez une ColorMatrix pour augmenter la luminosité
        ColorMatrix brightnessMatrix = new ColorMatrix(new float[] {
                1, 0, 0, 0, 50,
                0, 1, 0, 0, 50,
                0, 0, 1, 0, 50,
                0, 0, 0, 1, 0
        });

        // Créez une ColorMatrix pour augmenter le contraste
        ColorMatrix contrastMatrix = new ColorMatrix(new float[] {
                1.5f, 0, 0, 0, -0.5f * 255,
                0, 1.5f, 0, 0, -0.5f * 255,
                0, 0, 1.5f, 0, -0.5f * 255,
                0, 0, 0, 1, 0
        });

        // Combinaison des deux matrices
        brightnessMatrix.postConcat(contrastMatrix);

        // Appliquez le filtre de couleur
        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(brightnessMatrix);
        brightImageView.setColorFilter(colorFilter);

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0]; // Assurez-vous que vous obtenez l'ID de la caméra correcte
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_CAMERA_PERMISSION);
        }

        startCamera();

        timerText = findViewById(R.id.timer_text);

        //bouton d'action
        start_recorder = findViewById(R.id.start_recorder);
        end_recorder = findViewById(R.id.end_recorder);
        image_capture = findViewById(R.id.capture_img);

        //boutons de captures
        linearLayout = findViewById(R.id.linear_layout);
        btn_photo = findViewById(R.id.button3);
        btn_portrait = findViewById(R.id.button2);
        btn_video = findViewById(R.id.button4);
        btn_slowMotion = findViewById(R.id.button1);

        //bouton d'option flash
        flash = findViewById(R.id.flash);
        menu_flash = findViewById(R.id.menu_flash);
        flash_on = findViewById(R.id.menu_flash_on);
        flash_off = findViewById(R.id.menu_flash_off);
        flash_auto = findViewById(R.id.menu_flash_auto);
        //bouton d'option minuteur
        minuteur = findViewById(R.id.minuteur);
        menu_minuteur = findViewById(R.id.menu_minuteur);
        minuteur_0 = findViewById(R.id.menu_minuteur_off);
        minuteur_1 = findViewById(R.id.menu_minuteur_1);
        minuteur_2 = findViewById(R.id.menu_minuteur_2);
        minuteur_3 = findViewById(R.id.menu_minuteur_3);
        //bouton d'option taile
        taile = findViewById(R.id.taile);
        menu_taile = findViewById(R.id.menu_taile);
        taile_1 = findViewById(R.id.menu_taile_1);
        taile_2 = findViewById(R.id.menu_taile_2);
        taile_3 = findViewById(R.id.menu_taile_3);
        taile_4 = findViewById(R.id.menu_taile_4);

        //autre
        option = findViewById(R.id.setting);
        filtre = findViewById(R.id.filtre);
        rotation = findViewById(R.id.turn_cmr);
        menu_option = findViewById(R.id.menu_options);

        //gestion du menu
        flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideMenu(menu_flash);
            }
        });
        flash_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideMenu(menu_flash);
                switchIcon(flash_off, flash);
                mode_flash = 0;
                flashMode = FlashMode.OFF;
            }
        });
        flash_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideMenu(menu_flash);
                switchIcon(flash_on, flash);
                mode_flash = 1;
                flashMode = FlashMode.ON;
            }
        });
        flash_auto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideMenu(menu_flash);
                switchIcon(flash_auto, flash);
                mode_flash = 2;
                flashMode = FlashMode.AUTO;
            }
        });
        minuteur.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideMenu(menu_minuteur);
            }
        });
        minuteur_0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideMenu(menu_minuteur);
                switchIcon(minuteur_0, minuteur);
                mode_min = 0;
            }
        });
        minuteur_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideMenu(menu_minuteur);
                switchIcon(minuteur_1, minuteur);
                mode_min = 2;
            }
        });
        minuteur_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideMenu(menu_minuteur);
                switchIcon(minuteur_2, minuteur);
                mode_min = 5;
            }
        });
        minuteur_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideMenu(menu_minuteur);
                switchIcon(minuteur_3, minuteur);
                mode_min = 10;
            }
        });
        taile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideMenu(menu_taile);
            }
        });
        taile_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideMenu(menu_taile);
                switchIcon(taile_1, taile);
                setPreviewSize(3, 4);
                mode_taille = 1;
            }
        });
        taile_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideMenu(menu_taile);
                switchIcon(taile_2, taile);
                setPreviewSize(1, 1);
                mode_taille = 2;
            }
        });
        taile_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideMenu(menu_taile);
                switchIcon(taile_3, taile);
                setPreviewSize(9, 16);
                mode_taille = 3;
            }
        });
        taile_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHideMenu(menu_taile);
                switchIcon(taile_4, taile);
                setPreviewSize(9, 22);
                mode_taille = 4;
            }
        });
        //fin de gestion du menu

        // Ajout des boutons à la liste
        tab_btn.add(btn_slowMotion);
        tab_btn.add(btn_portrait);
        tab_btn.add(btn_photo);
        tab_btn.add(btn_video);

        //sélecteur par défaut
        btnCenter(btn_photo);

        // Ajout des listeners de clic pour chaque bouton
        btn_photo.setOnClickListener(v -> {
            mode = "photo";
            btnCenter(btn_photo);
        });

        btn_portrait.setOnClickListener(v -> {
            mode = "portrait";
            btnCenter(btn_portrait);
        });

        btn_video.setOnClickListener(v -> {
            mode = "video";
            btnCenter(btn_video);
        });

        btn_slowMotion.setOnClickListener(v -> {
            mode = "slow_motion";
            btnCenter(btn_slowMotion);
        });

        //Appel des méthodes d'enrégistrement
        start_recorder.setOnClickListener(v -> {
            //methode de debut d'enrégistrement vidéo
            start_recorder.setVisibility(View.GONE);
            end_recorder.setVisibility(View.VISIBLE);
        });
        end_recorder.setOnClickListener(v -> {
            //methode de fin d'enrégistrement vidéo
            start_recorder.setVisibility(View.VISIBLE);
            end_recorder.setVisibility(View.GONE);
        });
        image_capture.setOnClickListener(v -> {
            //methode de prise de photo
            if (mode_min == 0){
                takePicture();
            }else {
                timerText.setVisibility(View.VISIBLE);

                new CountDownTimer(mode_min * 1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        int secondsRemaining = (int) (millisUntilFinished / 1000);
                        timerText.setText(String.valueOf(secondsRemaining+1));
                    }

                    @Override
                    public void onFinish() {
                        timerText.setVisibility(View.GONE);
                        takePicture();
                    }
                }.start();
            }

        });

        //changement de caméra
        switchCameraButton = findViewById(R.id.turn_cmr);
        assert switchCameraButton != null;
        switchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });
    }

    private void switchCamera() {
        isCameraFront = !isCameraFront;
        closeCamera();
        openCamera();
    }

    private void centerViewInHorizontalScrollView(Button button) {
        int scrollViewWidth = horizontalScrollView.getWidth();
        int buttonWidth = button.getWidth();
        int buttonLeft = button.getLeft();

        // Reset the background color of all buttons
        for (Button btn : tab_btn) {
            btn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.black));
        }

        // Set the background color of the selected button
        button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.ColoerBtn));

        // Calculate the scroll position to center the button
        int scrollToX = buttonLeft - (scrollViewWidth / 2) + (buttonWidth / 2);
        horizontalScrollView.smoothScrollTo(scrollToX, 0);
    }

    //centrage de l'option de capture sélectionné
    private void btnCenter(Button button) {
        // Use post to ensure that the view is fully rendered before scrolling
        horizontalScrollView.post(() -> centerViewInHorizontalScrollView(button));
        btnCapture();
    }

    //méthode pour adapter le bouton de capture
    protected void btnCapture() {
        if (mode.equals("photo") || mode.equals("portrait")) {
            image_capture.setVisibility(View.VISIBLE);
            start_recorder.setVisibility(View.GONE);
            end_recorder.setVisibility(View.GONE);
        } else {
            image_capture.setVisibility(View.GONE);
            start_recorder.setVisibility(View.VISIBLE);
            end_recorder.setVisibility(View.GONE);
        }
    }

    //gestion du menue
    public void showHideMenu(LinearLayout ll) {
        if (ll.getVisibility() == View.VISIBLE) {
            ll.setVisibility(View.GONE);
            menu_option.setVisibility(View.VISIBLE);
        } else {
            ll.setVisibility(View.VISIBLE);
            menu_option.setVisibility(View.GONE);
        }
    }

    //chagement d'icone
    public void switchIcon(ImageView img1, ImageView img2) {
        //img1 est l'ancienne icone et img2 la nouvelle

        // Récupérer le Drawable de img1
        Drawable drawable = img1.getDrawable();

        // Définir le Drawable de img1 comme source de img2
        img2.setImageDrawable(drawable);
    }


    //Methode technique

    //gestion des vues
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    protected void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[isCameraFront ? 1 : 0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;

            maximumZoomLevel = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            imageDimension = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(SurfaceTexture.class)[0];
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void startPreview() {
        if (null == cameraDevice || !textureView.isAvailable() || null == imageDimension) {
            return;
        }
        SurfaceTexture texture = textureView.getSurfaceTexture();
        if (null == texture) {
            return;
        }
        texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
        Surface surface = new Surface(texture);
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == cameraDevice) {
                        return;
                    }
                    cameraCaptureSessions = cameraCaptureSession;
                    try {
                        updatePreview();
                    }catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() throws CameraAccessException {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 4); // Augmentez cette valeur pour plus de luminosité
        captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 1600); // Augmentez cette valeur pour plus de luminosité
        captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);

        // Mise au point automatique continue
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

// Stabilisation d'image
        captureRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
        captureRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);

// Vitesse d'obturation
        captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 1000000000L / 1000);

// Balance des blancs manuelle pour la lumière du jour
        captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT);

        // Activer le mode HDR
        captureRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_HDR);

        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] jpegSizes = null;

        if (map != null) {
            jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
        }

// Choisissez la plus grande taille disponible
        Size largest = Collections.max(Arrays.asList(jpegSizes), new CompareSizesByArea());
        captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
        captureRequestBuilder.set(CaptureRequest.JPEG_THUMBNAIL_SIZE, largest);

        captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);

        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, -2); // Ajustez cette valeur en fonction de l'éclairage

        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void takePicture() {
        if (cameraDevice == null) {
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

            switch (flashMode) {
                case ON:
                    captureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
                    break;
                case AUTO:
                    captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
                case OFF:
                default:
                    captureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                    break;
            }

            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);

            /*ce code récupère la dernière image disponible, la convertit
            en un tableau d'octets et l'enregistre dans un fichier avec un
            nom unique dans le répertoire public des images de l'appareil.*/
            final String fileName = generateFileName();
            final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName);
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes, file);
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes, File file) {
                    try (FileOutputStream output = new FileOutputStream(file)) {
                        output.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            /*Cela permet de gérer le processus de capture d'image,
            du moment où l'image est disponible jusqu'à la création de
            l'aperçu de la caméra.*/
            reader.setOnImageAvailableListener(readerListener, null);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    startPreview();
                }
            };

            /*Ceci permet d'afficher le flux vidéo de la caméra dans l'application.*/
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            zoomIn();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            zoomOut();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void zoomIn() {
        if (zoomLevel < maximumZoomLevel) {
            zoomLevel += 0.1f;
            applyZoom();
        }
    }

    private void zoomOut() {
        if (zoomLevel > 1f) {
            zoomLevel -= 0.1f;
            applyZoom();
        }
    }

    private void applyZoom() {
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Rect rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            int cropW = (int) (rect.width() / zoomLevel);
            int cropH = (int) (rect.height() / zoomLevel);
            int cropX = (rect.width() - cropW) / 2;
            int cropY = (rect.height() - cropH) / 2;
            Rect zoomRect = new Rect(cropX, cropY, cropX + cropW, cropY + cropH);
            captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void closeCamera() {
        if (cameraCaptureSessions != null) {
            cameraCaptureSessions.close();
            cameraCaptureSessions = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = detector.getScaleFactor();
            if (scale > 1) {
                zoomIn();
            } else {
                zoomOut();
            }
            return true;
        }
    }

    private void setupCamera() {
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] jpegSizes = null;

            if (map != null) {
                jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
            }

            if (jpegSizes != null && jpegSizes.length > 0) {
                Size largest = Collections.max(Arrays.asList(jpegSizes), new CompareSizesByArea());
                captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
                captureRequestBuilder.set(CaptureRequest.JPEG_THUMBNAIL_SIZE, largest);
            }

            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 4);
            captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 1600);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            captureRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_HDR);

// Mise au point automatique continue
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

// Stabilisation d'image
            captureRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
            captureRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);

// Vitesse d'obturation
            captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 1000000000L / 1000);

// Balance des blancs manuelle pour la lumière du jour
            captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startCamera() {
        try {
            CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    try {
                        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        setupCamera(); // Appel de la méthode pour configurer la caméra après avoir initialisé captureRequestBuilder
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    cameraDevice.close();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int error) {
                    cameraDevice.close();
                }
            };

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cameraManager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //méthode de gestion des tailles
    private void setPreviewSize(int widthRatio, int heightRatio) {
        int width = imageDimension.getWidth();
        int height = imageDimension.getHeight();

        // Calculate the aspect ratio and set the new preview size
        imageDimension = new Size(width, width * heightRatio / widthRatio);

        // Re-create the camera preview with the new size
        startPreview();
    }

    //génération des noms pour les photos
    private String generateFileName() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String uuid = UUID.randomUUID().toString();
        return "IMG_" + timestamp + "_" + uuid + ".jpg";
    }

    /*
    @Override
    protected void onPause() {
        super.onPause();
        // Restaurer la luminosité d'origine
        setScreenBrightness(originalBrightness);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Régler la luminosité à 100% lorsque l'application revient en premier plan
        setScreenBrightness(255);
    }
*/

}

// Define CompareSizesByArea class
class CompareSizesByArea implements Comparator<Size> {
    @Override
    public int compare(Size lhs, Size rhs) {
        // We cast here to ensure the multiplications won't overflow
        return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                (long) rhs.getWidth() * rhs.getHeight());
    }
}
