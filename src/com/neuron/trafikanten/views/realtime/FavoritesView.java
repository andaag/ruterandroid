package com.neuron.trafikanten.views.realtime;

import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenDevi;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRealtime;
import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.realtime.GenericRealtimeList;
import com.neuron.trafikanten.db.FavoriteLineDbAdapter.FavoriteData;
import com.neuron.trafikanten.db.FavoriteLineDbAdapter.FavoriteStation;

public class FavoritesView extends GenericRealtimeView {
	private static final String TAG = "Trafikanten-FavoritesView";
	
	/*
	 * Options menu:
	 */
	private static final int REFRESH_ID = Menu.FIRST;
	
	/*
	 * This is a queue of favorite stations, when size = 0 we're done loading
	 */
	private ArrayList<FavoriteStation> favStations;
	/*
	 * This is a list of actual departures generated by load().onData.
	 * Same logic as favStations when loading devi.
	 * Stations are pushed into this as favStations is cleared.
	 */
	private ArrayList<FavoriteStation> favStationsDevi;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState,"/favorites", GenericRealtimeList.RENDERER_STATION);
        
        /*
         * Setup view and adapter.
         */
        setContentView(R.layout.favorites);
        favStations = favoriteLineDbAdapter.getFavoriteData();
        favStationsDevi = new ArrayList<FavoriteStation>();
        clearView();
        load();
    }

	@Override
	protected void load() {
		if (favStations.size() == 0) {
			loadDevi();
			return;
		}
		final FavoriteStation favStation = favStations.get(0);
		favStations.remove(0);
		
		//final long STARTTIME = System.currentTimeMillis();
		
		tracker.trackEvent("Data", "Favorites", "Data", 0);
		final TrafikantenRealtime realtimeProvider = createRealtimeProvider(this, favStation.station.stationId);
		realtimeProvider.start(new IGenericProviderHandler<RealtimeData>() {
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
				final String line = realtimeData.line;
				final String destination = realtimeData.destination;
				for (FavoriteData favoriteData : favStation.items) {
					if (favoriteData.destination.equals(destination) && favoriteData.line.equals(line)) {
						realtimeList.addData(realtimeData, favStation.station);
						break;
					}
				}
			}

			@Override
			public void onPostExecute(Exception exception) {
				setProgressBarIndeterminateVisibility(false);
				clearRealtimeProvider(realtimeProvider);
				//Log.d(TAG,"PERF : Download + parse of realtime data for " + favStation.station.stopName + " took " + (System.currentTimeMillis() - STARTTIME) + "ms");
				if (exception != null) {
					Log.w(TAG,"onException " + exception);
			        if (exception.getClass().getSimpleName().equals("ParseException")) {
			        	Toast.makeText(FavoritesView.this, R.string.trafikantenErrorParse, Toast.LENGTH_LONG).show();
			        } else {
			        	Toast.makeText(FavoritesView.this, R.string.trafikantenErrorOther, Toast.LENGTH_LONG).show();
			        }
				} else {
					//refreshTitle();
					/*
					 * Show info text if view is empty
					 */
					//infoText.setVisibility(realtimeList.getCount() > 0 ? View.GONE : View.VISIBLE); // note, must do this if we're done loading!
					realtimeList.notifyDataSetChanged();
					favStationsDevi.add(favStation);
					load(); // keep loading until we are done.
					loadDevi(); // keep loading until we are done.
				}
			}

			@Override
			public void onPreExecute() {
				setProgressBarIndeterminateVisibility(true);				
			}
		});
	}
	
	
	private void loadDevi() {
		if (deviProvider != null) return;
		if (favStationsDevi.size() == 0) {
			return;
		}
		final FavoriteStation favStation = favStationsDevi.get(0);
		favStationsDevi.remove(0);
    	/*
    	 * Create list of lines - then merge it into a comma seperated list
    	 */
    	StringBuffer deviLines = new StringBuffer();
    	{
    		final int count = favStation.items.size();
	    	for (int i = 0; i < count; i++) {
	    		if (i > 0) {
	    			deviLines.append(",");
	    		}
	    		deviLines.append(favStation.items.get(i).line);
	    	}
    	}
    	
    	tracker.trackEvent("Data", "Favorites", "Devi", 0);
		/*
		 * Send dispatch along with devi data request.
		 */
		try {
			tracker.dispatch();
		} catch (Exception e) {}
		
    	deviProvider = new TrafikantenDevi(this, favStation.station.stationId, deviLines.toString(), new IGenericProviderHandler<DeviData>() {
    		@Override
    		public void onExtra(int what, Object obj) {
    			/* Class has no extra data */
    		}

			@Override
			public void onData(DeviData deviData) {
				realtimeList.addData(deviData);
			}

			@Override
			public void onPostExecute(Exception exception) {
				setProgressBarIndeterminateVisibility(false);
				deviProvider = null;

				if (exception != null) {
					Log.w(TAG,"onException " + exception);
			        if (exception.getClass().getSimpleName().equals("ParseException")) {
			        	Toast.makeText(FavoritesView.this, R.string.trafikantenErrorParse, Toast.LENGTH_LONG).show();
			        } else {
			        	Toast.makeText(FavoritesView.this, R.string.trafikantenErrorOther, Toast.LENGTH_LONG).show();
			        }

		
				} else {
					realtimeList.notifyDataSetChanged();
				}
				loadDevi();
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
        	stopProviders();
        	tracker.trackEvent("Navigation", "Favorites", "Refresh", 0);
            favStations = favoriteLineDbAdapter.getFavoriteData();
            favStationsDevi = new ArrayList<FavoriteStation>();
        	load();
        	return true;
        }
		return super.onOptionsItemSelected(item);
	}

}
