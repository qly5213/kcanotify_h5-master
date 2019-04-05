package com.antest1.kcanotify.h5;

import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ChatFileUtils {

    private static int FREE_SD_SPACE_NEEDED_TO_CACHE = 1;
    private static int MB = 1024 * 1024;
    public static int freeSpaceOnSd() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory()
                .getPath());


        double sdFreeMB = ((double) stat.getAvailableBlocks() * (double) stat
                .getBlockSize()) / MB;


        return (int) sdFreeMB;
    }
    public static boolean saveBmpToSd(String dir, Bitmap bm, String filename,
                                      int quantity, boolean recyle) {
        boolean ret = true;
        if (bm == null) {
            return false;
        }


        if (FREE_SD_SPACE_NEEDED_TO_CACHE > freeSpaceOnSd()) {
            bm.recycle();
            bm = null;
            return false;
        }


        File dirPath = new File(dir);


        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }


        if (!dir.endsWith(File.separator)) {
            dir += File.separator;
        }


        File file = new File(dir + filename);
        OutputStream outStream = null;
        try {
            file.createNewFile();
            outStream = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, quantity, outStream);
            outStream.flush();
            outStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            ret = false;
        } catch (IOException e) {
            e.printStackTrace();
            ret = false;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            ret = false;
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (recyle && !bm.isRecycled()) {
                bm.recycle();
                bm = null;
                Log.e("BitmaptoCard", "saveBmpToSd, recyle");
            }
        }


        return ret;
    }
}
