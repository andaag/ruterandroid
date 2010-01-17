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

package com.neuron.trafikanten.views.route;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.views.GenericSelectStationView;

public class SelectRouteStationView extends GenericSelectStationView {
	public static final String STATIONLIST_PARCELABLE = "stationList";
	private Button multiSelectButton;
	
	private ArrayList<StationData> selectedStations = new ArrayList<StationData>();
	
	final static private int MULTISELECT_DISABLED = 0;
	final static private int MULTISELECT_ENABLED = 1;
	
	private int multiSelect = MULTISELECT_DISABLED;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		selectStationType = STATIONTYPE_ROUTE;

		if (savedInstanceState != null) {
			/*
			 * Not saving instance state for this view, just confusing to switch into a station selector for the route view.
			 */
			favoriteDbAdapter.close();
			historyDbAdapter.close();
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		
		multiSelectButton = (Button) findViewById(R.id.multiSelectButton);
		multiSelectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch(multiSelect) {
				case MULTISELECT_DISABLED:
					/*
					 * Multiselect was disabled, lets enable it and refresh.
					 */
					multiSelect = MULTISELECT_ENABLED;
					refreshMultiSelect();
					multiSelectButton.setText(android.R.string.ok);
					break;
				case MULTISELECT_ENABLED:
					/*
					 * Multiselect was enabled, this means button click = return with stations
					 */
					
					/*
					 * Update history for all the stations we ended up using
					 */
					for(StationData station : selectedStations) {
						updateHistory(station);
					}
					favoriteDbAdapter.close();
					historyDbAdapter.close();

					/*
					 * Return list of stations
					 */
					Bundle bundle = new Bundle();
					bundle.putParcelableArrayList(STATIONLIST_PARCELABLE, selectedStations);
					
					final Intent intent = new Intent();
			        intent.putExtras(bundle);
			        setResult(RESULT_OK, intent);
			        finish();
				}
			
			}
		});
		multiSelectButton.setVisibility(View.VISIBLE);
	}
	
	private void refreshMultiSelect() {
		int layout = multiSelect == MULTISELECT_ENABLED ? R.layout.selectstation_list_multiselect : R.layout.selectstation_list;
		selectedStations.clear();
		setAdapterLayout(layout);
	}
	
	/*
	 * Handler for station selected
	 */
	@Override
	public void stationSelected(StationData station) {
		switch(multiSelect) {
		case MULTISELECT_DISABLED:
			/*
			 * Multiselect is disabled, return as normal
			 */
			updateHistory(station);
			favoriteDbAdapter.close();
			historyDbAdapter.close();

			Bundle bundle = new Bundle();
			bundle.putParcelable(StationData.PARCELABLE, station);
			
			final Intent intent = new Intent();
	        intent.putExtras(bundle);
	        setResult(RESULT_OK, intent);
	        finish();
	        break;
		case MULTISELECT_ENABLED:
			/*
			 * With multiselect enabled stationSelected means toggle.
			 */
			
			if (selectedStations.contains(station)) {
				selectedStations.remove(station);
			} else {
				selectedStations.add(station);
			}
		}
		
	}

	@Override
	public void setProgressBar(boolean value) {
		setProgressBarIndeterminateVisibility(value);
	}
}
