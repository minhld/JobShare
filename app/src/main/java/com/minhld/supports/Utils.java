package com.minhld.supports;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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

    public static final SimpleDateFormat SDF = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");

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
            bos.write(buff, 0, buff.length);
        }
        return bos.toByteArray();
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

    public interface ConfirmListener {
        public void confirmed();
    }
}
