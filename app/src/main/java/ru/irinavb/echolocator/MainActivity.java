package ru.irinavb.echolocator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import ru.irinavb.echolocator.classes.SoundGraphView;
import ru.irinavb.echolocator.classes.XYCoordinates;
import ru.irinavb.echolocator.fft.RealDoubleFFT;


public class MainActivity extends AppCompatActivity {

    private TextView distanceTextView;
    private ImageView graphImageView;

    private Bitmap graphBitMap;
    private Canvas graphCanvas;

    private Timer timer;
    private boolean isRunning = false;
    private int timeStep = 0;
    private Handler handler;

    private Button toggleStartButton;
    private SoundGraphView soundGraphView;
    private MediaPlayer mediaPlayer;
    private ToneGenerator toneGenerator;
    private final int blockSize = 256;
    File fileSpike;
    FileWriter streamSpike;
    private static final String FILE_NAME_SPIKE = "fileNameSpikeMaxValue.txt";

    //Source: https://stackoverflow.com/questions/5511250/capturing-sound-for-analysis-and-visualizing-frequencies-in-android
    private RealDoubleFFT fft;
    int freqInHz = 10000;
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private RecordAudio recordTask;
    private double currentFFTSpike = 0.0;
    private double currentDistance;
    private ArrayList<Double> fftAverageHolder;


    @Override
    public void onResume() {
        super.onResume();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());
        toggleStartButton = findViewById(R.id.playSound);

        fft = new RealDoubleFFT(blockSize);

        String pathSpikeMaxValue = this.getFilesDir().getAbsolutePath();
        fileSpike = new File(pathSpikeMaxValue, FILE_NAME_SPIKE);

        boolean hasMic = checkMicAvailability();
        distanceTextView = findViewById(R.id.calculated_distance_text_view);

        graphImageView = findViewById(R.id.ivGraph);
        graphImageView.setScaleY(-1);
        graphBitMap = Bitmap.createBitmap(420, 250, Bitmap.Config.ARGB_8888);
        graphCanvas = new Canvas(graphBitMap);

        graphImageView.setImageBitmap(graphBitMap);

        soundGraphView = new SoundGraphView(this);
        soundGraphView.draw(graphCanvas);

        toggleStartButton.setOnClickListener(view1 -> {
            if (!isRunning) {
                if (hasMic) {
                    askForRecordingPermission();
                } else {
                    Toast.makeText(this,
                            "The application cannot function without microphone",
                            Toast.LENGTH_SHORT).show();
                    playSound();
                }
            } else {
                toggleStart();
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    private class RecordAudio extends AsyncTask<Void, double[], Void> {
        //Source: https://stackoverflow.com/questions/5511250/capturing-sound-for-analysis-and-visualizing-frequencies-in-android
        @Override
        protected Void doInBackground(Void... arg0) {

            try {
                if (fftAverageHolder == null) {
                    fftAverageHolder = new ArrayList<>();
                }
                int bufferSize = AudioRecord.getMinBufferSize(freqInHz,
                        channelConfiguration, audioEncoding);
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Permission not given.", Toast.LENGTH_SHORT).show();
                }
                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, freqInHz,
                        channelConfiguration, audioEncoding, bufferSize);

                short[] buffer = new short[blockSize];
                double[] toTransform = new double[blockSize];

                audioRecord.startRecording();

                while (isRunning) {
                    if (fftAverageHolder.size() == 30) {
                        fftAverageHolder.clear();
                    }
                    int bufferReadResult = audioRecord.read(buffer, 0,
                            blockSize);

                    for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0; 
                    }
                    fft.ft(toTransform);
                    publishProgress(toTransform);
                    ArrayList<XYCoordinates> graphCoordinates = new ArrayList<>();
                    ArrayList<Double> frequency40 = new ArrayList<>();
                    for (int i = 0; i < toTransform.length; i++) {
                        XYCoordinates coordinate = new XYCoordinates((float) ((i + 8) * 1.55), (float) (toTransform[i] * 10) + 125);
                        if (i > 39 && i < 51) {
                            frequency40.add(toTransform[i] * 100);
                        }
                        graphCoordinates.add(coordinate);
                    }
                    double max40 = Collections.max(frequency40);
                    fftAverageHolder.add(max40);
                    if (fftAverageHolder.size() == 30) {
                        double total = 0.0;
                        for (int i = 0; i < 30; i++) {
                            total += fftAverageHolder.get(i);
                        }
                        currentFFTSpike = total / 30;
                    }
                    soundGraphView.setGraphCoordinates(graphCoordinates);
                }
                audioRecord.stop();
            } catch (Throwable t) {
                t.printStackTrace();
                Log.e("AudioRecord", "Recording Failed");
            }
            return null;
        }
    }

    private void saveDistance(double distance) {
        try {
            streamSpike = new FileWriter(fileSpike, true);
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date(System.currentTimeMillis());
            String now = dateFormat.format(date);
            streamSpike.write(now + "- distance: " + distance + "-type: fft");
            streamSpike.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                streamSpike.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean checkMicAvailability() {
        return this.getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_MICROPHONE);
    }

    //Source: https://developer.android.com/training/permissions/requesting#java
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void askForRecordingPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED) {
            startProgram();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this, "Needs dialog.", Toast.LENGTH_SHORT).show();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    //Source: https://developer.android.com/training/permissions/requesting#java
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startProgram();
                } else {
                    Toast.makeText(this, "This button will only play a sound.",
                            Toast.LENGTH_SHORT).show();
                }
            });

    private void startProgram() {
        isRunning = true;
        toggleStartButton.setText(R.string.button_stop_label);
        try {
            PrintWriter pw = new PrintWriter(fileSpike);
            pw.close();
        } catch (FileNotFoundException e) {
            Log.d("File empty", "error");
        }
        recordSound();
        playSound();
    }

    private void toggleStart() {
        isRunning = false;
        toggleStartButton.setText(R.string.button_start_label);
        stopRecording();
        stopSound();
    }

    private void playSound() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        toneGenerator.startTone(ToneGenerator.TONE_DTMF_1);
    }

    private void stopSound() {
        toneGenerator.stopTone();
        toneGenerator.release();
        toneGenerator = null;
    }

    private void recordSound() {
        recordTask = new RecordAudio();
        recordTask.execute();
        startDrawing();
    }


    private void stopRecording() {
        recordTask.cancel(true);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void startDrawing() {
        if (timer == null) {
            timer = new Timer();
            try {
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        drawGraph();
                        timeStep += 1;
                        if (timeStep % 20 == 0) {
                            currentDistance = calculateDistance(currentFFTSpike);
                            saveDistance(currentDistance);
                            handler.post(() -> updateDistanceText(currentDistance));
                            Log.d("Current pos", currentDistance + "");
                        }
                    }
                }, 0, 50);
            } catch (IllegalArgumentException | IllegalStateException iae) {
                iae.printStackTrace();
            }
        } else {
            timer = null;
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateDistanceText(double distance) {
        String stringDistanceSound = String.format(getString(R.string.distance_value), distance);
        distanceTextView.setText(stringDistanceSound);
    }

    public void drawGraph() {
        soundGraphView.setClose(currentDistance < 0.5);
        graphCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);
        soundGraphView.draw(graphCanvas);
        graphImageView.setImageBitmap(graphBitMap);
    }

    private double calculateDistance(double fft) {
        double x = (0.00007 * Math.pow(fft, 2)) - (0.0601 * fft) + 12.151;
        if (x < 0.0) {
            x = 0.0;
        }
        return x;
    }
}