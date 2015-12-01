package com.minhld.jobshare;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import com.minhld.jobs.JobDataParser;

import org.json.JSONObject;

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
    public String getJsonMetadata(Object objData) {
        Bitmap bmp = (Bitmap) objData;
        return "{ 'width': " + bmp.getWidth() + ", 'height': " + bmp.getHeight() + " }";
    }

    @Override
    public Object buildFinalObjectFromMetadata(String jsonMetadata) {
        try {
            JSONObject resultObj = new JSONObject(jsonMetadata);
            int width = resultObj.getInt("width"), height = resultObj.getInt("height");
            Bitmap finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            return finalBitmap;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Object mergeParts(Object finalObj, byte[] partObj, int index) {
        // get bitmap from original data
        Bitmap partBmp = BitmapFactory.decodeByteArray(partObj, 0, partObj.length);

        int pieceWidth = partBmp.getWidth();
        Canvas canvas = new Canvas((Bitmap) finalObj);
        canvas.drawBitmap(partBmp, index * pieceWidth, 0, null);
        return null;
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
