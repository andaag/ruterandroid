package com.neuron.trafikanten.views.realtime;

import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRealtime;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.realtime.GenericRealtimeList;
import com.neuron.trafikanten.db.FavoriteLineDbAdapter.FavoriteData;
import com.neuron.trafikanten.db.FavoriteLineDbAdapter.FavoriteStation;

public class FavoritesView extends GenericRealtimeView {
	private static final String TAG = "Trafikanten-FavoritesView";
	private ArrayList<FavoriteStation> favStations;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState,"/favorites", GenericRealtimeList.RENDERER_STATION);
        
        /*
         * Setup view and adapter.
         */
        setContentView(R.layout.favorites);
        favStations = favoriteLineDbAdapter.getFavoriteData();
        load();
    }

	@Override
	protected void load() {
		if (favStations.size() == 0) return;
		final FavoriteStation favStation = favStations.get(0);
		favStations.remove(0);
		
		tracker.trackEvent("Data", "Favorites", "Data", 0);
		realtimeProvider = new TrafikantenRealtime(this, favStation.station.stationId, new IGenericProviderHandler<RealtimeData>() {
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
				realtimeProvider = null;
				if (exception != null) {
					Log.w(TAG,"onException " + exception);
					clearView();
					/*infoText.setVisibility(View.VISIBLE);
			        if (exception.getClass().getSimpleName().equals("ParseException")) {
			        	infoText.setText(R.string.trafikantenErrorParse);
			        } else {
			        	infoText.setText(R.string.trafikantenErrorOther);
			        }*/
				} else {
					//refreshTitle();
					/*
					 * Show info text if view is empty
					 */
					//infoText.setVisibility(realtimeList.getCount() > 0 ? View.GONE : View.VISIBLE);
					realtimeList.notifyDataSetChanged();
					//loadDevi();	
					load(); // keep loading until we are done.
				}
			}

			@Override
			public void onPreExecute() {
				setProgressBarIndeterminateVisibility(true);				
			}
		});
	}

}
