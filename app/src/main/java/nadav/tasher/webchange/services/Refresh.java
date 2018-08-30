package nadav.tasher.webchange.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Random;

import nadav.tasher.lightool.communication.network.Download;
import nadav.tasher.lightool.info.Device;
import nadav.tasher.webchange.R;
import nadav.tasher.webchange.architecture.Site;

import static nadav.tasher.lightool.info.Device.isOnline;
import static nadav.tasher.webchange.architecture.Center.check;
import static nadav.tasher.webchange.architecture.Center.getTempFile;
import static nadav.tasher.webchange.architecture.Center.md5;
import static nadav.tasher.webchange.architecture.Center.prefName;
import static nadav.tasher.webchange.architecture.Center.sitesPref;

public class Refresh extends JobService {

    private static final int ID = 102;
    private SharedPreferences sp;

    public static void reschedule(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Device.isJobServiceScheduled(context, ID)) {
                ComponentName serviceComponent = new ComponentName(context, Refresh.class);
                JobInfo.Builder builder = new JobInfo.Builder(ID, serviceComponent);
                builder.setMinimumLatency(context.getResources().getInteger(R.integer.background_loop_time));
                builder.setRequiresDeviceIdle(false);
                builder.setOverrideDeadline(context.getResources().getInteger(R.integer.background_loop_time));
                JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
                if (jobScheduler != null) {
                    jobScheduler.schedule(builder.build());
                }
            }
        }
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        startRefresh(jobParameters);
        return false;
    }

    private void startRefresh(JobParameters jobParameters) {
        sp = getSharedPreferences(prefName, MODE_PRIVATE);
        try {
            JSONArray siteArray = new JSONArray(sp.getString(sitesPref, new JSONArray().toString()));
            for (int i = 0; i < siteArray.length(); i++) {
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
                            reschedule(getApplicationContext());
                        }

                        @Override
                        public void onFailure(Exception e) {
                            reschedule(getApplicationContext());
                        }
                    }).execute();
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        jobFinished(jobParameters, false);
        reschedule(getApplicationContext());
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

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}