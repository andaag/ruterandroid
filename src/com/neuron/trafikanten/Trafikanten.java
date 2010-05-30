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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.db.FavoriteDbAdapter;
import com.neuron.trafikanten.db.HistoryDbAdapter;
import com.neuron.trafikanten.views.realtime.RealtimeView;
import com.neuron.trafikanten.views.realtime.SelectRealtimeStationView;
import com.neuron.trafikanten.views.route.SelectRouteView;


public class Trafikanten extends TabActivity {
	private static Activity activity;
	public final static String KEY_MYLOCATION = "myLocation";
	private GoogleAnalyticsTracker tracker;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start("UA-16690738-3", this);
		Trafikanten.activity = this;
	 	setTitle("Trafikanten - " + getText(R.string.app_version));
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        if (isShortcut()) {
        	setVisible(false);
        }
        
        
        /*
         * Google analytics
         */
        tracker.trackPageView("/home");
		final SharedPreferences preferences = activity.getSharedPreferences("trafikantenandroid", Context.MODE_PRIVATE);
		// Tags device version, model, and amount of data downloaded.
        tracker.trackEvent("Device", Build.VERSION.RELEASE, Build.MODEL, (int)preferences.getLong(HelperFunctions.KEY_DOWNLOADBYTE, 0) / 1024);

        
        /*
         * Setup tab host
         */
	 	final TabHost tabHost = getTabHost();
	 	
	 	{
	 		/*
	 		 * Hack : Tweaks for devices with software keyboards, hide keyboard when switching tabs.
	 		 */
            tabHost.setOnTabChangedListener(new OnTabChangeListener()
	        {
	        public void onTabChanged(String tabId)
	            {
	            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
	            imm.hideSoftInputFromWindow(tabHost.getApplicationWindowToken(), 0);
	            }
	        });
	 	}
	 	
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
        tracker.trackPageView("/createShortcut");
        
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
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP); 
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
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
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
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Stop the tracker when it is no longer needed.
		tracker.stop();
	}
}
