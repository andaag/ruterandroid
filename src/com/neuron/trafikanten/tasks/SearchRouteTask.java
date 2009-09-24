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
import com.neuron.trafikanten.dataProviders.IRouteProvider;
import com.neuron.trafikanten.dataSets.RouteData;

public class SearchRouteTask extends GenericTask {
	public static final int TASK_ROUTE = 103;
	private IRouteProvider routeProvider;
	
	/*
	 * Saved data:
	 */
	private RouteData routeData;
	
	public static void StartTask(Activity activity, RouteData routeData) {
		final Intent intent = new Intent(activity, SearchRouteTask.class);
		intent.putExtra(RouteData.PARCELABLE, routeData);
		StartGenericTask(activity, intent, TASK_ROUTE);
	}
	
	@Override
	public int getlayoutId() { return R.layout.dialog_progress;	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		message.setText(R.string.searchRouteTask);
		
		if (savedInstanceState == null) {
			routeData = getIntent().getExtras().getParcelable(RouteData.PARCELABLE);
		} else {
			routeData = savedInstanceState.getParcelable(RouteData.PARCELABLE);
		}
		routeProvider = DataProviderFactory.getRouteProvider(handler);
	}
	
	/*
	 * Save state, routeProvider uses static's for getting results, so it's not background safe.
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		routeProvider.Stop();
		super.onPause();
	}

	/*
	 * Resume state, restart search.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		routeProvider.Search(routeData);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(RouteData.PARCELABLE, routeData);
	}
	
}
