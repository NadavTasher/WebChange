package nadav.tasher.webchange.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Calendar;
import java.util.Random;

import nadav.tasher.lightool.communication.network.Download;
import nadav.tasher.webchange.R;
import nadav.tasher.webchange.architecture.Site;

import static nadav.tasher.lightool.info.Device.isOnline;
import static nadav.tasher.webchange.architecture.Center.check;
import static nadav.tasher.webchange.architecture.Center.getTempFile;
import static nadav.tasher.webchange.architecture.Center.md5;
import static nadav.tasher.webchange.architecture.Center.prefName;
import static nadav.tasher.webchange.architecture.Center.sitesPref;

public class Refresh extends Service {

    private static final int ID = 102;
    private SharedPreferences sp;
    private int i;

    public static void reschedule(Context context) {
        Calendar cal = Calendar.getInstance();
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent serviceIntent = new Intent(context, Refresh.class);
        PendingIntent servicePendingIntent =
                PendingIntent.getService(context,
                        ID,
                        serviceIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);
        if (am != null) {
            am.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    cal.getTimeInMillis(),
                    context.getResources().getInteger(R.integer.background_loop_time),
                    servicePendingIntent
            );
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startRefresh();
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startRefresh() {
        sp = getSharedPreferences(prefName, MODE_PRIVATE);
        try {
            final JSONArray siteArray = new JSONArray(sp.getString(sitesPref, new JSONArray().toString()));
            for (i = 0; i < siteArray.length(); i++) {
                JSONObject currentSite = siteArray.getJSONObject(i);
                final Site mSite = Site.fromJSON(currentSite);
                if (isOnline(getApplicationContext())) {

                    mSite.getDownload(getTempFile(getApplicationContext()), new Download.Callback() {
                        @Override
                        public void onSuccess(File file) {
                            if (check(mSite, file)) {
                                mSite.setSum(md5(file));
                                inform(mSite.getUrl().replaceAll("(^([a-z]+)://)|(/.+)|(/)", "").toLowerCase() + " changed!", "Tap to open", mSite.getUrl());
                            }
                            if (i >= siteArray.length()) stopSelf();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (i >= siteArray.length()) stopSelf();
                        }
                    }).execute();
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        reschedule(getApplicationContext());
    }

    private void inform(String title, String message, String url) {
        Intent openUrl = new Intent(Intent.ACTION_VIEW);
        openUrl.setData(Uri.parse(url));
        Notification.Builder mBuilder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(
                        this,
                        0,
                        openUrl,
                        PendingIntent.FLAG_UPDATE_CURRENT
                ))
                .setDefaults(Notification.DEFAULT_ALL);
        NotificationManager mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (mManager != null) {
            if (Build.VERSION.SDK_INT >= 26) {
                mManager.createNotificationChannel(new NotificationChannel(getString(R.string.app_name), getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT));
                mBuilder.setChannelId(getString(R.string.app_name));
            }
            mManager.notify(getString(R.string.app_name), new Random().nextInt(1000), mBuilder.build());
        }
    }
}