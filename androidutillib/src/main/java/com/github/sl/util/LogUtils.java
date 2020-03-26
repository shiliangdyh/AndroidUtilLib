package com.github.sl.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Calendar;

/**
 * 日志工具类
 */
public class LogUtils {
    private static Context context;

    private LogUtils() {
    }

    private static final int TO_CONSOLE = 0x1;
    private static final int TO_SCREEN = 0x10;
    private static final int TO_FILE = 0x100;
    private static final int FROM_LOGCAT = 0x1000;

    private static final int LOG_MAXSIZE = 2 * 1024 * 1024;

    private static final String LOG_TEMP_FILE = "log.temp";
    private static final String LOG_LAST_FILE = "log_last.txt";

    private static final int LOG_LEVEL = Log.VERBOSE;

    private static final int DEBUG_ALL = TO_CONSOLE | TO_FILE;

    private static Calendar mDate = Calendar.getInstance();

    private static boolean DEBUG = true;

    public static void d(String tag, String msg) {
        log(tag, msg, DEBUG_ALL, Log.DEBUG);
    }

    public static void init(Context context) {
        LogUtils.context = context.getApplicationContext();
    }

    private static void log(String tag, String msg, int outDest, int level) {
        if (!DEBUG) {
            return;
        }

        if (TextUtils.isEmpty(tag)) {
            tag = "TAG_NULL";
        }
        if (TextUtils.isEmpty(msg)) {
            msg = "MSG_NULL";
        }

        if (level >= LOG_LEVEL) {

            if ((outDest & TO_SCREEN) != 0) {
                logToScreen(tag, msg, level);
            }

            if ((outDest & TO_FILE) != 0) {
                logToFile(tag, msg, level);
            }

            if ((outDest & FROM_LOGCAT) != 0) {
            }

            if ((outDest & TO_CONSOLE) != 0) {
                int max_str_length = 2001 - tag.length();
                //大于4000时
                while (msg.length() > max_str_length) {
                    logToConsole(tag, msg.substring(0, max_str_length), level);
                    msg = msg.substring(max_str_length);
                }
                //剩余部分
                logToConsole(tag, msg, level);
            }
        }
    }

    /**
     * 打印到控制台
     *
     * @param tag
     * @param msg
     * @param level
     */
    private static void logToConsole(String tag, String msg, int level) {
        switch (level) {
            case Log.DEBUG:
                Log.d(tag, msg);
                break;
            case Log.ERROR:
                Log.e(tag, msg);
                break;
            case Log.INFO:
                Log.i(tag, msg);
                break;
            case Log.WARN:
                Log.w(tag, msg);
                break;
        }
    }

    /**
     * 打印到文件
     *
     * @param tag
     * @param msg
     * @param level
     */
    private static void logToFile(String tag, String msg, int level) {
        synchronized (LogUtils.class) {
//            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                return;
//            }

            File file = new File(getLogFolder(), LOG_TEMP_FILE);
            long mFileSize = 0;

            FileOutputStream outStream = null;
            try {
                if (file.exists()) {
                    outStream = new FileOutputStream(file, true);
                    mFileSize = file.length();
                } else {
                    outStream = new FileOutputStream(file);
                    mFileSize = 0;
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (outStream != null) {
                try {
                    byte[] d = getLogStr(tag, msg).getBytes();

                    if (mFileSize < LOG_MAXSIZE) {
                        outStream.write(d);
                        outStream.write("\r\n".getBytes());
                        outStream.flush();
                        mFileSize += d.length;
                    } else {
                        try {
                            if (outStream != null) {
                                outStream.close();
                                outStream = null;
                                mFileSize = 0;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (renameLogFile()) {
                            logToFile(tag, msg, level);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean renameLogFile() {
        synchronized (LogUtils.class) {
            File file = new File(getLogFolder(), LOG_TEMP_FILE);
            File destFile = new File(getLogFolder(), LOG_LAST_FILE);
            if (destFile.exists()) {
                destFile.delete();
            }
            file.renameTo(destFile);
            if (file.exists()) {
                return file.delete();
            } else {
                return true;
            }
        }
    }

    private static String getLogStr(String tag, String msg) {
        mDate.setTimeInMillis(System.currentTimeMillis());

        StringBuilder buffer = new StringBuilder();
        buffer.setLength(0);
        buffer.append("[");
        buffer.append(tag);
        buffer.append(" : ");
        buffer.append(mDate.get(Calendar.MONTH) + 1);
        buffer.append("-");
        buffer.append(mDate.get(Calendar.DATE));
        buffer.append(" ");
        buffer.append(mDate.get(Calendar.HOUR_OF_DAY));
        buffer.append(":");
        buffer.append(mDate.get(Calendar.MINUTE));
        buffer.append(":");
        buffer.append(mDate.get(Calendar.SECOND));
        buffer.append(":");
        buffer.append(mDate.get(Calendar.MILLISECOND));
        buffer.append("] ");
        buffer.append(msg);

        return buffer.toString();
    }

    private static File getLogFolder() {
        File folder = null;

        String relativePath = getLogFile();
        folder = new File(context.getFilesDir(), relativePath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }

    private static String getLogFile() {
        String packageName = context.getPackageName();
        String[] list = packageName.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append("/").append(s);
        }
        return sb.toString();
    }

    private static void logToScreen(String tag, String msg, int level) {

    }
}
