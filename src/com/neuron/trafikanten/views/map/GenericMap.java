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

package com.neuron.trafikanten.views.map;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.TransparentPanel;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.SearchStationData;
import com.neuron.trafikanten.db.FavoriteDbAdapter;

public class GenericMap extends MapActivity {
	private static final int DIALOG_LIST = Menu.FIRST;
	private MyLocationOverlay locationOverlay;
	private static GenericStationOverlay stationOverlay;
	private static ViewHolder viewHolder = new ViewHolder();
	
	/*
	 * Holder for currently selected station in panel
	 */
	static private SearchStationData selectedStation;
	
	/*
	 * Options menu items
	 */
	private final static int MYLOCATION_ID = Menu.FIRST;
	
	/*
	 * Variables cached locally for performance
	 */
	private MapView mapView;
		
	public static void Show(Activity activity, int what) {
		Intent intent = new Intent(activity, GenericMap.class);
		activity.startActivityForResult(intent, what);
	}
	
	public static void Show(Activity activity, ArrayList<SearchStationData> stationList, int what) {
		Intent intent = new Intent(activity, GenericMap.class);
		intent.putExtra(SearchStationData.PARCELABLE, stationList);
		activity.startActivityForResult(intent, what);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		
		/*
		 * Setup map view
		 */
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		viewHolder.list = (ImageButton) findViewById(R.id.list);
		viewHolder.infoPanel = (TransparentPanel) findViewById(R.id.infoPanel);
		viewHolder.infoPanel.setVisibility(View.GONE);
		viewHolder.name = (TextView) findViewById(R.id.name);
		viewHolder.information = (TextView) findViewById(R.id.information);
		viewHolder.select = (ImageButton) findViewById(R.id.select);
		
		/*
		 * Setup onClick handler for list button
		 */
		viewHolder.list.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(DIALOG_LIST);
			}
		});
		
		/*
		 * Setup onClick handler for select station button
		 */
		viewHolder.select.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtra(SearchStationData.PARCELABLE, selectedStation);
				setResult(RESULT_OK, intent);
				finish();
			}
			
		});
		
		/*
		 * Setup overlays
		 */
		//final Drawable drawable = getResources().getDrawable(R.drawable.icon_unknown);
		final Drawable drawable = getResources().getDrawable(R.drawable.icon_mapmarker);
		stationOverlay = new GenericStationOverlay(drawable);
		
		/*
		 * Load stations
		 */
		if (savedInstanceState == null) {
			final Bundle bundle = getIntent().getExtras();
			if (bundle.containsKey(SearchStationData.PARCELABLE)) {
				/*
				 * Load stations passed to us
				 */
				final ArrayList<SearchStationData> stationList = bundle.getParcelableArrayList(SearchStationData.PARCELABLE);
				final FavoriteDbAdapter favoriteDbAdapter = new FavoriteDbAdapter(this);
				favoriteDbAdapter.refreshFavorites(stationList);
				favoriteDbAdapter.close();
				stationOverlay.add(this, stationList);
			}
		} else {
			// TODO : saveInstanceState in mapview.
			finish();
		}

		/*
         * Add MyLocationOverlay so we can see where we are.
         */
		locationOverlay = new MyLocationOverlay(this, mapView);
		boolean animateToGpsPosition = true;
        if (animateToGpsPosition) {
        	locationOverlay.runOnFirstFix(new Runnable() {
	            public void run() {
	            	mapView.getController().animateTo(locationOverlay.getMyLocation());
	            }
	        });
        }
        
        /*
         * Add all overlays to the overlay list
         */
        List<Overlay> overlays = mapView.getOverlays();
        overlays.add(stationOverlay);
        overlays.add(locationOverlay);
        //mapView.invalidate();
		
	}
	
	static public void onStationTap(SearchStationData station) {
		viewHolder.infoPanel.setVisibility(View.VISIBLE);
		viewHolder.name.setText(station.stopName);
		viewHolder.information.setText(station.extra);
		selectedStation = station;
	}

	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch(id) {
		case DIALOG_LIST:
			/*
			 * Dialog contains a list, force recreating it.
			 */
			removeDialog(DIALOG_LIST);
			dialog = onCreateDialog(DIALOG_LIST);
			break;
		}
		super.onPrepareDialog(id, dialog);
	}
	
	/*
	 * Creating dialogs
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_LIST:
			/*
			 * TODO : If stationOverlay items are dynamic, and can be updated while if the view is open, this will need to be converted to prepareDialog
			 */
			AlertDialog.Builder builder = new AlertDialog.Builder(GenericMap.this);
			builder.setTitle(getText(R.string.selectStation));
			String[] items = new String[stationOverlay.size()];
			for (int i = 0; i < stationOverlay.size(); i++) {
				items[i] = stationOverlay.getItem(i).station.stopName;
			}
			builder.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final SearchStationOverlayItem stationItem = stationOverlay.getItem(which);
					mapView.getController().animateTo(stationItem.getPoint());
					onStationTap(stationItem.station);
				}
			});
			return builder.create(); 
		}
		return super.onCreateDialog(id);
	}

	
	
	/*
	 * Setup options menu (available on menu button)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuItem myLocation = menu.add(0, MYLOCATION_ID, 0, R.string.myLocation);
		myLocation.setIcon(android.R.drawable.ic_menu_mylocation);
		return true;
	}

	/*
	 * Options menu item selected (available on menu button)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case MYLOCATION_ID:
        	final GeoPoint point = locationOverlay.getMyLocation();
        	if (point == null) {
        		Toast.makeText(this, R.string.noLocationFound, Toast.LENGTH_SHORT).show();
        	} else {
        		mapView.getController().animateTo(locationOverlay.getMyLocation());
        	}
        	break;
        }
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected void onPause() {
		locationOverlay.disableMyLocation();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		locationOverlay.enableMyLocation();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}
	
	static class ViewHolder {
		ImageButton list;
		
		TransparentPanel infoPanel;
		TextView name;
		TextView information;
		ImageButton select;
	}
}
