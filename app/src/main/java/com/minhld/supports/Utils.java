package com.minhld.supports;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Environment;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import dalvik.system.DexClassLoader;

/**
 * Created by minhld on 9/22/2015.
 */
public class Utils {
    public static final int SERVER_PORT = 8881;
    public static final int SERVER_TIMEOUT = 5000;
    public static final int MESSAGE_READ_CLIENT = 0x500 + 1;
    public static final int MESSAGE_READ_SERVER = 0x500 + 2;
    public static final int MESSAGE_READ_JOB_SENT = 0x500 + 3;
    public static final int MESSAGE_READ_NO_FILE = 0x500 + 5;
    public static final int MY_HANDLE = 0x500 + 6;

    // same value as MESSAGE_READ_SERVER, because it will be used for replacing
    // each other sometimes.
    public static final int JOB_OK = 0x500 + 2;
    public static final int JOB_FAILED = -1;

    public static final int MAIN_JOB_DONE = 1;
    public static final int MAIN_INFO = -1;

    public static final SimpleDateFormat SDF = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    public static final String JOB_FILE_NAME = "Job.jar";
    public static final String JOB_CLASS_NAME = "com.minhld.jobs.Job";
    public static final String JOB_EXEC_METHOD = "exec";


    public enum SocketType {
        SERVER,
        CLIENT
    }

    public static class XDevice {
        public String address;
        public String name;

        public XDevice () {}

        public XDevice (String addr, String name) {
            this.address = addr;
            this.name = name;
        }
    }

    /**
     * list of connected client devices that currently connect to current server<br>
     * this list will be used as iterating devices for sending, checking, etc...
     */
    public static ArrayList<XDevice> connectedDevices = new ArrayList<>();

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

    /**
     * Serialize an object to binary array
     *
     * @param obj
     * @return
     * @throws IOException
     */
    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(obj);
        return b.toByteArray();
    }

    /**
     * Deserialize an object from a binary array
     *
     * @param bytes
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        ObjectInputStream o = new ObjectInputStream(b);
        return o.readObject();
    }

    /**
     * this function will execute a class that is stored in Download folder
     *
     * @param c
     * @return
     * @throws Exception
     */
    public static Object runRemote(Context c, String jobPath, Bitmap srcBmp) throws Exception {
        // check if the files are valid or not
        if (!new File(jobPath).exists()) {
            throw new Exception("job or data file does not exist");
        }

        // address the class object and its executable method
        String dex_dir = c.getDir("dex", 0).getAbsolutePath();
        ClassLoader parent  = c.getClass().getClassLoader();
        DexClassLoader loader = new DexClassLoader(jobPath, dex_dir, null, parent);
        Class jobClass = loader.loadClass(JOB_CLASS_NAME);
        Object o = jobClass.newInstance();
        Method m = jobClass.getMethod(JOB_EXEC_METHOD, Bitmap.class);

        // address the resource
        return (Bitmap) m.invoke(o, srcBmp);
    }

    /**
     * write data from a byte array to file
     *
     * @param outputFilePath
     * @param data
     * @throws IOException
     */
    public static void writeFile(String outputFilePath, byte[] data) throws IOException {
        FileOutputStream fos = new FileOutputStream(outputFilePath);
        fos.write(data, 0, data.length);
        fos.flush();
        fos.close();
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
     * read file and return binary array
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static byte[] readFile(File file) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(file);
        int read = 0;
        byte[] buff = new byte[1024];
        while ((read = fis.read(buff)) != -1) {
            bos.write(buff, 0, read);
        }
        return bos.toByteArray();
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

    public static byte[] intToBytes(int val) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(val).array();
    }

    public static int bytesToInt(byte[] arr) {
        return ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * get the absolute path of the default Download folder
     *
     * @return
     */
    public static String getDownloadPath() {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
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
