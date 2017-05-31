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
package fr.gouv.etalab.mastodon.asynctasks;

import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

import fr.gouv.etalab.mastodon.client.API;
import fr.gouv.etalab.mastodon.helper.Helper;
import fr.gouv.etalab.mastodon.interfaces.OnRetrieveNotificationsInterface;
import fr.gouv.etalab.mastodon.client.Entities.Notification;


/**
 * Created by Thomas on 28/04/2017.
 * Retrieves notifications on the instance
 */

public class RetrieveNotificationsAsyncTask extends AsyncTask<Void, Void, Void> {

    private Context context;
    private List<Notification> notifications;
    private String max_id;
    private String acct, userId;
    private OnRetrieveNotificationsInterface listener;
    private String instance;
    private String token;
    private API api;


    public RetrieveNotificationsAsyncTask(Context context, String instance, String token, String max_id, String acct, String userId, OnRetrieveNotificationsInterface onRetrieveNotificationsInterface){
        this.context = context;
        this.max_id = max_id;
        this.listener = onRetrieveNotificationsInterface;
        this.acct = acct;
        this.instance = instance;
        this.userId = userId;
        this.token = token;
    }

    @Override
    protected Void doInBackground(Void... params) {

        api = new API(context, instance, token);
        if( acct == null)
            notifications = api.getNotifications(max_id);
        else
            notifications = api.getNotificationsSince(max_id);
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        listener.onRetrieveNotifications(notifications, acct, userId, api.getError());
    }

}