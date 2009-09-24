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
import com.neuron.trafikanten.dataProviders.IRealtimeProvider;

public class RealtimeDataTask extends GenericTask {
	public static final int TASK_REALTIME = 102;
	private static final String KEY_STATIONID = "stationid";
	private IRealtimeProvider realtimeProvider;
	
	/*
	 * Saved instance data:
	 */
	private int stationId;
	
	public static void StartTask(Activity activity, int stationId) {
		final Intent intent = new Intent(activity, RealtimeDataTask.class);
		intent.putExtra(KEY_STATIONID, stationId);
		StartGenericTask(activity, intent, TASK_REALTIME);
	}
	
	@Override
	public int getlayoutId() { return R.layout.dialog_progress;	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		message.setText(R.string.realtimeDataTask);
		
		if (savedInstanceState == null) {
			stationId = getIntent().getExtras().getInt(KEY_STATIONID);
		} else {
			stationId = savedInstanceState.getInt(KEY_STATIONID);
		}
		realtimeProvider = DataProviderFactory.getRealtimeProvider(handler);
	}
	
	private void fetch() {
		realtimeProvider.Fetch(stationId);
	}

	/*
	 * Save state, realtimeProvider uses static's for getting results, so it's not background safe.
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		realtimeProvider.Stop();
		super.onPause();
	}

	/*
	 * Resume state, restart search.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		fetch();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_STATIONID, stationId);
	}
	
	
}
