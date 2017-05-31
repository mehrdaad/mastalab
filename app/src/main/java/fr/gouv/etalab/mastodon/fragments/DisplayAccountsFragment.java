package fr.gouv.etalab.mastodon.fragments;
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import fr.gouv.etalab.mastodon.client.Entities.Account;
import fr.gouv.etalab.mastodon.client.Entities.Error;
import fr.gouv.etalab.mastodon.helper.Helper;
import mastodon.etalab.gouv.fr.mastodon.R;
import fr.gouv.etalab.mastodon.asynctasks.RetrieveAccountsAsyncTask;
import fr.gouv.etalab.mastodon.drawers.AccountsListAdapter;
import fr.gouv.etalab.mastodon.interfaces.OnRetrieveAccountsInterface;


/**
 * Created by Thomas on 27/04/2017.
 * Fragment to display content related to accounts
 */
public class DisplayAccountsFragment extends Fragment implements OnRetrieveAccountsInterface {


    private TextView noAction;
    private boolean flag_loading;
    private Context context;
    private AsyncTask<Void, Void, Void> asyncTask;
    private AccountsListAdapter accountsListAdapter;
    private String max_id;
    private List<Account> accounts;
    private RetrieveAccountsAsyncTask.Type type;
    private RelativeLayout mainLoader, nextElementLoader, textviewNoAction;
    private boolean firstLoad;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int accountPerPage;
    private String targetedId;
    private boolean hideHeader = false;
    private boolean comesFromSearch = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_accounts, container, false);

        context = getContext();
        Bundle bundle = this.getArguments();
        accounts = new ArrayList<>();
        if (bundle != null) {
            type = (RetrieveAccountsAsyncTask.Type) bundle.get("type");
            targetedId = bundle.getString("targetedId", null);
            hideHeader = bundle.getBoolean("hideHeader", false);
            if( bundle.containsKey("accounts")){
                ArrayList<Parcelable> accountsReceived = bundle.getParcelableArrayList("accounts");
                assert accountsReceived != null;
                for(Parcelable account: accountsReceived){
                    accounts.add((Account)account);
                }
                comesFromSearch = true;
            }
        }
        max_id = null;
        firstLoad = true;
        flag_loading = true;


        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeContainer);
        SharedPreferences sharedpreferences = context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
        accountPerPage = sharedpreferences.getInt(Helper.SET_ACCOUNTS_PER_PAGE, 40);
        final ListView lv_accounts = (ListView) rootView.findViewById(R.id.lv_accounts);

        mainLoader = (RelativeLayout) rootView.findViewById(R.id.loader);
        nextElementLoader = (RelativeLayout) rootView.findViewById(R.id.loading_next_accounts);
        textviewNoAction = (RelativeLayout) rootView.findViewById(R.id.no_action);
        mainLoader.setVisibility(View.VISIBLE);
        nextElementLoader.setVisibility(View.GONE);
        accountsListAdapter = new AccountsListAdapter(context, type, this.accounts);
        lv_accounts.setAdapter(accountsListAdapter);

        if( !comesFromSearch) {
            lv_accounts.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {

                }

                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                    if (firstVisibleItem + visibleItemCount == totalItemCount) {
                        if (!flag_loading) {
                            flag_loading = true;
                            if (type != RetrieveAccountsAsyncTask.Type.FOLLOWERS && type != RetrieveAccountsAsyncTask.Type.FOLLOWING)
                                asyncTask = new RetrieveAccountsAsyncTask(context, type, max_id, DisplayAccountsFragment.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            else
                                asyncTask = new RetrieveAccountsAsyncTask(context, type, targetedId, max_id, DisplayAccountsFragment.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            nextElementLoader.setVisibility(View.VISIBLE);
                        }
                    } else {
                        nextElementLoader.setVisibility(View.GONE);
                    }
                }
            });

            //Hide account header when scrolling for ShowAccountActivity
            if (hideHeader) {
                lv_accounts.setOnScrollListener(new AbsListView.OnScrollListener() {
                    int lastFirstVisibleItem = 0;

                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {
                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                        if( firstVisibleItem == 0) {
                            Intent intent = new Intent(Helper.HEADER_ACCOUNT);
                            intent.putExtra("hide", false);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                        }else if (view.getId() == lv_accounts.getId() && totalItemCount > visibleItemCount) {
                            final int currentFirstVisibleItem = lv_accounts.getFirstVisiblePosition();

                            if (currentFirstVisibleItem > lastFirstVisibleItem) {
                                Intent intent = new Intent(Helper.HEADER_ACCOUNT);
                                intent.putExtra("hide", true);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            } else if (currentFirstVisibleItem < lastFirstVisibleItem) {
                                Intent intent = new Intent(Helper.HEADER_ACCOUNT);
                                intent.putExtra("hide", false);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            }
                            lastFirstVisibleItem = currentFirstVisibleItem;
                        }
                    }
                });
            }

            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    max_id = null;
                    accounts = new ArrayList<>();
                    firstLoad = true;
                    flag_loading = true;
                    if (type != RetrieveAccountsAsyncTask.Type.FOLLOWERS && type != RetrieveAccountsAsyncTask.Type.FOLLOWING)
                        asyncTask = new RetrieveAccountsAsyncTask(context, type, max_id, DisplayAccountsFragment.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    else
                        asyncTask = new RetrieveAccountsAsyncTask(context, type, targetedId, max_id, DisplayAccountsFragment.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            });
            swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent,
                    R.color.colorPrimary,
                    R.color.colorPrimaryDark);


            if (type != RetrieveAccountsAsyncTask.Type.FOLLOWERS && type != RetrieveAccountsAsyncTask.Type.FOLLOWING)
                asyncTask = new RetrieveAccountsAsyncTask(context, type, max_id, DisplayAccountsFragment.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                asyncTask = new RetrieveAccountsAsyncTask(context, type, targetedId, max_id, DisplayAccountsFragment.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }else {
            accountsListAdapter.notifyDataSetChanged();
            mainLoader.setVisibility(View.GONE);
            nextElementLoader.setVisibility(View.GONE);
            if( accounts == null || accounts.size() == 0 )
                textviewNoAction.setVisibility(View.VISIBLE);
        }
        return rootView;
    }



    @Override
    public void onCreate(Bundle saveInstance)
    {
        super.onCreate(saveInstance);
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    public void onStop() {
        super.onStop();
        if(asyncTask != null && asyncTask.getStatus() == AsyncTask.Status.RUNNING)
            asyncTask.cancel(true);
    }


    @Override
    public void onRetrieveAccounts(List<Account> accounts, Error error) {

        mainLoader.setVisibility(View.GONE);
        nextElementLoader.setVisibility(View.GONE);
        if( error != null){
            final SharedPreferences sharedpreferences = context.getSharedPreferences(Helper.APP_PREFS, Context.MODE_PRIVATE);
            boolean show_error_messages = sharedpreferences.getBoolean(Helper.SET_SHOW_ERROR_MESSAGES, true);
            if( show_error_messages)
                Toast.makeText(getContext(), error.getError(),Toast.LENGTH_LONG).show();
            return;
        }
        if( firstLoad && (accounts == null || accounts.size() == 0))
            textviewNoAction.setVisibility(View.VISIBLE);
        else
            textviewNoAction.setVisibility(View.GONE);
        if( accounts != null && accounts.size() > 1)
            max_id =accounts.get(accounts.size()-1).getId();
        else
            max_id = null;

        if( accounts != null) {
            for(Account tmpAccount: accounts){
                this.accounts.add(tmpAccount);
            }
            accountsListAdapter.notifyDataSetChanged();
        }
        swipeRefreshLayout.setRefreshing(false);
        firstLoad = false;
        flag_loading = accounts != null && accounts.size() < accountPerPage;
    }
}