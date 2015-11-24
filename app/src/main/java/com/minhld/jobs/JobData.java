package com.minhld.jobs;

import com.minhld.supports.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by minhld on 11/3/2015.
 */
public class JobData implements Serializable {
    public int index;
    // this is an another representation of bitmapData in binary data
    public byte[] byteData;
    public byte[] jobClass;

    public JobData() {
        this.index = 0;
        this.byteData = new byte[0];
        this.jobClass = new byte[0];
    }

    public JobData(int index, byte[] bmpData, byte[] jobClassBytes) {
        this.index = index;
        this.byteData = bmpData;
        this.jobClass = jobClassBytes;
    }

    public JobData(int index, byte[] bmpData, File jobClassFile) {
        this.index = index;

        // assign the binary data
        this.byteData = bmpData;

        // assign the job details data
        try {
            jobClass = Utils.readFile(jobClassFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * return a serialized byte array of the current job object
     *
     * @return
     */
    public byte[] toByteArray() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] thisBytesData = Utils.serialize(this);
            byte[] lengthBytes = Utils.intToBytes(thisBytesData.length);
            bos.write(lengthBytes, 0, lengthBytes.length);
            bos.write(thisBytesData, 0, thisBytesData.length);
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

}
