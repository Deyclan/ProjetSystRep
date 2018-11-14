package com.brandon.wifip2p.tests;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

public class TestView extends View
{
    private short[] data;
    Paint paint = new Paint();
    Path path = new Path();
    float min, max;

    public TestView(Context context)
    {
        super(context);

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    void setData(short[] data)
    {
        min = Short.MAX_VALUE;
        max = Short.MIN_VALUE;
        this.data = data;
        for(int i = 0; i < data.length; i++)
        {
            if(data[i] < min)
                min = data[i];

            if(data[i] > max)
                max = data[i];
        }
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        canvas.drawRGB(255, 255, 255);
        if(data != null)
        {
            float interval = (float)this.getWidth()/data.length;
            for(int i = 0; i < data.length; i+=10)
                canvas.drawCircle(i*interval,(data[i]-min)/(max - min)*this.getHeight(),5 ,paint);

        }
        super.onDraw(canvas);
    }
}