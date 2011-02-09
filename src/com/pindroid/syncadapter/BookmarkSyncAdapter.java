/*
 * PinDroid - http://code.google.com/p/PinDroid/
 *
 * Copyright (C) 2010 Matt Schmidt
 *
 * PinDroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * PinDroid is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PinDroid; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 */

package com.pindroid.syncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.pindroid.Constants;
import com.pindroid.client.PinboardApi;
import com.pindroid.client.Update;
import com.pindroid.platform.BookmarkManager;
import com.pindroid.platform.TagManager;
import com.pindroid.providers.BookmarkContent.Bookmark;
import com.pindroid.providers.TagContent.Tag;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * SyncAdapter implementation for syncing bookmarks.
 */
public class BookmarkSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "BookmarkSyncAdapter";

    private final Context mContext;

    public BookmarkSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {
    	
        try {
            InsertBookmarks(account, syncResult);
        } catch (final ParseException e) {
            syncResult.stats.numParseExceptions++;
            Log.e(TAG, "ParseException", e);
        } catch (final AuthenticationException e) {
            syncResult.stats.numAuthExceptions++;
            Log.e(TAG, "AuthException", e);
        } catch (final IOException e) {
            syncResult.stats.numIoExceptions++;
            Log.e(TAG, "IOException", e);
        }
    }
    
    private void InsertBookmarks(Account account, SyncResult syncResult) 
    	throws AuthenticationException, IOException{

    	final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
    	long lastUpdate = settings.getLong(Constants.PREFS_LAST_SYNC, 0);
    	final String username = account.name;

    	final Update update = PinboardApi.lastUpdate(account, mContext);
    	
    	if(update.getLastUpdate() > lastUpdate) {
	
			Log.d("BookmarkSync", "In Bookmark Load");
			final ArrayList<String> accounts = new ArrayList<String>();
			accounts.add(account.name);
			BookmarkManager.TruncateBookmarks(accounts, mContext, false);
			TagManager.TruncateTags(username, mContext);
			
			final ArrayList<Tag> tagList = PinboardApi.getTags(account, mContext);
			final ArrayList<Bookmark> addBookmarkList = PinboardApi.getAllBookmarks(null, account, mContext);
			
			if(!tagList.isEmpty()){
				TagManager.BulkInsert(tagList, username, mContext);
			}

			if(!addBookmarkList.isEmpty()){
				BookmarkManager.BulkInsert(addBookmarkList, username, mContext);
			}

    		final SharedPreferences.Editor editor = settings.edit();
    		editor.putLong(Constants.PREFS_LAST_SYNC, update.getLastUpdate());
            editor.commit();

    	} else {
    		Log.d("BookmarkSync", "No update needed.  Last update time before last sync.");
    	}
    }
}