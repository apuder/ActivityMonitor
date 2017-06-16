package org.puder.activitymonitor;

import android.os.Environment;

import org.puder.activitymonitor.ann.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


class StorageUtil {

    private FileOutputStream out;


    StorageUtil(String name) {
        try {
            out = new FileOutputStream(createFileName(name));
        } catch (Exception e) {
            out = null;
        }
        ;
    }

    private static String getBaseDir() {
        File sdcard = Environment.getExternalStorageDirectory();
        String dirName = sdcard.getAbsolutePath() + File.separator + Config.STORAGE_DIR
                + File.separator;
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dirName;
    }

    static String createFileName(String name) {
        return getBaseDir() + name;
    }

    static void delete(String name) {
        File f = new File(createFileName(name));
        f.delete();
    }

    static List<String> getDirectoryListing() {
        File directory = new File(getBaseDir());
        File[] files = directory.listFiles();
        List<String> fileNames = new ArrayList<>();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                fileNames.add(files[i].getName());
            }
        }
        return fileNames;
    }

    void append(String row) {
        if (out == null) {
            return;
        }
        try {
            out.write(row.getBytes());
        } catch (IOException e) {
        }
    }

    void close() {
        try {
            out.close();
            out = null;
        } catch (IOException e) {
        }
    }
}
