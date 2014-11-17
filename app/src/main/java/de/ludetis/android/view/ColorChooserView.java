package de.ludetis.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import de.ludetis.android.malkasten.R;

/**
 * Created by uwe on 07.10.14.
 */
public class ColorChooserView extends View implements View.OnClickListener {

    public static Drawable bgDrawable;
    private static Rect rectSrc;
    private static Paint paint;
    private int color;
    private static int paintColor=Color.BLACK;
    //private Animation shiver;

    public ColorChooserView(Context context) {
        super(context);
        init();
    }


    public ColorChooserView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.ColorChooserView, 0, 0);
        color = a.getColor(R.styleable.ColorChooserView_color, android.R.color.holo_blue_light);
        a.recycle();
        init();
    }

    public ColorChooserView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.ColorChooserView, 0, 0);
        color = a.getColor(R.styleable.ColorChooserView_color, android.R.color.holo_blue_light);
        a.recycle();
        init();
    }

    private void init() {
        setOnClickListener(this);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        //shiver = AnimationUtils.loadAnimation(getContext(), R.anim.shiver);
    }

    @Override
    public void draw(Canvas canvas) {
        if(bgDrawable==null) {
            bgDrawable = getResources().getDrawable(R.drawable.farbe);
            rectSrc = new Rect(0,0,127,127);
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
        }
        paint.setColor(color);

        canvas.drawCircle(canvas.getWidth()/2, canvas.getHeight()/2, canvas.getWidth()*0.4f, paint);
        canvas.drawBitmap(((BitmapDrawable) bgDrawable).getBitmap(), null, canvas.getClipBounds(), paint) ;
    }

    @Override
    public void onClick(View v) {
        paintColor = color;
        //v.startAnimation(shiver);
    }

    public static int getPaintColor() {
        return paintColor;
    }
}
