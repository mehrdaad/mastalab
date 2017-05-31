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
package fr.gouv.etalab.mastodon.client;


import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import fr.gouv.etalab.mastodon.helper.Helper;

import static fr.gouv.etalab.mastodon.helper.Helper.USER_AGENT;

/**
 * Created by Thomas on 23/04/2017.
 * Client to call urls
 */

public class OauthClient {

    private static AsyncHttpClient client = new AsyncHttpClient();
    private String instance;


    public OauthClient(String instance){
        this.instance = instance;
    }

    public void get(String action, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        try {
            client.setConnectTimeout(10000); //10s timeout
            client.setUserAgent(USER_AGENT);
            client.setSSLSocketFactory(new MastalabSSLSocketFactory(MastalabSSLSocketFactory.getKeystore()));
            client.get(getAbsoluteUrl(action), params, responseHandler);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }
    }

    public void post(String action, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        try {
            client.setConnectTimeout(10000); //10s timeout
            client.setUserAgent(USER_AGENT);
            client.setSSLSocketFactory(new MastalabSSLSocketFactory(MastalabSSLSocketFactory.getKeystore()));
            client.post(getAbsoluteUrl(action), params, responseHandler);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }

    }

    private String getAbsoluteUrl(String action) {
        return "https://" + instance + action;
    }


}