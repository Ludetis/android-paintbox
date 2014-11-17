package de.ludetis.android.malkasten;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
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
    private static final float STROKE_WIDTH = 20;
    private static final float DIST_THRESHOLD = 1;
    private ImageView image;
    private boolean down;
    private Bitmap bitmap;
    private Paint paint;
    private float fw, fh;
    private float scale;
    private float pressureScale;
    private Map<Integer,Coord> from = new HashMap<Integer, Coord>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_malkasten);
        pressureScale = getResources().getDimension(R.dimen.pressure_scale);

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

        scale = getResources().getDisplayMetrics().density;

        SharedPreferences sp = getSharedPreferences("malkasten",0);
        if(sp.getBoolean("showInfo",true)) {
            showInfo();
        }

    }

    private void showInfo() {
        (new AlertDialog.Builder(this)).setMessage(R.string.info).setPositiveButton(android.R.string.ok,null).show();
        SharedPreferences sp = getSharedPreferences("malkasten",0);
        SharedPreferences.Editor edit = sp.edit();
        edit.putBoolean("showInfo",false);
        edit.commit();
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
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                from.put(pointerId, new Coord(x, y));
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

    private void draw(float x1, float y1, float x2, float y2, float pressure) {
        if(pressure>1f) pressure=1f;
        Canvas canvas = new Canvas(bitmap);
        paint.setColor(ColorChooserView.getPaintColor());
        if(x1==x1 && y1==y2) {
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x1*fw,y1*fh,scale*pressure*STROKE_WIDTH*pressureScale/2/2, paint);
        } else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(STROKE_WIDTH * pressure * pressureScale * scale/2);
            canvas.drawLine(x1*fw,y1*fh,x2*fw,y2*fh,paint);
        }
        image.invalidate();
    }
}
