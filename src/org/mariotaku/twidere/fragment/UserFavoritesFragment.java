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

package org.mariotaku.twidere.fragment;

import java.util.List;

import org.mariotaku.twidere.adapter.ParcelableStatusesAdapter;
import org.mariotaku.twidere.loader.UserFavoritesLoader;
import org.mariotaku.twidere.model.ParcelableStatus;

import android.os.Bundle;
import android.support.v4.content.Loader;

public class UserFavoritesFragment extends ParcelableStatusesListFragment {

	private boolean isAllItemsLoaded = false;

	@Override
	public boolean isListLoadFinished() {
		return isAllItemsLoaded;
	}

	@Override
	public Loader<List<ParcelableStatus>> newLoaderInstance(Bundle args) {
		if (args != null) {
			final long account_id = args.getLong(INTENT_KEY_ACCOUNT_ID);
			final long user_id = args.getLong(INTENT_KEY_USER_ID, -1);
			final long max_id = args.getLong(INTENT_KEY_MAX_ID, -1);
			final String screen_name = args.getString(INTENT_KEY_SCREEN_NAME);
			if (user_id != -1)
				return new UserFavoritesLoader(getActivity(), account_id, user_id, max_id, getData());
			else if (screen_name != null)
				return new UserFavoritesLoader(getActivity(), account_id, screen_name, max_id, getData());

		}
		return null;
	}

	@Override
	public void onDataLoaded(Loader<List<ParcelableStatus>> loader, ParcelableStatusesAdapter adapter) {
		if (loader instanceof UserFavoritesLoader) {
			final int total = ((UserFavoritesLoader) loader).getTotalItemsCount();
			isAllItemsLoaded = total != -1 && total == adapter.getCount();
		}
	}

}