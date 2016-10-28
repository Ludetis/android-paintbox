package de.ludetis.android.malkasten;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.ludetis.android.view.ColorChooserView;


public class MalkastenActivity extends Activity implements View.OnTouchListener {

    class Coord { public float x, y;
        Coord(float x, float y) {
            this.x = x;
            this.y = y;
        }
    };

    private static final int IMG_WIDTH = 800;
    private static final int IMG_HEIGHT = 600;
    private static final float STROKE_WIDTH = 32;
    private static final int HIDE_SYSTEM_UI_DELAY = 3;
    private ImageView image;
    private Bitmap bitmap;
    private Paint paint;
    private float fw, fh;
    private Map<Integer,Coord> from = new HashMap<Integer, Coord>();
    private Handler handler = new Handler();
    private TextView tvInfo;
    private SharedPreferences prefs;

    private Runnable hideSystemUiRunnable = new Runnable() {
        @Override
        public void run() {
            hideSystemUi();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_malkasten);

        image = (ImageView) findViewById(R.id.image);
        image.setOnTouchListener(this);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        newBitmap();
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        prefs = getSharedPreferences("paintbox",0);

        tvInfo = (TextView) findViewById(R.id.info);

        findViewById(R.id.trash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation shiver = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shiver);
                v.startAnimation(shiver);
                newBitmap();
            }
        });

        findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),R.string.hold_to_share,Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.share).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Animation shiver = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shiver);
                v.startAnimation(shiver);
                FileOutputStream out = null;
                File file = null;
                try {
                    File dir = new File(getCacheDir(), "images");
                    dir.mkdirs();
                    file = new File(dir, "img_" + System.currentTimeMillis() + ".jpg");
                    out = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 99, out);
                } catch (IOException e) {
                    Log.e("Malkasten", "Exception", e);
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                            shareImage(file);
                            return true;
                        }
                    } catch (IOException e) {
                        Log.e("Malkasten", "Exception", e);
                    }
                }
                return false;
            }
        });


        SharedPreferences sp = getSharedPreferences("malkasten",0);
        if(sp.getBoolean("showInfo",true)) {
            showInfo();
        }
        //showInfo(); // comment in to test this dialog
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hideSystemUi();
        }
    }





    private void hideSystemUi() {
        if("KFTT".equals(android.os.Build.MODEL) ) {
            return;
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void showInfo() {
        final Dialog ad = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        ad.setContentView(R.layout.dlg_welcome);
        TextView tv = (TextView) ad.findViewById(R.id.info);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(Html.fromHtml(getString(R.string.info)));
        ad.findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ad!=null) ad.dismiss();
            }
        });
        ad.show();
        SharedPreferences sp = getSharedPreferences("malkasten",0);
        SharedPreferences.Editor edit = sp.edit();
        edit.putBoolean("showInfo",false);
        edit.apply();
    }

    private void shareImage(File f) {
        Uri uriToImage = FileProvider.getUriForFile(this, "de.ludetis.android.malkasten.fileprovider", f);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uriToImage);
        shareIntent.setType("image/jpeg");
        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
    }


    private void newBitmap() {
        bitmap = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        image.setImageBitmap(bitmap);
        image.invalidate();
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if(fw==0) fw = 1f*IMG_WIDTH/image.getWidth();
        if(fh==0) fh = 1f*IMG_HEIGHT/image.getHeight();
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        float x = event.getX(actionIndex);
        float y = event.getY(actionIndex);
        float pressure = event.getPressure(actionIndex);
        notifyPressureCalibration(pressure);
        pressure = calcCalibratedPressure(pressure);
        //tvInfo.setText(String.format("%1$.2f",pressure)); // TODO uncomment to have a pressure display
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                from.put(pointerId, new Coord(x, y));
                handler.postDelayed(hideSystemUiRunnable, 1000 * HIDE_SYSTEM_UI_DELAY);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                int id = pointerId;
                from.put(id, new Coord(x, y));
                break;

            case MotionEvent.ACTION_MOVE:
                int numPointers = event.getPointerCount();
                for(int p=0; p<numPointers; p++) {
                    x= event.getX(p);
                    y= event.getY(p);
                    pointerId = event.getPointerId(p);
                    Coord c = from.get(pointerId);
                    draw(c.x, c.y, x, y, pressure);
                    c.x = x;
                    c.y = y;
                    for(int i=0; i<event.getHistorySize(); i++) {
                        c = from.get(pointerId);
                        draw(c.x, c.y,   event.getHistoricalX(p,i), event.getHistoricalY(p,i), pressure);
                        c.x=event.getHistoricalX(p,i);
                        c.y=event.getHistoricalY(p, i);
                    }
                }

                break;

            case MotionEvent.ACTION_UP:
                Coord ce = from.get(pointerId);
                draw(ce.x, ce.y, x, y, pressure);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                Coord ce2 = from.get(pointerId);
                draw(ce2.x, ce2.y, x, y, pressure);
                for(int i=0; i<event.getHistorySize(); i++) {
                    ce2 = from.get(pointerId);
                    draw(ce2.x, ce2.y,   event.getHistoricalX(actionIndex,i), event.getHistoricalY(actionIndex,i), pressure);
                    ce2.x=event.getHistoricalX(actionIndex,i);
                    ce2.y=event.getHistoricalY(actionIndex,i);
                }
                break;
        }

        // tell the system that we handled the event and no further processing is required
        return true;
    }

    private void notifyPressureCalibration(float pressure) {
        float minKnownPressure = prefs.getFloat("minPressure",1);
        float maxKnownPressure = prefs.getFloat("maxPressure",0);
        SharedPreferences.Editor ed = prefs.edit();
        if(pressure<minKnownPressure) {
            ed.putFloat("minPressure",pressure);
            Log.d("LOG", "minPressure now is " + pressure);
        }
        if(pressure>maxKnownPressure) {
            ed.putFloat("maxPressure",pressure);
            Log.d("LOG", "maxPressure now is " + pressure);
        }
        ed.commit();
    }

    private float calcCalibratedPressure(float pressure) {
        float minKnownPressure = prefs.getFloat("minPressure",0);
        float maxKnownPressure = prefs.getFloat("maxPressure",1);
        if(maxKnownPressure-minKnownPressure==0) return pressure;
        return pressure/(maxKnownPressure-minKnownPressure);
    }

    private void draw(float x1, float y1, float x2, float y2, float pressure) {
        if(pressure>1f) pressure=1f;
        Canvas canvas = new Canvas(bitmap);
        paint.setColor(ColorChooserView.getPaintColor());

        if(x1==x1 && y1==y2) {
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x1*fw,y1*fh,pressure*STROKE_WIDTH/2, paint);
        } else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(pressure*STROKE_WIDTH);
            canvas.drawLine(x1*fw,y1*fh,x2*fw,y2*fh,paint);
        }
        image.invalidate();
    }
}
