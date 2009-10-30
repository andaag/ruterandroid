/**
 *     Copyright (C) 2009 Anders Aagaard <aagaande@gmail.com>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.neuron.trafikanten.tasks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.DataProviderFactory;
import com.neuron.trafikanten.dataProviders.ISearchProvider;

public class SearchStationTask extends GenericTask {
	public static final int TASK_SEARCH = 101;
	private static final String KEY_QUERY = "query";
	private static final String KEY_LATITUDE = "latitude";
	private static final String KEY_LONGITUDE = "longitude";
	private ISearchProvider searchProvider;
	
	/*
	 * Saved instance data
	 */
	private String query;
	private double latitude;
	private double longitude;
	
	public static void StartTask(Activity activity, String query) {
		final Intent intent = new Intent(activity, SearchStationTask.class);
		intent.putExtra(KEY_QUERY, query);
		StartGenericTask(activity, intent, TASK_SEARCH);
	}
	
	public static void StartTask(Activity activity, double latitude, double longitude) {
		final Intent intent = new Intent(activity, SearchStationTask.class);
		intent.putExtra(KEY_LATITUDE, latitude);
		intent.putExtra(KEY_LONGITUDE, longitude);
		StartGenericTask(activity, intent, TASK_SEARCH);
	}
	
	@Override
	public int getlayoutId() { return R.layout.dialog_progress;	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		message.setText(R.string.searchStationTask);
		if (savedInstanceState == null) {
			final Bundle bundle = getIntent().getExtras();
			if (bundle.containsKey(KEY_QUERY)) {
				query = bundle.getString(KEY_QUERY);
			} else {
				latitude = bundle.getDouble(KEY_LATITUDE);
				longitude = bundle.getDouble(KEY_LONGITUDE);
			}
		} else {
			query = savedInstanceState.getString(KEY_QUERY);
			latitude = savedInstanceState.getDouble(KEY_LATITUDE);
			longitude = savedInstanceState.getDouble(KEY_LONGITUDE);
		}
		searchProvider = DataProviderFactory.getSearchProvider(handler);
	}
	
	private void search() {
		if (query != null) {
			searchProvider.Search(query);
		} else {
			searchProvider.Search(latitude, longitude);
		}
		
	}
	
	/*
	 * Save state, realtimeProvider uses static's for getting results, so it's not background safe.
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		searchProvider.Stop();
		super.onPause();
	}

	/*
	 * Resume state, restart search.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		search();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_QUERY, query);
		outState.putDouble(KEY_LATITUDE, latitude);
		outState.putDouble(KEY_LONGITUDE, longitude);
	}
}
