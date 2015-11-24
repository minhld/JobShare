package com.minhld.jobs;

import java.io.IOException;

/**
 * this interface holds an abstraction of data parser. developer needs to override
 * this method to have the suitable parser for their data
 *
 * Created by minhld on 11/23/2015.
 */
public interface JobDataParser {
    public Class getDataClass();
    public Object readFile(String path) throws Exception;
    public byte[] parseToBytes(Object objData) throws Exception;
    public Object parseToObject(byte[] byteData) throws Exception;
    public Object getPartData(Object objData, int numOfParts, int index);
    public void destroy(Object data);
    public boolean isObjectDestroyed(Object data);
}
