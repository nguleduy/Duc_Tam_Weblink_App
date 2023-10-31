package com.abaltatech.weblinkclientapp;

import android.content.Context;

import com.abaltatech.weblinkclient.appcatalog.IPlatformFileManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class AndroidFileManager implements IPlatformFileManager {

    private final Context mContext;

    public AndroidFileManager(Context context) {
        mContext = context;
    }

    @Override
    public FileInputStream openFileForReading(String path) {
        try {
            return new FileInputStream(new File(path));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public FileOutputStream openFileForWriting(String path) {
        try {
            return new FileOutputStream(new File(path));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public boolean fileExists(String path) {
        boolean result = false;

        FileInputStream fis = openFileForReading(path);
        if (fis != null) {
            result = true;
            try {
                fis.close();
            } catch (IOException e) {
                // No-op
            }
        }

        return result;
    }

    @Override
    public boolean deleteFile(String filePath) {
        return new File(filePath).delete();
    }

    @Override
    public String getUniqueFileName(String basePath) {
        String currentTime = new SimpleDateFormat("ddMMyyyy_HHmmssSSS", Locale.getDefault()).format(new Date());
        return new File(basePath, currentTime).getAbsolutePath();
    }
}
