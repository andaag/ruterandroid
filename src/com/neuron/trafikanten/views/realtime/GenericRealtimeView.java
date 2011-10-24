package com.neuron.trafikanten.views.realtime;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.google.android.AnalyticsUtils;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenDevi;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRealtime;
import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.db.FavoriteLineDbAdapter;
import com.neuron.trafikanten.tasks.NotificationTask;
import com.neuron.trafikanten.tasks.SelectDeviTask;

/*
 * Generic view for rendering realtime data. This is used by RealtimeView and FavoritesView
 */
public abstract class GenericRealtimeView extends ListActivity {
	private static final String KEY_TIMEDIFFERENCE = "timeDifference";
	private static final String KEY_LAST_UPDATE = "lastUpdate";
	private static final String KEY_LIST = "list";
	
	/*
	 * Options menu:
	 */
	private static final int REFRESH_ID = Menu.FIRST;
	
	/*
	 * Context menu:
	 */
	private static final int NOTIFY_ID = Menu.FIRST;
	private static final int DEVI_ID = Menu.FIRST + 1;
	private static final int FAVORITE_ID = Menu.FIRST + 2;
	
	/*
	 * Data providers
	 */
	protected ArrayList<TrafikantenRealtime> _realtimeProviders = null;
	protected TrafikantenDevi deviProvider = null;
	protected FavoriteLineDbAdapter favoriteLineDbAdapter = null;
	
	/*
	 * Saved instance data
	 */
	protected GenericRealtimeListAdapter realtimeList;
	public long timeDifference = 0; // This is the time desync between system clock and trafikanten servers.
	protected long lastUpdate;
	
	/** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState, String viewName, int groupBy) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        /*
         * Analytics
         */
        AnalyticsUtils.getInstance(this).trackPageView(viewName);
		favoriteLineDbAdapter = new FavoriteLineDbAdapter(this);
		realtimeList = new GenericRealtimeListAdapter(this, this, groupBy);
		
		if (savedInstanceState != null) {
			timeDifference = savedInstanceState.getLong(KEY_TIMEDIFFERENCE);
        	lastUpdate = savedInstanceState.getLong(KEY_LAST_UPDATE);
        	realtimeList.setItems(savedInstanceState.getParcelable(KEY_LIST));
        	realtimeList.notifyDataSetChanged();
		}
		
        setListAdapter(realtimeList);
    }
    
    public TrafikantenRealtime createRealtimeProvider(Context context, int stationId) {
    	TrafikantenRealtime provider = new TrafikantenRealtime(context, stationId);
    	if (_realtimeProviders == null) {
    		_realtimeProviders = new ArrayList<TrafikantenRealtime>();
    	}
    	_realtimeProviders.add(provider);
    	return provider;
    }
    
    public void clearRealtimeProvider(TrafikantenRealtime realtimeProvider) {
    	if (_realtimeProviders != null) {
    		_realtimeProviders.remove(realtimeProvider);
    	}
    }
    
    public void stopProviders() {
    	if (_realtimeProviders != null) {
	    	for (TrafikantenRealtime realtimeProvider : _realtimeProviders) {
	    		realtimeProvider.kill();
	    	}
    	}
    	if (deviProvider != null) {
    		deviProvider.kill();
    	}
    	setProgressBarIndeterminateVisibility(false);
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
	
	/**
	 * Options menu code
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
        	onOptionsMenuRefresh();
        	return true;
        }
		return super.onOptionsItemSelected(item);
	}
	public abstract void onOptionsMenuRefresh();
	
	
	/**
	 * Context menu code
	 */
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
		final StationData station = getStation(info.position);
		if (realtimeData == null || station == null) return;

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
		final RealtimeData realtimeData = (RealtimeData) realtimeList.getRealtimeItem(info.position);
		final StationData station = getStation(info.position);
		
		if (realtimeData != null && station != null) {
			switch(item.getItemId()) {
			case NOTIFY_ID:
				final String notifyWith = realtimeData.line.equals(realtimeData.destination) 
					? realtimeData.line 
					: realtimeData.line + " " + realtimeData.destination;
				new NotificationTask(this, realtimeData, station, notifyWith, timeDifference);
				return true;
			case DEVI_ID:
				final ArrayList<DeviData> deviPopup = realtimeData.devi;
				new SelectDeviTask(this, deviPopup);
				return true;
			case FAVORITE_ID:
				favoriteLineDbAdapter.toggleFavorite(station, realtimeData.line, realtimeData.destination);
				return true;
			}
		}
		return super.onContextItemSelected(item);
	}
	
	/*
	 * For realtime this returns current station. For favorites this scans up from the selected position to find the station belonging to that realtime data.
	 */
	public abstract StationData getStation(int pos);
}
