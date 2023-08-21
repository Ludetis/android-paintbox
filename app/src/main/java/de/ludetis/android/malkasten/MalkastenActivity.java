package de.ludetis.android.malkasten;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import androidx.core.content.FileProvider;
import de.ludetis.android.view.ColorChooserView;
import io.paperdb.Paper;


public class MalkastenActivity extends Activity implements View.OnTouchListener {

    private static final int JPEG_QUALITY = 90;
    private static final int RC_SAVE_IMG = 765;
    public static final String BITMAP = "bitmap";

    private final int[] colorPots = {};

    class Coord { public float x, y;
        Coord(float x, float y) {
            this.x = x;
            this.y = y;
        }
    };
    private Random rnd = new Random();
    private static final int IMG_WIDTH = 800;
    private static final int IMG_HEIGHT = 600;
    private static final float STROKE_WIDTH = 16;
    private static final int HIDE_SYSTEM_UI_DELAY = 3;
    private ImageView image;
    private Bitmap bitmap;
    private Paint paint;
    private float fw, fh;
    private Map<Integer,Coord> from = new HashMap<Integer, Coord>();
    private Handler handler = new Handler();
    private SharedPreferences prefs;

    private Runnable hideSystemUiRunnable = () -> hideSystemUi();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_malkasten);

        Paper.init(this);

        image = (ImageView) findViewById(R.id.image);
        image.setOnTouchListener(this);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        if (savedInstanceState != null && BITMAP.equalsIgnoreCase(savedInstanceState.getString(BITMAP))) {
            bitmap = Paper.book().read(BITMAP);
            image.setImageBitmap(bitmap);
            image.invalidate();
            Paper.book().delete(BITMAP);
        }
        else
            newBitmap();
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);

        DisplayMetrics dm = getResources().getDisplayMetrics() ;
        paint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, STROKE_WIDTH, dm) );
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        prefs = getSharedPreferences("paintbox",0);

        findViewById(R.id.trash).setOnClickListener(this::onClearClicked);

        findViewById(R.id.save).setOnClickListener(this::onSaveClicked);
        findViewById(R.id.save).setOnLongClickListener(this::onSaveLongClicked);

    }



    private boolean onSaveLongClicked(View view) {

        Animation shiver = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shiver);
        view.startAnimation(shiver);

        save();

        return true;
    }

    private void save() {

        Uri imageUri = getImageUri(this, bitmap);

        Intent i = new Intent(Intent.ACTION_SEND);

        i.setType("image/jpg");
        i.putExtra(Intent.EXTRA_TITLE, getString(R.string.default_filename)+".jpg");
        i.putExtra(Intent.EXTRA_STREAM, imageUri);
        i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> activities = packageManager.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolvedIntentInfo : activities) {
            final String packageName = resolvedIntentInfo.activityInfo.packageName;
            grantUriPermission(packageName, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        try {
            startActivity(i);
        } catch (android.content.ActivityNotFoundException ex) {
            // no app installed to save... should never happen.
        }

    }


    public Uri getImageUri(Context inContext, Bitmap inImage) {

        File imagePath = new File(getFilesDir(), "images");
        File newFile = new File(imagePath, PseudoUUID.create()+".jpg");

        try {
            imagePath.mkdirs();
            FileOutputStream fos = new FileOutputStream(newFile);
            inImage.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return FileProvider.getUriForFile(this, "de.ludetis.android.malkasten.fileprovider", newFile);

    }

    private void onClearClicked(View view) {
        Animation shiver = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shiver);
        view.startAnimation(shiver);
        newBitmap();
    }

    private void onSaveClicked(View view) {
        showToast(R.string.hold_to_share);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hideSystemUi();
        }

        if(prefs.getBoolean("showInfo1",true)) {
            showInfo();
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
        tv.setText(Html.fromHtml( getString(R.string.info)));
        ad.findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor ed = prefs.edit();
                ed.putBoolean("showInfo1",false);
                ed.commit();

                if(ad!=null) ad.dismiss();
            }
        });

        ad.show();

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
        //tvInfo.setText(String.format("%1$.2f",pressure)); // uncomment to have a pressure display
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                from.put(pointerId, new Coord(x, y));
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) handler.postDelayed(hideSystemUiRunnable, 1000 * HIDE_SYSTEM_UI_DELAY);
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                int id = pointerId;
                from.put(id, new Coord(x, y));
                return true;

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

                return true;

            case MotionEvent.ACTION_UP:
                Coord ce = from.get(pointerId);
                draw(ce.x, ce.y, x, y, pressure);
                return true;

            case MotionEvent.ACTION_POINTER_UP:
                Coord ce2 = from.get(pointerId);
                draw(ce2.x, ce2.y, x, y, pressure);
                for(int i=0; i<event.getHistorySize(); i++) {
                    ce2 = from.get(pointerId);
                    draw(ce2.x, ce2.y,   event.getHistoricalX(actionIndex,i), event.getHistoricalY(actionIndex,i), pressure);
                    ce2.x=event.getHistoricalX(actionIndex,i);
                    ce2.y=event.getHistoricalY(actionIndex,i);
                }
                return true;
        }

        return true;
    }

    private void notifyPressureCalibration(float pressure) {
        float minKnownPressure = prefs.getFloat("minPressure",1);
        float maxKnownPressure = prefs.getFloat("maxPressure",0);
        SharedPreferences.Editor ed = prefs.edit();
        if(pressure<minKnownPressure) {
            ed.putFloat("minPressure",pressure);
            Log.w("LOG", "minPressure now is " + pressure);
        }
        if(pressure>maxKnownPressure) {
            ed.putFloat("maxPressure",pressure);
            Log.w("LOG", "maxPressure now is " + pressure);
        }
        ed.apply();
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Paper.book().write(BITMAP,bitmap);
        outState.putString(BITMAP,"saved");
        super.onSaveInstanceState(outState);

    }


    private void showToast(int stringResId) {
        Toast toast = new Toast(this);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.setDuration(Toast.LENGTH_SHORT);
        TextView textView = new TextView(this);
        textView.setText(stringResId);
        textView.setTextColor(getResources().getColor(R.color.black));
        textView.setTextSize(32f);
        toast.setView(textView);
        toast.show();
    }


}
