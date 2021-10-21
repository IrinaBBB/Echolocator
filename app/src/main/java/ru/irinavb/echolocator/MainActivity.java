package ru.irinavb.echolocator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    private LineChart chart;

    final int duration = 5; // duration of sound
    final int sampleRate = 22050; // Hz (maximum frequency is 7902.13Hz (B8))
    int freqInHz = 200; // Hz (maximum frequency is 7902.13Hz (B8))
    final int numSamples = duration * sampleRate;
    final double[] samples = new double[numSamples];
    final short[] buffer = new short[numSamples];
    int blockSize = 256;
    boolean started = false;

    AudioRecord audioRecord;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chart = findViewById(R.id.line_chart_up);

        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else
            initComponents();
    }

    private void initComponents() {
        createGraph();
        findViewById(R.id.button_start).setOnClickListener(view -> {
            playSound();
            /*Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                }
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Toast.makeText(this, "stop", Toast.LENGTH_SHORT).show();
            record();
        });
    }

    private void record() {
        RecordAudioTask recordAudioTask = new RecordAudioTask();
        this.started = true;
        recordAudioTask.execute();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.started = false;
    }

    private void createGraph() {
        LineDataSet set = new LineDataSet(dataValues(), freqInHz + " Hz");
        set.setLineWidth(3f);
        set.setDrawCircles(false);
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set);

        LineData data = new LineData(dataSets);
        chart.setData(data);
        chart.invalidate();
    }

    private void playSound() {
        for (int i = 0; i < numSamples; ++i) {
            samples[i] = Math.sin(freqInHz * 2 * Math.PI * i / (sampleRate)); // Sine wave
            buffer[i] = (short) (samples[i] * Short.MAX_VALUE);  // Higher amplitude increases volume
        }
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, buffer.length,
                AudioTrack.MODE_STATIC);

        audioTrack.write(buffer, 0, buffer.length);
        audioTrack.play();
    }

    private ArrayList<Entry> dataValues() {
        ArrayList<Entry> dataValues = new ArrayList<>();

        for (int i = 0; i < 500; ++i) {
            dataValues.add(new Entry(i,
                    (float) Math.sin(freqInHz * 2 * Math.PI * i / (sampleRate))));
        }
        return dataValues;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.frequency_200:
                item.setChecked(!item.isChecked());
                freqInHz = 200;
                createGraph();
                break;
            case R.id.frequency_350:
                item.setChecked(!item.isChecked());
                freqInHz = 350;
                createGraph();
                break;
            case R.id.frequency_500:
                item.setChecked(!item.isChecked());
                freqInHz = 500;
                createGraph();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initComponents();
            } else {
                Toast.makeText(this, "Record permission denial!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * RECORDING
     */
    private class RecordAudioTask extends AsyncTask<Void, double[], Boolean> {
        private int count = 0;
        @Override
        protected Boolean doInBackground(Void... voids) {
            int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            int bufferReadResult;

            short[] buffer = new short[blockSize];
            double[] toTransform = new double[blockSize];
            try {
                audioRecord.startRecording();
                while (started) {
                    if (count == 3) {
                        //Toast.makeText(MainActivity.this, "stop recording", Toast.LENGTH_SHORT)
                        // .show();
                        Log.d("Record", "doInBackground: stop");
                        Log.d("Record", "doInBackground: " + Arrays.toString(toTransform));
                        break;
                    } else {
                        bufferReadResult = audioRecord.read(buffer, 0, blockSize);
                        for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                            toTransform[i] = buffer[i] / 32768.0;
                        }

                        Log.d("Record", "doInBackground: " + Arrays.toString(toTransform));
                    }
                    count++;
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            return true;
        }
    }

}