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

package com.neuron.trafikanten.views.realtime;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenDevi;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRealtime;
import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.dataSets.realtime.GenericRealtimeList;
import com.neuron.trafikanten.tasks.NotificationTask;
import com.neuron.trafikanten.tasks.SelectDeviTask;
import com.neuron.trafikanten.tasks.ShowTipsTask;
import com.neuron.trafikanten.views.GenericDeviCreator;

public class RealtimeView extends GenericRealtimeView {
	private static final String TAG = "Trafikanten-RealtimeView";
	public static final String SETTING_HIDECA = "realtime_hideCaText";
	private static final String KEY_FINISHEDLOADING = "finishedLoading";

	/*
	 * Options menu:
	 */
	private static final int REFRESH_ID = Menu.FIRST;
	private static final int HIDECA_ID = Menu.FIRST + 1;
	
	/*
	 * Context menu:
	 */
	private static final int NOTIFY_ID = Menu.FIRST;
	private static final int DEVI_ID = Menu.FIRST + 1;
	private static final int FAVORITE_ID = Menu.FIRST + 2;
	
	/*
	 * Dialogs
	 */
	private int selectedId = 0;

	
	/*
	 * Saved instance data
	 */
	// HACK STATIONICONS, this does not need to be public static
	public static StationData station;
	private boolean finishedLoading = false; // we're finishedLoading when devi has loaded successfully.
	
	/*
	 * UI
	 */
	private LinearLayout devi;
	private TextView infoText;
	private TextView caText;
	

	
	/*
	 * Other
	 */
    public SharedPreferences settings;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState,"/realtime", GenericRealtimeList.RENDERER_PLATFORM);
        
        /*
         * Setup view and adapter.
         */
        setContentView(R.layout.realtime);
        devi = (LinearLayout) findViewById(R.id.devi);
		infoText = (TextView) findViewById(R.id.emptyText);
		caText = (TextView) findViewById(R.id.caInfoText);
		settings = getSharedPreferences("trafikanten", MODE_PRIVATE);
        		
        /*
         * Load instance state
         */
        if (savedInstanceState == null) {
        	Bundle bundle = getIntent().getExtras();
        	/*
        	 * Most of the time we get a normal StationData.PARCELABLE, but shortcuts sends a simple bundle.
        	 */
        	if (bundle.containsKey(StationData.PARCELABLE)) {
        		station = getIntent().getParcelableExtra(StationData.PARCELABLE);
        	} else {
        		station = StationData.readSimpleBundle(bundle);
        	}
        	load();
        } else {
        	station = savedInstanceState.getParcelable(StationData.PARCELABLE);
        	finishedLoading = savedInstanceState.getBoolean(KEY_FINISHEDLOADING);
        	infoText.setVisibility(realtimeList.getCount() > 0 ? View.GONE : View.VISIBLE);
        	
        	if (!finishedLoading) {
    			load();
        	}
        }

        registerForContextMenu(getListView());
        refreshTitle();
        refreshDevi();
        
        new ShowTipsTask(this, tracker, RealtimeView.class.getName(), R.string.tipRealtime, 38);
    }
    
    /*
     * Refreshes the title
     */
    private void refreshTitle() {
    	long lastUpdateDiff = (System.currentTimeMillis() - lastUpdate) / HelperFunctions.SECOND;
    	if (lastUpdateDiff > 60) {
    		lastUpdateDiff = lastUpdateDiff / 60;
    		setTitle("Trafikanten - " + station.stopName + "   (" + lastUpdateDiff + " min " + getText(R.string.old) + ")");
    	} else {
    		setTitle("Trafikanten - " + station.stopName);
    	}
    }
    
    /*
     * Refreshes station specific devi data.
     */
    private void refreshDevi() {
    	/*
    	 * Calculate a time difference it's easier to work with
    	 */
    	long timeDiff = timeDifference / 1000;
    	if (timeDiff < 0)
    		timeDiff = timeDiff * -1;
    	
    	/*
    	 * Render devi    	
    	 */
    	if ((station.devi == null || station.devi.size() == 0) && timeDiff < 60) {
    		/*
    		 * Nothing to display
    		 */
    		devi.setVisibility(View.GONE);
    	} else {
    		/*
    		 * Render devi information
    		 */
    		devi.setVisibility(View.VISIBLE);
    		devi.removeAllViews();
    		
    		if (timeDiff >= 90) {
    			DeviData deviData = new DeviData();
    			deviData.title = (String) getText(R.string.clockDiffTitle);
    			deviData.description = (String) getText(R.string.clockDiffDescription); 
    			deviData.body = (String) getText(R.string.clockDiffBodyHead);
    			if (timeDifference < 0) {
    				deviData.body = deviData.body + "\n\n" + getText(R.string.clockDiffBodyYourClock) + " " +  timeDiff + "s " + getText(R.string.clockDiffBodyBehind);   				
    			} else {
    				deviData.body = deviData.body + "\n\n" + getText(R.string.clockDiffBodyYourClock) + " " +  timeDiff + "s " + getText(R.string.clockDiffBodyAhead);
    			}
    			
    			deviData.validFrom = 0;
    			deviData.validTo = 0;
    			
    			devi.addView(GenericDeviCreator.createDefaultDeviText(this, tracker, deviData.title, deviData, true), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
    		}
    		
    		for (final DeviData deviData : station.devi) {
				devi.addView(GenericDeviCreator.createDefaultDeviText(this, tracker, deviData.title, deviData, true), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
    		}
  		
    	}
    }
    
    @Override
    protected void clearView() {
    	super.clearView();
    	finishedLoading = false;
    	station.devi = new ArrayList<DeviData>();
    	devi.setVisibility(View.GONE);
    }
    
    /*
     * Load data, variable used to prevent updating data set on every iteration.
     */
    private boolean caVisibilityChecked;
    @Override
    protected void load() {
    	super.load();

		caVisibilityChecked = settings.getBoolean(RealtimeView.SETTING_HIDECA, false); // if hideca = true we skip the visibility check
		
		tracker.trackEvent("Data", "Realtime", "Data", 0);
		realtimeProvider = new TrafikantenRealtime(this, station.stationId, new IGenericProviderHandler<RealtimeData>() {
			@Override
			public void onExtra(int what, Object obj) {
				switch (what) {
				case TrafikantenRealtime.MSG_TIMEDATA:
					timeDifference = (Long) obj;
					break;
				}
			}
			
			@Override
			public void onData(RealtimeData realtimeData) {
				if (!caVisibilityChecked && !realtimeData.realtime) {
					/*
					 * check "ca info text" visibility
					 */
					caVisibilityChecked = true;
		        	caText.setVisibility(View.VISIBLE);
				}
				realtimeList.addData(realtimeData);
			}

			@Override
			public void onPostExecute(Exception exception) {
				setProgressBarIndeterminateVisibility(false);
				realtimeProvider = null;
				if (exception != null) {
					Log.w(TAG,"onException " + exception);
					clearView();
					infoText.setVisibility(View.VISIBLE);
			        if (exception.getClass().getSimpleName().equals("ParseException")) {
			        	infoText.setText(R.string.trafikantenErrorParse);
			        } else {
			        	infoText.setText(R.string.trafikantenErrorOther);
			        }
				} else {
					refreshTitle();
					/*
					 * Show info text if view is empty
					 */
					infoText.setVisibility(realtimeList.getCount() > 0 ? View.GONE : View.VISIBLE);
					realtimeList.notifyDataSetChanged();
					loadDevi();	
				}
			}

			@Override
			public void onPreExecute() {
				setProgressBarIndeterminateVisibility(true);				
			}
		});
    }
    
    /*
     * Load devi data
     */
    private void loadDevi() {
    	/*
    	 * Create list of lines - first create lineList
    	 */
    	ArrayList<String> lineList = new ArrayList<String>();
    	{
    		
	    	final int count = realtimeList.getCount();
	    	for (int i = 0; i < count; i++) {
	    		final RealtimeData realtimeData = realtimeList.getRealtimeItem(i);
	    		if (realtimeData != null) {
		    		if (!lineList.contains(realtimeData.line)) {
		    			lineList.add(realtimeData.line);
		    		}
	    		}
	    	}
    	}
    	/*
    	 * Create list of lines - then merge it into a comma seperated list
    	 */
    	StringBuffer deviLines = new StringBuffer();
    	{
    		final int count = lineList.size();
	    	for (int i = 0; i < count; i++) {
	    		if (i > 0) {
	    			deviLines.append(",");
	    		}
	    		deviLines.append(lineList.get(i));
	    	}
    	}
    	
    	tracker.trackEvent("Data", "Realtime", "Devi", 0);
		/*
		 * Send dispatch along with devi data request.
		 */
		try {
			tracker.dispatch();
		} catch (Exception e) {}
		
    	deviProvider = new TrafikantenDevi(this, station.stationId, deviLines.toString(), new IGenericProviderHandler<DeviData>() {
    		@Override
    		public void onExtra(int what, Object obj) {
    			/* Class has no extra data */
    		}

			@Override
			public void onData(DeviData deviData) {
				for (final String deviName : deviData.stops) {
					if (deviName.equals(station.stopName)) {
						/*
						 * Station specific data
						 */
						station.devi.add(deviData);
						break;
					}
				}

				/*
				 * Line data (will be ignored if line isn't shown in view, so no point in checking data.lines)
				 */
				realtimeList.addData(deviData);
			}

			@Override
			public void onPostExecute(Exception exception) {
				setProgressBarIndeterminateVisibility(false);
				finishedLoading = true;
				deviProvider = null;

				if (exception != null) {
					Log.w(TAG,"onException " + exception);
			        if (exception.getClass().getSimpleName().equals("ParseException")) {
			        	Toast.makeText(RealtimeView.this, R.string.trafikantenErrorParse, Toast.LENGTH_LONG).show();
			        } else {
			        	Toast.makeText(RealtimeView.this, R.string.trafikantenErrorOther, Toast.LENGTH_LONG).show();
			        }

		
				} else {
					refreshDevi();
					realtimeList.notifyDataSetChanged();
				}
			}

			@Override
			public void onPreExecute() {
				setProgressBarIndeterminateVisibility(true);
			}
    	});
    }
    
    /**
     * menu code
     */
    
	/*
	 * Options menu, visible on menu button.
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		final MenuItem refresh = menu.add(0, REFRESH_ID, 0, R.string.refresh);
		refresh.setIcon(R.drawable.ic_menu_refresh);
        
        final MenuItem showHideCaText = menu.add(0, HIDECA_ID, 0, R.string.changeCaTextVisibility);
        showHideCaText.setIcon(android.R.drawable.ic_menu_info_details);

		return true;
	}
    
	/*
	 * Options menu item selected, options menu visible on menu button.
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case REFRESH_ID:
        	tracker.trackEvent("Navigation", "Realtime", "Refresh", 0);
        	load();
        	return true;
        case HIDECA_ID:
        	boolean hideCaText = !(caText.getVisibility() == View.GONE);
        	SharedPreferences.Editor editor = settings.edit();
            if (hideCaText) {
            	caText.setVisibility(View.GONE);
            	editor.putBoolean(SETTING_HIDECA, true);
            } else {
            	caText.setVisibility(View.VISIBLE);
            	editor.putBoolean(SETTING_HIDECA, false);
            }
            editor.commit();
        	return true;
        }
		return super.onOptionsItemSelected(item);
	}

	/*
	 * onCreate - Context menu is a popup from a longpress on a list item.
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		final RealtimeData realtimeData = realtimeList.getRealtimeItem(info.position);
		if (realtimeData == null) return;

		if (realtimeData.devi.size() > 0)
			menu.add(0, DEVI_ID, 0, R.string.warnings);
		menu.add(0, NOTIFY_ID, 0, R.string.alarm);		
		if (favoriteLineDbAdapter.isFavorite(station, realtimeData.line, realtimeData.destination)) {
			menu.add(0, FAVORITE_ID, 0, R.string.removeFavorite);
		} else {
			menu.add(0, FAVORITE_ID, 0, R.string.addFavorite);
		}
	}
	
	/*
	 * onSelected - Context menu is a popup from a longpress on a list item.
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        selectedId = info.position;
		final RealtimeData realtimeData = (RealtimeData) realtimeList.getRealtimeItem(selectedId);
		
		switch(item.getItemId()) {
		case NOTIFY_ID:
			final String notifyWith = realtimeData.line.equals(realtimeData.destination) 
				? realtimeData.line 
				: realtimeData.line + " " + realtimeData.destination;
			new NotificationTask(this, tracker, realtimeData, station, notifyWith, timeDifference);
			return true;
		case DEVI_ID:
			final ArrayList<DeviData> deviPopup = realtimeData.devi;
			new SelectDeviTask(this, tracker, deviPopup);
			return true;
		case FAVORITE_ID:
			favoriteLineDbAdapter.toggleFavorite(station, realtimeData.line, realtimeData.destination);
			return true;
		}
		return super.onContextItemSelected(item);
	}
	
	@Override
	protected void refresh() {
		super.refresh();
		refreshTitle();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(StationData.PARCELABLE, station);
		outState.putBoolean(KEY_FINISHEDLOADING, finishedLoading);
	}
	

}

