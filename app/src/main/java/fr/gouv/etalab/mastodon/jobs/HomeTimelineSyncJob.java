package fr.gouv.etalab.mastodon.jobs;
/* Copyright 2017 Thomas Schneider
 *
 * This file is a part of Mastodon Etalab for mastodon.etalab.gouv.fr
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Mastodon Etalab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Thomas Schneider; if not,
 * see <http://www.gnu.org/licenses>. */

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import fr.gouv.etalab.mastodon.asynctasks.RetrieveHomeTimelineServiceAsyncTask;
import fr.gouv.etalab.mastodon.client.Entities.Account;
import fr.gouv.etalab.mastodon.client.Entities.Status;
import fr.gouv.etalab.mastodon.helper.Helper;
import fr.gouv.etalab.mastodon.interfaces.OnRetrieveHomeTimelineServiceInterface;
import fr.gouv.etalab.mastodon.sqlite.AccountDAO;
import fr.gouv.etalab.mastodon.sqlite.Sqlite;
import mastodon.etalab.gouv.fr.mastodon.R;

import static fr.gouv.etalab.mastodon.helper.Helper.HOME_TIMELINE_INTENT;
import static fr.gouv.etalab.mastodon.helper.Helper.notify_user;


/**
 * Created by Thomas on 20/05/2017.
 * Notifications for home timeline job
 */

public class HomeTimelineSyncJob extends Job implements OnRetrieveHomeTimelineServiceInterface{

    static final String HOME_TIMELINE = "home_timeline";
    private int notificationId;

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        callAsynchronousTask();
        return Result.SUCCESS;
    }


    public static int schedule(boolean updateCurrent){

        Set<JobRequest> jobRequests = JobManager.instance().getAllJobRequestsForTag(HOME_TIMELINE);
        if (!jobRequests.isEmpty() && !updateCurrent) {
            return jobRequests.iterator().next().getJobId();
        }
        return new JobRequest.Builder(HomeTimelineSyncJob.HOME_TIMELINE)
                .setPeriodic(TimeUnit.MINUTES.toMillis(Helper.MINUTES_BETWEEN_HOME_TIMELINE), TimeUnit.MINUTES.toMillis(5))
                .setPersisted(true)
                .setUpdateCurrent(updateCurrent)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(false)
                .build()
                .schedule();
    }


    /**
     * Task in background starts here.
     */
    private void callAsynchronousTask() {
        final SharedPreferences sharedpreferences = getContext().getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        boolean notif_hometimeline = sharedpreferences.getBoolean(Helper.SET_NOTIF_HOMETIMELINE, true);
        //User disagree with home timeline refresh
        if( !notif_hometimeline)
            return; //Nothing is done
        SQLiteDatabase db = Sqlite.getInstance(getContext(), Sqlite.DB_NAME, null, Sqlite.DB_VERSION).open();
        //If an Internet connection and user agrees with notification refresh
        //If WIFI only and on WIFI OR user defined any connections to use the service.
        if(!sharedpreferences.getBoolean(Helper.SET_WIFI_ONLY, false) || Helper.isOnWIFI(getContext())) {
            List<Account> accounts = new AccountDAO(getContext(),db).getAllAccount();
            //It means there is no user in DB.
            if( accounts == null )
                return;
            //Retrieve users in db that owner has.
            for (Account account: accounts) {
                String since_id = sharedpreferences.getString(Helper.LAST_HOMETIMELINE_MAX_ID + account.getAcct(), null);
                notificationId = (int) Math.round(Double.parseDouble(account.getId())/1000);
                new RetrieveHomeTimelineServiceAsyncTask(getContext(), since_id, account.getAcct(), HomeTimelineSyncJob.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            }
        }
    }


    @Override
    public void onRetrieveHomeTimelineService(List<Status> statuses, String acct) {
        if( statuses == null || statuses.size() == 0)
            return;
        Bitmap icon_notification = null;
        final SharedPreferences sharedpreferences = getContext().getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);

        String max_id = sharedpreferences.getString(Helper.LAST_HOMETIMELINE_MAX_ID + acct, null);
        //No previous notifications in cache, so no notification will be sent
        if( max_id != null ){
            String message;
            String title = null;
            for(Status status: statuses){
                //The notification associated to max_id is discarded as it is supposed to have already been sent
                if( status.getId().equals(max_id))
                    continue;
                String notificationUrl = status.getAccount().getAvatar();
                if( notificationUrl != null && icon_notification == null){
                    try {
                        ImageLoader imageLoaderNoty = ImageLoader.getInstance();
                        File cacheDir = new File(getContext().getCacheDir(), getContext().getString(R.string.app_name));
                        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getContext())
                                .threadPoolSize(5)
                                .threadPriority(Thread.MIN_PRIORITY + 3)
                                .denyCacheImageMultipleSizesInMemory()
                                .diskCache(new UnlimitedDiskCache(cacheDir))
                                .build();
                        imageLoaderNoty.init(config);
                        icon_notification = imageLoaderNoty.loadImageSync(notificationUrl);
                        title = getContext().getResources().getString(R.string.notif_pouet, status.getAccount().getDisplay_name());
                    }catch (Exception e){
                        icon_notification = BitmapFactory.decodeResource(getContext().getResources(),
                                R.drawable.mastodon_logo);
                    }
                }
            }
            if(statuses.size() > 0 )
                message = getContext().getResources().getQuantityString(R.plurals.other_notif_hometimeline, statuses.size(), statuses.size());
            else
                message = "";
            notify_user(getContext(), HOME_TIMELINE_INTENT, notificationId, icon_notification,title,message);
        }
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(Helper.LAST_HOMETIMELINE_MAX_ID + acct, statuses.get(0).getId());
        editor.apply();
    }



}
