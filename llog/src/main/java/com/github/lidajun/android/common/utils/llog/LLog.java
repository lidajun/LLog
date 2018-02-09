package com.github.lidajun.android.common.utils.llog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.appcompat.BuildConfig;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * 日志
 * Created by lidajun on 18-1-11.
 */

public class LLog {
    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    private static int sShowLevel = BuildConfig.DEBUG ? VERBOSE : -1;
    private static boolean isSave = false;
    private static int sSaveLevel = ERROR;
    @SuppressLint("StaticFieldLeak")
    private static Context sContext = null;
    private static StringBuilder sSb = new StringBuilder();
    private static File sFile;
    private static LogHandler sHandler;
    private static Throwable sThrowable = new Throwable();

    private LLog() {
    }

    public static void showLevel(int level) {
        sShowLevel = level;
    }

    public static void saveLevel(@NonNull Context context, @NonNull String dirPath, @NonNull String fileName, int saveLevel) throws IOException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                e("can`t make dir");
                return;
            }
        }
        sContext = context.getApplicationContext();
        isSave = true;
        sFile = new File(dirPath + File.separator + fileName);
        String baseInfo = getBaseInfo();
        i(baseInfo);
        if (!sFile.exists()) {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(sFile,true)));
            pw.println(baseInfo);
            pw.close();
            resetSb();
        }
        sSaveLevel = saveLevel;
        HandlerThread sHandlerThread = new HandlerThread("LLog");
        sHandlerThread.start();
        sHandler = new LogHandler(sHandlerThread.getLooper());
    }

    private static String getBaseInfo() {
        //appVersion
        String versionCode = String.valueOf(BuildConfig.VERSION_CODE);
        sSb.append("versionCode=").append(versionCode);
        String versionName = BuildConfig.VERSION_NAME;
        sSb.append("\nversionName=").append(versionName);
        //android 版本
        String OSVersion = Build.VERSION.RELEASE;
        sSb.append("\nOSVersion=").append(OSVersion);
        String SDK = String.valueOf(Build.VERSION.SDK_INT);
        sSb.append("\nSDK=").append(SDK);
        //手机厂商
        String manufacturer = Build.MANUFACTURER;
        sSb.append("\nmanufacturer=").append(manufacturer);
        //打机型号
        String model = Build.MODEL;
        sSb.append("\nmodel=").append(model);
        String cpu = Build.CPU_ABI;
        sSb.append("\ncpu=").append(cpu);
        //可申请内存
        String maxMemory = Formatter.formatFileSize(sContext, Runtime.getRuntime().maxMemory());
        sSb.append("\nmaxMemory=").append(maxMemory);
        String s = sSb.toString();
        resetSb();
        return s;
    }

    private static void resetSb() {
        if (sSb.length() > 0) {
            sSb.delete(0, sSb.length());
        }
    }

    public static void v(Object... o) {
        show(VERBOSE, null, o);
    }

    public static void d(Object... o) {
        show(DEBUG, null, o);
    }

    public static void i(Object... o) {
        show(INFO, null, o);
    }

    public static void w(Object... o) {
        show(WARN, null, o);
    }

    public static void e(Object... o) {
        show(ERROR, null, o);
    }

    public static void v(String tag, Object msg) {
        show(VERBOSE, tag, msg);
    }

    public static void d(String tag, Object msg) {
        show(DEBUG, tag, msg);
    }

    public static void i(String tag, String msg) {
        show(INFO, tag, msg);
    }

    public static void w(String tag, String msg) {
        show(WARN, tag, msg);
    }

    public static void e(String tag, String msg) {
        show(ERROR, tag, msg);
    }

    private static void show(int level, String tag, Object... o) {
        if (level >= sShowLevel) {
            if (null == tag) {
                tag = getTag();
            }
            switch (level) {
                case VERBOSE:
                    Log.v(tag, getString(o));
                    break;
                case DEBUG:
                    Log.d(tag, getString(o));
                    break;
                case INFO:
                    Log.i(tag, getString(o));
                    break;
                case WARN:
                    Log.w(tag, getString(o));
                    break;
                case ERROR:
                    Log.e(tag, getString(o));
                    break;
            }
        }
        if (level >= sSaveLevel) {
            save(level, tag, o);
        }
    }

    private static String getString(Object[] o) {
        if (o.length == 1 && o[0] instanceof String) {
            return (String) o[0];
        }
        for (Object anO : o) {
            sSb.append(anO.toString()).append(" ");
        }
        String s = sSb.toString();
        if (TextUtils.isEmpty(s)) {
            s = " ";
        } else {
            resetSb();
        }
        return s;
    }

    private static String getTag() {
        StackTraceElement[] trace = sThrowable.fillInStackTrace().getStackTrace();
        String callingClass = "";
        String caller = "";
        for (int i = 3; i < trace.length; i++) {
            StackTraceElement stackTraceElement = trace[i];
            Class<?> clazz = stackTraceElement.getClass();
            if (!clazz.equals(LLog.class)) {
                callingClass = stackTraceElement.getClassName();
                if (!callingClass.equals(LLog.class.getName())) {
                    callingClass = callingClass.substring(callingClass.lastIndexOf('.') + 1);
                    caller = stackTraceElement.getMethodName();
                    break;
                }
            }
        }
        return String.format(Locale.US, "[%s] %s: %s", Thread.currentThread().getName(), callingClass, caller);
    }

    private static void save(int level, String tag, Object... o) {
        if (isSave && sSaveLevel <= level) {
            if (null == tag) {
                tag = getTag();
            }
            String msg = getString(o);
            DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance();
            sSb.append(dateFormat.format(System.currentTimeMillis())).append(" level=").append(level).append(" ").append(tag).append(" ").append(msg);
            Message message = sHandler.obtainMessage();
            message.obj = sSb.toString();
            message.sendToTarget();
            resetSb();
        }
    }

    public static void reset() {
        isSave = false;
        sSb = null;
        sHandler = null;
        sContext = null;
        sFile = null;
        sSaveLevel = ERROR;
    }

    private static class LogHandler extends Handler {
        LogHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (null != msg.obj && msg.obj instanceof String) {
                writer(((String) msg.obj));
            }
        }

        private void writer(String msg) {
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(new BufferedWriter(new FileWriter(sFile, true)));
                pw.println(msg);
            } catch (IOException ignored) {

            } finally {
                if (pw != null) {
                    pw.close();
                }
            }
        }
    }
}
