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

package com.neuron.trafikanten;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Window;
import android.widget.TabHost;

import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.db.FavoriteDbAdapter;
import com.neuron.trafikanten.db.HistoryDbAdapter;
import com.neuron.trafikanten.views.realtime.RealtimeView;
import com.neuron.trafikanten.views.realtime.SelectRealtimeStationView;
import com.neuron.trafikanten.views.route.SelectRouteView;


public class Trafikanten extends TabActivity {
	private static Activity activity;
	public final static String KEY_MYLOCATION = "myLocation";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Trafikanten.activity = this;
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        
        if (isShortcut()) {
        	return;
        }
        
        /*
         * Setup tab host
         */
	 	final TabHost tabHost = getTabHost();
	 	setTitle("Trafikanten - " + getText(R.string.app_version));
	 	
	 	{
	 		/*
	 		 * Add realtime tab
	 		 */
	 		final Intent intent = new Intent(this, SelectRealtimeStationView.class);
	 		if (getIntent().hasExtra(KEY_MYLOCATION)) {
	 			intent.putExtra(KEY_MYLOCATION, true);
	 		}
		    tabHost.addTab(tabHost.newTabSpec("RealtimeTab")
		 			.setIndicator(getText(R.string.realtime), getResources().getDrawable(R.drawable.ic_menu_recent_history))
		 			.setContent(intent));
	 	}
	     
	 	{
	 		/*
	 		 * Add route tab
	 		 */
		    tabHost.addTab(tabHost.newTabSpec("RouteTab")
		 			.setIndicator(getText(R.string.route), getResources().getDrawable(R.drawable.ic_menu_directions))
		 			.setContent(new Intent(this, SelectRouteView.class)));
	 	}
	}
	
	private boolean isShortcut() {
		final Intent intent = getIntent();
        final String action = intent.getAction();
        if (!Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
        	return false;        	
        }
        
        /*
         * Todo show list of shortcuts to create.
         */
        ArrayList<CharSequence> selectList = new ArrayList<CharSequence>();
        selectList.add(getText(R.string.myLocation));
        
        final ArrayList<StationData> realtimeStations = new ArrayList<StationData>(); 
        
        /*
         * Add all history/favorite stations
         */
        {
        	final FavoriteDbAdapter favoriteDbAdapter = new FavoriteDbAdapter(this);
        	final HistoryDbAdapter historyDbAdapter = new HistoryDbAdapter(this);
        	
        	favoriteDbAdapter.addFavoritesToList(false, realtimeStations);
        	historyDbAdapter.addHistoryToList(false, realtimeStations);
        }
        for(StationData station : realtimeStations) {
        	selectList.add(station.stopName);
        }
        
        /*
         * Setup select contact alert dialog
         */
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.selectContact);
        final String[] items = new String[selectList.size()];
        selectList.toArray(items);
        
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
            	switch(item) {
            	case 0:
            		// My location
            		createShortcutMyLocation();
            		break;
            	default:
            		// A station
            		final StationData station = realtimeStations.get(item - 1);
            		createShortcutStation(station);
            		break;
            		
            	}
                dialog.dismiss();
                finish();
            }
        });
        
        Dialog dialog = builder.create();
        dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				finish();				
			}
        	
        });
        dialog.show();
        
        return true;
	}
	
	private void createShortcutMyLocation() {
        final Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.setClassName(this, this.getClass().getName());
        shortcutIntent.putExtra(KEY_MYLOCATION, true);
        /*
         * Setup container
         */
        final Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getText(R.string.myLocation));
        final Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this,  R.drawable.icon);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
        setResult(RESULT_OK, intent);
	}
	
	private void createShortcutStation(StationData station) {
        final Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.setClassName(this, RealtimeView.class.getName());
        
        final Bundle bundle = new Bundle();
        station.writeSimpleBundle(bundle);
        shortcutIntent.putExtras(bundle);
        
        /*
         * Setup container
         */
        final Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, station.stopName);
        final Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this,  R.drawable.icon);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
        setResult(RESULT_OK, intent);
	}
	
	/*
	 * Function is always used from one of the tab views, therefor this is perfectly fine.
	 */
	public static void tabHostSetProgressBarIndeterminateVisibility(boolean value)
	{
		activity.setProgressBarIndeterminateVisibility(value);
	}
	
	/*
	 * onPause - stop anything needed.
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
	}

	/*
	 * Resume state, refresh settings
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
	}
}
