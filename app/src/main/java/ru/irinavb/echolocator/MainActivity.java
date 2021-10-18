package ru.irinavb.echolocator;

import android.graphics.Color;
import android.hardware.SensorEvent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private LineChart chart;

    final int duration = 5; // duration of sound
    final int sampleRate = 22050; // Hz (maximum frequency is 7902.13Hz (B8))
    final int freqInHz = 200; // Hz (maximum frequency is 7902.13Hz (B8))
    final int numSamples = duration * sampleRate;
    final double[] samples = new double[numSamples];
    final short[] buffer = new short[numSamples];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chart = findViewById(R.id.line_chart_up);

        // enable description text
        chart.getDescription().setEnabled(true);

        // enable touch gestures
        chart.setTouchEnabled(true);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(true);

        // set an alternative background color
        chart.setBackgroundColor(Color.WHITE);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);


        // add empty data
        chart.setData(data);


        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = chart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMaximum(10f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        chart.getAxisLeft().setDrawGridLines(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.setDrawBorders(false);
        findViewById(R.id.button_start).setOnClickListener(view -> {
            new Thread(() -> {
                for(int i = 0; i < 500; i++) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addEntry();
                        }
                    });
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            t2.start();
        });
    }

    private void addEntry() {
        LineData data = chart.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            float sample = (float) Math.sin(freqInHz * 2 * Math.PI * set.getEntryCount() / (sampleRate));
            data.addEntry(new Entry(set.getEntryCount(), sample + 5), 0);
            chart.notifyDataSetChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();

            // limit the number of visible entries
            chart.setVisibleXRangeMaximum(150);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            chart.moveViewToX(data.getEntryCount());
        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.MAGENTA);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }

    Thread t2 = new Thread() {
            public void run() {
                final int duration = 5; // duration of sound
                final int sampleRate = 22050; // Hz (maximum frequency is 7902.13Hz (B8))
                final int freqInHz = 200; // Hz (maximum frequency is 7902.13Hz (B8))
                final int numSamples = duration * sampleRate;
                final double[] samples = new double[numSamples];
                final short[] buffer = new short[numSamples];
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
//                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
//                        sampleRate, AudioFormat.CHANNEL_OUT_MONO,
//                        AudioFormat.ENCODING_PCM_16BIT, buffer.length,
//                        AudioTrack.MODE_STATIC);
//
//                audioTrack.write(buffer, 0, buffer.length);
//                audioTrack.play();
            }
        };

    @Override
    protected void onResume() {
        super.onResume();
        for (int i = 0; i < numSamples; ++i)
        {
            samples[i] = Math.sin(freqInHz * 2 * Math.PI * i / (sampleRate)); // Sine wave
            buffer[i] = (short) (samples[i] * Short.MAX_VALUE);  // Higher amplitude increases volume
        }
    }
}