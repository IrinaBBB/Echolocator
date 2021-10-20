package ru.irinavb.echolocator;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private LineChart chart;

    final int duration = 5; // duration of sound
    final int sampleRate = 22050; // Hz (maximum frequency is 7902.13Hz (B8))
    int freqInHz = 200; // Hz (maximum frequency is 7902.13Hz (B8))
    final int numSamples = duration * sampleRate;
    final double[] samples = new double[numSamples];
    final short[] buffer = new short[numSamples];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chart = findViewById(R.id.line_chart_up);
        createGraph();
        findViewById(R.id.button_start).setOnClickListener(view -> {
            playSound();
        });
    }

    private void createGraph() {
        LineDataSet set = new LineDataSet(dataValues(), freqInHz + " Hz");
        set.setLineWidth(2f);
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set);

        LineData data = new LineData(dataSets);
        chart.setData(data);
        chart.invalidate();
    }

    private void playSound() {
        for (int i = 0; i < numSamples; ++i)
        {
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

        for (int i = 0; i < 500; ++i)
        {
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
}