package com.minhld.jobshare;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by minhld on 11/23/2015.
 */
public class UITools {
    public static final SimpleDateFormat SDF = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");

    /**
     * display confirmation YES/NO
     *
     * @param c
     * @param message
     * @param listener
     */
    public static void showYesNo(Context c, String message, final ConfirmListener listener){
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("confirm");
        builder.setMessage(message);

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (listener != null) {
                    listener.confirmed();
                }
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    public static void writeBitmapToFile(String outputBitmapPath, Bitmap bmp,
                                         boolean releaseBitmap) throws IOException {
        File file = new File(outputBitmapPath);
        FileOutputStream fOut = new FileOutputStream(file);

        bmp.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        fOut.flush();
        fOut.close();

        if (releaseBitmap) {
            bmp.recycle();
            System.gc();
        }
    }

    /**
     * create a scaled down bitmap with new width & height
     * but maintain the image ratio
     *
     * @param src
     * @param width
     * @return
     */
    public static Bitmap createScaleImage(Bitmap src, int width) {
        int height = (width * src.getHeight()) / src.getWidth();
        return Bitmap.createScaledBitmap(src, width, height, true);
    }

    /**
     * calculate the sample size
     *
     * @param bmp
     * @param resizedWidth
     * @return
     */
    public static int calculateInSampleSize(Bitmap bmp, int resizedWidth) {
        final int width = bmp.getWidth();
        final int height = bmp.getHeight();

        final int resizedHeight = (resizedWidth * height) / width;
        int inSampleSize = 1;

        if (height > resizedHeight || width > resizedWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > resizedHeight &&
                    (halfWidth / inSampleSize) > resizedWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * write the log out to the main screen
     *
     * @param c
     * @param log
     * @param msg
     */
    public static void writeLog(Activity c, final TextView log, final String msg){
        c.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log.append(SDF.format(new Date()) + ": " + msg + "\r\n");
            }
        });
    }

    /**
     * write the logout with prefix and exception
     *
     * @param c
     * @param log
     * @param prefix
     * @param e
     */
    public static void writeLog(Activity c, final TextView log, final String prefix, final Exception e){
        c.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log.append(SDF.format(new Date()) + ": [" + prefix + "] " + e.getMessage() + "\r\n");
                e.printStackTrace();
            }
        });
    }

    public interface ConfirmListener {
        public void confirmed();
    }

}
