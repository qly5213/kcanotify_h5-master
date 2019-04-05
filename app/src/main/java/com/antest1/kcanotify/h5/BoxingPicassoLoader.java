package com.antest1.kcanotify.h5;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.widget.ImageView;

import com.bilibili.boxing.loader.IBoxingCallback;
import com.bilibili.boxing.loader.IBoxingMediaLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;

public class BoxingPicassoLoader implements IBoxingMediaLoader {

    @Override
    public void displayThumbnail(@NonNull ImageView img, @NonNull String absPath, int width, int height) {
        String path = "file://" + absPath;
        Picasso.with(img.getContext()).load(path).placeholder(R.drawable.ic_boxing_default_image).centerCrop().resize(width, height).into(img);
    }

    @Override
    public void displayRaw(@NonNull ImageView img, @NonNull String absPath, int width, int height, final IBoxingCallback callback) {
        String path = "file://" + absPath;
        RequestCreator creator = Picasso.with(img.getContext())
                .load(path);
        if (width > 0 && height > 0) {
            creator.transform(new BitmapTransform(width, height));
        }
        creator.into(img, new Callback() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError() {
                if (callback != null) {
                    callback.onFail(null);
                }
            }
        });
    }

    private class BitmapTransform implements Transformation {
        private final int mMaxWidth;
        private final int mMaxHeight;

        BitmapTransform(int maxWidth, int maxHeight) {
            this.mMaxWidth = maxWidth;
            this.mMaxHeight = maxHeight;
        }

        @Override
        public Bitmap transform(Bitmap source) {
            int targetWidth, targetHeight;
            double aspectRatio;

            if (source.getWidth() > source.getHeight()) {
                targetWidth = mMaxWidth;
                aspectRatio = (double) source.getHeight() / (double) source.getWidth();
                targetHeight = (int) (targetWidth * aspectRatio);
            } else {
                targetHeight = mMaxHeight;
                aspectRatio = (double) source.getWidth() / (double) source.getHeight();
                targetWidth = (int) (targetHeight * aspectRatio);
            }

            Bitmap result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
            if (result != source) {
                source.recycle();
            }
            return result;
        }

        @Override
        public String key() {
            return mMaxWidth + "x" + mMaxHeight;
        }

    }

}
