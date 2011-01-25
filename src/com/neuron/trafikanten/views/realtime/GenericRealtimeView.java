package com.neuron.trafikanten.views.realtime;

import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenDevi;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRealtime;
import com.neuron.trafikanten.db.FavoriteLineDbAdapter;

/*
 * Generic view for rendering realtime data. This is used by RealtimeView and FavoritesView
 */
public abstract class GenericRealtimeView extends ListActivity {
	private static final String KEY_TIMEDIFFERENCE = "timeDifference";
	private static final String KEY_LAST_UPDATE = "lastUpdate";
	private static final String KEY_LIST = "list";
	/*
	 * Data providers
	 */
	protected TrafikantenRealtime realtimeProvider = null;
	protected TrafikantenDevi deviProvider = null;
	protected FavoriteLineDbAdapter favoriteLineDbAdapter = null;
	
	/*
	 * Saved instance data
	 */
	protected GenericRealtimeListAdapter realtimeList;
	public long timeDifference = 0; // This is the time desync between system clock and trafikanten servers.
	protected long lastUpdate;
	
	/*
	 * Other
	 */
    public GoogleAnalyticsTracker tracker;
	
	/** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState, String viewName, int groupBy) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        /*
         * Analytics
         */
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start("UA-16690738-3", this);
		tracker.trackPageView(viewName);
		favoriteLineDbAdapter = new FavoriteLineDbAdapter(this);
		realtimeList = new GenericRealtimeListAdapter(this, this, tracker, groupBy);
		
		if (savedInstanceState != null) {
			timeDifference = savedInstanceState.getLong(KEY_TIMEDIFFERENCE);
        	lastUpdate = savedInstanceState.getLong(KEY_LAST_UPDATE);
        	realtimeList.setItems(savedInstanceState.getParcelable(KEY_LIST));
        	realtimeList.notifyDataSetChanged();
		}
		
        setListAdapter(realtimeList);
    }
    
    public void stopProviders() {
    	if (realtimeProvider != null) {
    		realtimeProvider.kill();
    	}
    	if (deviProvider != null) {
    		deviProvider.kill();
    	}
    }
    
    protected void refresh() {
    	realtimeList.notifyDataSetChanged(); // force rendering times again
    }
    protected void load() {
        lastUpdate = System.currentTimeMillis();
    	stopProviders();
    	clearView();
    }
    protected void clearView() {
    	realtimeList.clear();
    }
    
    
    /*
     * Keep refreshing the view every 10 seconds while view is visible.
     */
    final Handler autoRefreshHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			refresh();
			autoRefreshHandler.sendEmptyMessageDelayed(0, 10000);
			return true;
		}
	});   
    
	/**
	 * Saving load instance state/onpause/ondestroy etc
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(KEY_TIMEDIFFERENCE, timeDifference);
		outState.putParcelable(KEY_LIST, realtimeList.getParcelable());
		outState.putLong(KEY_LAST_UPDATE, lastUpdate);
	}
	
	/*
	 * Functions for dealing with program state.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		favoriteLineDbAdapter.close();
		autoRefreshHandler.removeMessages(0);
	}

	@Override
	protected void onResume() {
		super.onResume();
		favoriteLineDbAdapter.open();
		refresh();
		autoRefreshHandler.sendEmptyMessageDelayed(0, 10000);
	}
	
	@Override
	protected void onStop() {
		/*
		 * make sure background threads is properly killed off.
		 */
		stopProviders();
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Stop the tracker when it is no longer needed.
		tracker.stop();
	}
}
