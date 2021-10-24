package ru.irinavb.echolocator.classes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;


public class SoundGraphView extends View {

    private Paint borderPaint, pathPaint, circlePaint;

    private Path pathBorder, pathGraph;

    private ArrayList<XYCoordinates> graphCoordinates;
    private boolean isClose = false;


    public SoundGraphView(Context context) {
        super(context);
        init();
    }

    public SoundGraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SoundGraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    public void init() {
        graphCoordinates = new ArrayList<>();
        pathBorder = new Path();
        pathGraph = new Path();

        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.GRAY);

        pathPaint = new Paint();
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setColor(Color.GREEN);

        circlePaint = new Paint();
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(Color.BLUE);

        pathBorder = new Path();
        pathGraph = new Path();

    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        pathGraph.reset();
        pathBorder.moveTo(30, canvas.getHeight() - 200);
        pathBorder.lineTo(canvas.getWidth() - 30, canvas.getHeight() - 200);
        pathBorder.moveTo(30, 200);
        pathBorder.lineTo(canvas.getWidth() - 30, 200);
        if (isClose) {
            pathPaint.setColor(Color.MAGENTA);
        } else {
            pathPaint.setColor(Color.GREEN);
        }

        for (int i = 1; i < graphCoordinates.size(); i++) {
            pathGraph.moveTo(graphCoordinates.get(i - 1).getX(), graphCoordinates.get(i - 1).getY());
            pathGraph.lineTo(graphCoordinates.get(i).getX(), graphCoordinates.get(i).getY());
            canvas.drawPath(pathGraph, pathPaint);
        }

        canvas.drawPath(pathBorder, borderPaint);
        invalidate();
    }

    public void setGraphCoordinates(ArrayList<XYCoordinates> graphCoordinates) {
        this.graphCoordinates = graphCoordinates;
    }

    public void setClose(boolean close) {
        isClose = close;
    }
}
