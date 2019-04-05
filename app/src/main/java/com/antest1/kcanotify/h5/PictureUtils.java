package com.antest1.kcanotify.h5;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class PictureUtils {
    /**
     * 将bitmap转为Base64字符串
     *
     * @param bitmap
     * @return base64字符串
     */
    public static String bitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
        byte[] bytes = outputStream.toByteArray();
        //Base64算法加密，当字符串过长（一般超过76）时会自动在中间加一个换行符，字符串最后也会加一个换行符。
        // 导致和其他模块对接时结果不一致。所以不能用默认Base64.DEFAULT，而是Base64.NO_WRAP不换行
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    /**
     * 将base64字符串转为bitmap
     * @param base64String
     * @return bitmap
     */
    public static Bitmap base64ToBitmap(String base64String) {

        byte[] bytes = Base64.decode(base64String, Base64.NO_WRAP);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return bitmap;

    }
}
