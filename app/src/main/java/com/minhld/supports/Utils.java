package com.minhld.supports;

import android.content.Context;
import android.os.Environment;

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
    public static final int MESSAGE_INFO = 0x500 + 6;

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

        public XDevice (String address, String name) {
            this.address = address;
            this.name = name;
        }
    }

    /**
     * list of connected client devices that currently connect to current server<br>
     * this list will be used as iterating devices for sending, checking, etc...
     */
    public static ArrayList<XDevice> connectedDevices = new ArrayList<>();

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
    public static Object runRemote(Context c, String jobPath, Object srcObject, Class type) throws Exception {
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
        Method m = jobClass.getMethod(JOB_EXEC_METHOD, type);

        // address the resource
        return m.invoke(o, srcObject);
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

}
