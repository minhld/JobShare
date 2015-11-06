package com.minhld.jobs;

import android.graphics.Bitmap;

import com.minhld.supports.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;

/**
 * Created by minhld on 11/3/2015.
 */
public class JobData implements Serializable {
    public int index;
    public byte[] data;
    public byte[] jobClass;

    public JobData() {
        this.index = 0;
        this.data = new byte[0];
        this.jobClass = new byte[0];
    }

    public JobData(int index, Bitmap bmpData, File jobClassFile) {
        try {
            // assign the binary data
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmpData.compress(Bitmap.CompressFormat.JPEG, 0, bos);
            data = bos.toByteArray();
            bos.close();

            // assign the job details data
            bos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(jobClassFile);
            int read = 0;
            while ((read = fis.read()) != -1) {
                bos.write(read);
            }
            jobClass = bos.toByteArray();
            bos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public byte[] toByteArray() {
//        try {
//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            int dataSize = this.data.length;
//            byte[] jobSizeBytes = Utils.serialize(new Long(jobSize));
//            bos.write(jobSizeBytes, 0, jobSizeBytes.length);
//            bos.write(this.data.s);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
