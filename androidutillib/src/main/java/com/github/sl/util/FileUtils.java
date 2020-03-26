package com.github.sl.util;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;

import java.io.File;

public class FileUtils {


    public static final String HB_IAMGE_CACHE_PATH = "hbImageCache";

    /**
     * 获取Android私有files目录
     * data/data/package_name/files/
     * 应用卸载后删除
     *
     * @param context
     * @return
     */
    public static File getPrivateFileDir(Context context) {
        return context.getFilesDir();
    }

    /**
     * 获取Android私有缓存目录
     * data/data/package_name/cache/
     * 应用卸载后删除
     *
     * @param context
     * @return
     */
    public static File getPrivateCacheDir(Context context) {
        return context.getCacheDir();
    }

    /**
     * 获取Android私有根目录
     * data/data/package_name/
     * 应用卸载后删除
     *
     * @param context
     * @return
     */
    public static File getPrivateRootDir(Context context) {
        return context.getFilesDir().getParentFile();
    }

    /**
     * 获取Android SD卡目录
     * Android/data/package_name/files/{@param relativePath}/
     * 应用卸载后删除
     *
     * @param context
     * @param relativePath
     * @return
     */
    public static File getExternalFileDir(Context context, @NonNull String relativePath) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return context.getExternalFilesDir(relativePath);
        }
        return new File(getPrivateFileDir(context), relativePath);
    }

    /**
     * 获取Android SD卡目录
     * Android/data/package_name/cache/
     * 应用卸载后删除
     *
     * @param context
     * @return
     */
    public static File getExternalCacheDir(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return context.getExternalCacheDir();
        } else {
            return getPrivateCacheDir(context);
        }
    }


    /**
     * 获取Android SD卡目录
     * 10.0中 获取到的是 Android/sandbox/package_name/relativePath/   10.0沙盒机制，卸载删除
     * 10.0以下获取到的是 sd根目录/relativePath/ 卸载存在
     *
     * @param context
     * @param relativePath
     * @return
     */
    public static File getExternalPublicStorageDir(Context context, String relativePath) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //10.0
                return Environment.getExternalStoragePublicDirectory(relativePath);
            } else {
                return new File(Environment.getExternalStorageDirectory(), relativePath);
            }
        }
        return getExternalFileDir(context, relativePath);
    }
}
