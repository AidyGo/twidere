/*
 *				Twidere - Twitter client for Android
 * 
 * Copyright (C) 2012 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.util.content;

import static org.mariotaku.twidere.util.Utils.trim;
import static org.mariotaku.twidere.util.content.DatabaseUpgradeHelper.safeUpgrade;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.mariotaku.querybuilder.SQLQueryBuilder;
import org.mariotaku.querybuilder.query.SQLCreateViewQuery;
import org.mariotaku.twidere.Constants;
import org.mariotaku.twidere.provider.TweetStore.Accounts;
import org.mariotaku.twidere.provider.TweetStore.CachedHashtags;
import org.mariotaku.twidere.provider.TweetStore.CachedStatuses;
import org.mariotaku.twidere.provider.TweetStore.CachedTrends;
import org.mariotaku.twidere.provider.TweetStore.CachedUsers;
import org.mariotaku.twidere.provider.TweetStore.DirectMessages;
import org.mariotaku.twidere.provider.TweetStore.Drafts;
import org.mariotaku.twidere.provider.TweetStore.Filters;
import org.mariotaku.twidere.provider.TweetStore.Mentions;
import org.mariotaku.twidere.provider.TweetStore.Statuses;
import org.mariotaku.twidere.provider.TweetStore.Tabs;
import org.mariotaku.twidere.util.TwidereQueryBuilder.ConversationsEntryQueryBuilder;
import org.mariotaku.twidere.util.TwidereQueryBuilder.DirectMessagesQueryBuilder;

import java.util.HashMap;

public final class TwidereSQLiteOpenHelper extends SQLiteOpenHelper implements Constants {

	private final Context mContext;

	public TwidereSQLiteOpenHelper(final Context context, final String name, final int version) {
		super(context, name, null, version);
		mContext = context;
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {
		db.beginTransaction();
		db.execSQL(createTable(TABLE_ACCOUNTS, Accounts.COLUMNS, Accounts.TYPES, true));
		db.execSQL(createTable(TABLE_STATUSES, Statuses.COLUMNS, Statuses.TYPES, true));
		db.execSQL(createTable(TABLE_MENTIONS, Mentions.COLUMNS, Mentions.TYPES, true));
		db.execSQL(createTable(TABLE_DRAFTS, Drafts.COLUMNS, Drafts.TYPES, true));
		db.execSQL(createTable(TABLE_CACHED_USERS, CachedUsers.COLUMNS, CachedUsers.TYPES, true));
		db.execSQL(createTable(TABLE_CACHED_STATUSES, CachedStatuses.COLUMNS, CachedStatuses.TYPES, true));
		db.execSQL(createTable(TABLE_CACHED_HASHTAGS, CachedHashtags.COLUMNS, CachedHashtags.TYPES, true));
		db.execSQL(createTable(TABLE_FILTERED_USERS, Filters.Users.COLUMNS, Filters.Users.TYPES, true));
		db.execSQL(createTable(TABLE_FILTERED_KEYWORDS, Filters.Keywords.COLUMNS, Filters.Keywords.TYPES, true));
		db.execSQL(createTable(TABLE_FILTERED_SOURCES, Filters.Sources.COLUMNS, Filters.Sources.TYPES, true));
		db.execSQL(createTable(TABLE_FILTERED_LINKS, Filters.Links.COLUMNS, Filters.Links.TYPES, true));
		db.execSQL(createTable(TABLE_DIRECT_MESSAGES_INBOX, DirectMessages.Inbox.COLUMNS, DirectMessages.Inbox.TYPES,
				true));
		db.execSQL(createTable(TABLE_DIRECT_MESSAGES_OUTBOX, DirectMessages.Outbox.COLUMNS,
				DirectMessages.Outbox.TYPES, true));
		db.execSQL(createTable(TABLE_TRENDS_LOCAL, CachedTrends.Local.COLUMNS, CachedTrends.Local.TYPES, true));
		db.execSQL(createTable(TABLE_TABS, Tabs.COLUMNS, Tabs.TYPES, true));
		db.execSQL(createDirectMessagesView().getSQL());
		db.execSQL(createDirectMessageConversationEntriesView().getSQL());
		db.setTransactionSuccessful();
		db.endTransaction();
	}

	@Override
	public void onDowngrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
		handleVersionChange(db);
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
		handleVersionChange(db);
		if (oldVersion <= 43 && newVersion >= 44) {
			final ContentValues values = new ContentValues();
			final SharedPreferences prefs = mContext
					.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
			// Here I use old consumer key/secret because it's default key for
			// older versions
			final String pref_consumer_key = prefs.getString(PREFERENCE_KEY_CONSUMER_KEY, TWITTER_CONSUMER_KEY);
			final String pref_consumer_secret = prefs
					.getString(PREFERENCE_KEY_CONSUMER_SECRET, TWITTER_CONSUMER_SECRET);
			values.put(Accounts.CONSUMER_KEY, trim(pref_consumer_key));
			values.put(Accounts.CONSUMER_SECRET, trim(pref_consumer_secret));
			db.update(TABLE_ACCOUNTS, values, null, null);
		}
	}

	private SQLCreateViewQuery createDirectMessageConversationEntriesView() {
		final SQLCreateViewQuery.Builder qb = SQLQueryBuilder.createView(true,
				DirectMessages.ConversationEntries.TABLE_NAME);
		qb.as(ConversationsEntryQueryBuilder.build());
		return qb.build();
	}

	private SQLCreateViewQuery createDirectMessagesView() {
		final SQLCreateViewQuery.Builder qb = SQLQueryBuilder.createView(true, DirectMessages.TABLE_NAME);
		qb.as(DirectMessagesQueryBuilder.build());
		return qb.build();
	}

	private void handleVersionChange(final SQLiteDatabase db) {
		final HashMap<String, String> accountsAlias = new HashMap<String, String>();
		final HashMap<String, String> filtersAlias = new HashMap<String, String>();
		final HashMap<String, String> draftsAlias = new HashMap<String, String>();
		accountsAlias.put(Accounts.SCREEN_NAME, "username");
		accountsAlias.put(Accounts.NAME, "username");
		accountsAlias.put(Accounts.ACCOUNT_ID, "user_id");
		accountsAlias.put(Accounts.SIGNING_OAUTH_BASE_URL, "oauth_rest_base_url");
		filtersAlias.put(Filters.VALUE, "text");
		draftsAlias.put(Drafts.MEDIA_URI, "image_uri");
		draftsAlias.put(Drafts.MEDIA_TYPE, "attached_image_type");
		safeUpgrade(db, Accounts.TABLE_NAME, Accounts.COLUMNS, Accounts.TYPES, true, false, accountsAlias);
		safeUpgrade(db, Statuses.TABLE_NAME, Statuses.COLUMNS, Statuses.TYPES, true, true, null);
		safeUpgrade(db, TABLE_MENTIONS, Mentions.COLUMNS, Mentions.TYPES, true, true, null);
		safeUpgrade(db, TABLE_DRAFTS, Drafts.COLUMNS, Drafts.TYPES, true, false, draftsAlias);
		safeUpgrade(db, TABLE_CACHED_USERS, CachedUsers.COLUMNS, CachedUsers.TYPES, true, true, null);
		safeUpgrade(db, TABLE_CACHED_STATUSES, CachedStatuses.COLUMNS, CachedStatuses.TYPES, true, true, null);
		safeUpgrade(db, TABLE_CACHED_HASHTAGS, CachedHashtags.COLUMNS, CachedHashtags.TYPES, true, true, null);
		safeUpgrade(db, TABLE_FILTERED_USERS, Filters.Users.COLUMNS, Filters.Users.TYPES, true, false, filtersAlias);
		safeUpgrade(db, TABLE_FILTERED_KEYWORDS, Filters.Keywords.COLUMNS, Filters.Keywords.TYPES, true, false,
				filtersAlias);
		safeUpgrade(db, TABLE_FILTERED_SOURCES, Filters.Sources.COLUMNS, Filters.Sources.TYPES, true, false,
				filtersAlias);
		safeUpgrade(db, TABLE_FILTERED_LINKS, Filters.Links.COLUMNS, Filters.Links.TYPES, true, false, filtersAlias);
		safeUpgrade(db, TABLE_DIRECT_MESSAGES_INBOX, DirectMessages.Inbox.COLUMNS, DirectMessages.Inbox.TYPES, true,
				true, null);
		safeUpgrade(db, TABLE_DIRECT_MESSAGES_OUTBOX, DirectMessages.Outbox.COLUMNS, DirectMessages.Outbox.TYPES, true,
				true, null);
		safeUpgrade(db, TABLE_TRENDS_LOCAL, CachedTrends.Local.COLUMNS, CachedTrends.Local.TYPES, true, true, null);
		safeUpgrade(db, TABLE_TABS, Tabs.COLUMNS, Tabs.TYPES, true, false, null);
		db.execSQL(createDirectMessagesView().getSQL());
		db.execSQL(createDirectMessageConversationEntriesView().getSQL());
	}

	private static String createTable(final String tableName, final String[] columns, final String[] types,
			final boolean create_if_not_exists) {
		if (tableName == null || columns == null || types == null || types.length != columns.length
				|| types.length == 0)
			throw new IllegalArgumentException("Invalid parameters for creating table " + tableName);
		final StringBuilder stringBuilder = new StringBuilder(create_if_not_exists ? "CREATE TABLE IF NOT EXISTS "
				: "CREATE TABLE ");

		stringBuilder.append(tableName);
		stringBuilder.append(" (");
		final int length = columns.length;
		for (int n = 0, i = length; n < i; n++) {
			if (n > 0) {
				stringBuilder.append(", ");
			}
			stringBuilder.append(columns[n]).append(' ').append(types[n]);
		}
		return stringBuilder.append(");").toString();
	}

}
