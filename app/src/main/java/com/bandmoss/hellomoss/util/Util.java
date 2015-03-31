package com.bandmoss.hellomoss.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import com.bandmoss.hellomoss.R;
import com.bandmoss.hellomoss.model.UserInfo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rok on 2015. 3. 18..
 */
public class Util {

    private static MediaPlayer mediaPlayer;

    /**
     * @param context used to check the device version and DownloadManager information
     * @return true if the download manager is available
     */
    public static boolean isDownloadManagerAvailable(Context context) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
                return false;
            }
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setClassName("com.android.providers.downloads.ui", "com.android.providers.downloads.ui.DownloadList");
            List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            return list.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static long requestDownload(Context context, String url) {
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(uri.getLastPathSegment());
        request.setDescription(context.getString(R.string.app_name));
        request.setVisibleInDownloadsUi(true);

        // in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uri.getLastPathSegment());

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        return manager.enqueue(request);
    }

    /**
     * More info this method can be found at
     * http://developer.android.com/training/camera/photobasics.html
     *
     * @return
     * @throws java.io.IOException
     */
    public static File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }

    public static int getNavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public static boolean hasNavigationBar(Context context) {
        Resources resources = context.getResources();
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        return id > 0 && resources.getBoolean(id);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setTaskDescription(Activity activity, String label) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) try {
            int color = activity.getResources().getColor(R.color.colorPrimary);
            Bitmap icon = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_launcher);
            activity.setTaskDescription(new ActivityManager.TaskDescription(label, icon, color));
        } catch (Exception ignored) {

        }
    }

    public static void playIntro(Context context) {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();

            AssetFileDescriptor descriptor = context.getAssets().openFd("intro.mp3");
            mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mediaPlayer.setLooping(false);
            mediaPlayer.prepare();
            mediaPlayer.start();

            descriptor.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void requestUserInfo(final Callback<UserInfo> callback) {
        HttpGetTask asyncTask = new HttpGetTask("http://bandmoss.com/xe/index.php?mid=memberinfo&module_srl=134267", new Callback<String>() {
            @Override
            public void callback(String result) {
                UserInfo userInfo = null;
                if(result != null) {

                    String nickname = null;
                    String imageUrl = null;

                    Pattern pattern = Pattern.compile("\"id\\s*:\\s*.{2,5}\"");
                    Matcher matcher = pattern.matcher(result);
                    if (matcher.find()) {
                        nickname = matcher.group().replaceAll("[id :\"]", "");
                    }

                    pattern = Pattern.compile("http:\\/\\/.*(.gif)|(.jpg)|(.png)");
                    matcher = pattern.matcher(result);
                    if (matcher.find()) {
                        imageUrl = matcher.group();
                    }

                    if(nickname != null || imageUrl != null) {
                        userInfo = new UserInfo(nickname, imageUrl);
                    }
                }

                if(callback != null) {
                    callback.callback(userInfo);
                }
            }
        });

        asyncTask.execute();
    }


}
