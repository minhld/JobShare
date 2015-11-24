package com.minhld.jobshare;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.minhld.jobs.JobDataParser;

import java.io.ByteArrayOutputStream;

/**
 *
 * Created by minhld on 11/23/2015.
 */
public class BitmapJobDataParser implements JobDataParser {

    @Override
    public Class getDataClass() {
        return Bitmap.class;
    }

    @Override
    public Object readFile(String path) throws Exception {
        return BitmapFactory.decodeFile(path);
    }

    @Override
    public Object parseToObject(byte[] byteData) throws Exception {
        return BitmapFactory.decodeByteArray(byteData, 0, byteData.length);
    }

    @Override
    public byte[] parseToBytes(Object objData) throws Exception {
        Bitmap bmpData = (Bitmap) objData;

        // assign the binary data
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmpData.compress(Bitmap.CompressFormat.JPEG, 0, bos);
        byte[] byteData = bos.toByteArray();
        bos.close();

        return byteData;
    }

    @Override
    public Object getPartData(Object data, int numOfParts, int index) {
        Bitmap bmpData = (Bitmap) data;
        int pieceWidth = bmpData.getWidth() / numOfParts;
        return Bitmap.createBitmap(bmpData, (pieceWidth * index), 0, pieceWidth, bmpData.getHeight());
    }

    @Override
    public void destroy(Object data) {
        ((Bitmap) data).recycle();
    }

    @Override
    public boolean isObjectDestroyed(Object data) {
        return ((Bitmap) data).isRecycled();
    }
}
