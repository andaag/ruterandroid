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

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenRealtime;
import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.RouteDeviData;
import com.neuron.trafikanten.dataSets.RouteProposal;
import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.tasks.NotificationTask;
import com.neuron.trafikanten.views.GenericDeviCreator;
import com.neuron.trafikanten.views.map.GenericMap;

public class DetailedRouteView extends ListActivity {
	//private final static String TAG = "Trafikanten-DetailedRouteView";
	private RouteAdapter routeList;
	public GoogleAnalyticsTracker tracker;
	private RouteDeviLoader routeDeviLoader;
	
	/*
	 * Realtime loaders:
	 */
	private RouteRealtimeLoader routeRealtimeLoader = null;
	
	/*
	 * Context menu:
	 */
	private static final int NOTIFY_ID = Menu.FIRST;
	private static final int REALTIME_ID = Menu.FIRST + 1;
	
	/*
	 * Option menu
	 */
	private final static int MAP_ID = Menu.FIRST + 1;
	
	/*
	 * Dialogs
	 */
	private int selectedId = 0;
	
	/*
	 * Saved instance data
	 */
	public RouteDeviData deviList;
	private ArrayList<RouteProposal> routeProposalList;
	private int proposalPosition;

	/*
	 * this is a list of departure dates, it's used to store next/previous button clicks.
	 * See the button onClick listeners for how this works.
	 */
	public final static String KEY_PROPOSALPOSITION = "proposalPosition";

	public static void ShowRoute(Activity activity, ArrayList<RouteProposal> routeProposalList, RouteDeviData deviList, int proposalPosition) {
		final Intent intent = new Intent(activity, DetailedRouteView.class);
		final Bundle bundle = new Bundle();
		
		bundle.putParcelableArrayList(RouteProposal.PARCELABLE, routeProposalList);
		bundle.putInt(KEY_PROPOSALPOSITION, proposalPosition);
		bundle.putParcelable(RouteDeviData.PARCELABLE, deviList);
		
		
		intent.putExtras(bundle);
		activity.startActivity(intent);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        /*
         * Analytics
         */
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start("UA-16690738-3", this);
		tracker.trackPageView("/detailedRouteView");
		/*
		 * Setup the view
		 */
		setContentView(R.layout.route_detailed);
		routeList = new RouteAdapter(this);

		/*
		 * Setup the next/previous buttons
		 */
        mNextImageView = (ImageView) findViewById(R.id.next_entry);
        mPrevImageView = (ImageView) findViewById(R.id.prev_entry);
        OnClickListener switchEntryOnClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (v.getId()) {
				case R.id.next_entry:
					proposalPosition++;
					break;
				case R.id.prev_entry:
					proposalPosition--;
					break;
				}
				if (proposalPosition < 0)
					proposalPosition = 0;
				if (proposalPosition > routeProposalList.size() - 1)
					proposalPosition = routeProposalList.size() - 1;
				load();
				
			}
        };
        mNextImageView.setOnClickListener(switchEntryOnClickListener);
        mPrevImageView.setOnClickListener(switchEntryOnClickListener);

		
		/*
		 * Load instance state
		 */
		final Bundle bundle = (savedInstanceState != null) ? savedInstanceState : getIntent().getExtras();
		routeProposalList = bundle.getParcelableArrayList(RouteProposal.PARCELABLE);
		proposalPosition = bundle.getInt(KEY_PROPOSALPOSITION);
		deviList = bundle.getParcelable(RouteDeviData.PARCELABLE);
		load();
		registerForContextMenu(getListView());
		loadDevi();
	}
	
	/*
	 * Load devi, this shouldn't be needed, but might be if a station got loaded too fast.
	 */
	private void loadDevi() {
		routeDeviLoader = new RouteDeviLoader(this, deviList, new IGenericProviderHandler<Void>() {

			@Override
			public void onData(Void data) {}

			@Override
			public void onExtra(int i, Object data) {}

			@Override
			public void onPostExecute(Exception e) {
	        	Toast.makeText(DetailedRouteView.this, R.string.trafikantenErrorOther, Toast.LENGTH_SHORT).show();
	        	routeDeviLoader = null;
	        	routeList.notifyDataSetChanged();
	        	loadDevi();
			}

			@Override
			public void onPreExecute() {}
			
		});
		if (routeDeviLoader.load(routeProposalList.get(proposalPosition))) {
			setProgressBarIndeterminateVisibility(true);
		} else {
			setProgressBarIndeterminateVisibility(false);
		}				
	}
	
	/*
	 * NEXT/PREVIOUS CODE STOLEN FROM ANDROID GALLERY APP STARTS HERE.
	 */
    private ImageView mNextImageView;
    private ImageView mPrevImageView;
    private boolean mPaused = false;
    private Handler mHandler = new Handler();
    private final Animation mHideNextImageViewAnimation = new AlphaAnimation(1F, 0F);
    private final Animation mHidePrevImageViewAnimation = new AlphaAnimation(1F, 0F);
    private final Animation mShowNextImageViewAnimation = new AlphaAnimation(0F, 1F);
    private final Animation mShowPrevImageViewAnimation = new AlphaAnimation(0F, 1F);
    
    @Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
    	showOnScreenControls();
    	scheduleDismissOnScreenControls();
		return super.dispatchTouchEvent(ev);
	}

	private void scheduleDismissOnScreenControls() {
        mHandler.removeCallbacks(mDismissOnScreenControlRunner);
        mHandler.postDelayed(mDismissOnScreenControlRunner, 2000);
    }
    
    private final Runnable mDismissOnScreenControlRunner = new Runnable() {
        public void run() {
            hideOnScreenControls();
        }
    };
    
    private void hideOnScreenControls() {
        if (mNextImageView.getVisibility() == View.VISIBLE) {
            Animation a = mHideNextImageViewAnimation;
            a.setDuration(500);
            mNextImageView.startAnimation(a);
            mNextImageView.setVisibility(View.INVISIBLE);
        }

        if (mPrevImageView.getVisibility() == View.VISIBLE) {
            Animation a = mHidePrevImageViewAnimation;
            a.setDuration(500);
            mPrevImageView.startAnimation(a);
            mPrevImageView.setVisibility(View.INVISIBLE);
        }
    }
    
    private void showOnScreenControls() {
        if (mPaused) return;
        boolean showPrev = proposalPosition > 0;
        boolean showNext = proposalPosition < routeProposalList.size() - 1;

        boolean prevIsVisible = mPrevImageView.getVisibility() == View.VISIBLE;
        boolean nextIsVisible = mNextImageView.getVisibility() == View.VISIBLE;

        if (showPrev && !prevIsVisible) {
            Animation a = mShowPrevImageViewAnimation;
            a.setDuration(500);
            mPrevImageView.startAnimation(a);
            mPrevImageView.setVisibility(View.VISIBLE);
        } else if (!showPrev && prevIsVisible) {
            Animation a = mHidePrevImageViewAnimation;
            a.setDuration(500);
            mPrevImageView.startAnimation(a);
            mPrevImageView.setVisibility(View.GONE);
        }

        if (showNext && !nextIsVisible) {
            Animation a = mShowNextImageViewAnimation;
            a.setDuration(500);
            mNextImageView.startAnimation(a);
            mNextImageView.setVisibility(View.VISIBLE);
        } else if (!showNext && nextIsVisible) {
            Animation a = mHideNextImageViewAnimation;
            a.setDuration(500);
            mNextImageView.startAnimation(a);
            mNextImageView.setVisibility(View.GONE);
        }

    }
    
	@Override
	protected void onStart() {
		mPaused = false;
		super.onStart();
	}

	@Override
	protected void onStop() {
		mPaused = true;
		if (routeRealtimeLoader != null) {
			routeRealtimeLoader.kill();
		}
		super.onStop();
	}
	/*
	 * NEXT/PREVIOUS CODE STOLEN FROM ANDROID GALLERY APP END HERE.
	 */
	/*
	 * Options menu, visible on menu button.
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuItem map = menu.add(0, MAP_ID, 0, R.string.map);
		map.setIcon(android.R.drawable.ic_menu_mapmode);
		
		return true;
	}
	
	/*
	 * Options menu item selected, options menu visible on menu button.
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case MAP_ID:
        	GenericMap.Show(this, routeProposalList.get(proposalPosition).travelStageList, true, 0);
        	break;
        }
		return super.onOptionsItemSelected(item);
	}
	
	/*
	 * Load station data
	 */
	private void load() {
		final ArrayList<RouteData> list = routeProposalList.get(proposalPosition).travelStageList;
		routeList.setList(list);
		setListAdapter(routeList);
		// this forces a redraw of onscreen controls
        showOnScreenControls();
	}
	
	/*
	 * onCreate - Context menu is a popup from a longpress on a list item.
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, NOTIFY_ID, 0, R.string.alarm);
		
		final RouteData routeData = (RouteData) routeList.getItem(selectedId);
		if (routeData.fromStation.realtimeStop) {
			menu.add(0, REALTIME_ID, 0, R.string.realtime);
		}
	}
	
	private void loadRealtimeData(RouteData routeData) {
		if (routeData.fromStation.realtimeStop) {
			if (routeRealtimeLoader == null) {
				routeRealtimeLoader = new RouteRealtimeLoader(tracker, this, routeList);
			}

			routeRealtimeLoader.load(routeData.fromStation, routeData);
		}
	}
    
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		final RouteData routeData = (RouteData) routeList.getItem(selectedId);
		if (routeData.realtimeData == null) {
			loadRealtimeData(routeData);			
		} else {
			routeData.realtimeData = null;
			routeList.notifyDataSetChanged();
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
        final RouteData routeData = (RouteData) routeList.getItem(selectedId);
		
		switch(item.getItemId()) {
		case NOTIFY_ID:
			final String notifyWith = routeData.line.equals(routeData.destination) ? routeData.line : routeData.line + " " + routeData.destination;
			new NotificationTask(this, tracker, routeProposalList, proposalPosition, deviList, routeData.departure, notifyWith);
			return true;
		case REALTIME_ID:
			loadRealtimeData(routeData);
		}
		return super.onContextItemSelected(item);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(RouteProposal.PARCELABLE, routeProposalList);
		outState.putInt(KEY_PROPOSALPOSITION, proposalPosition);
		outState.putParcelable(RouteDeviData.PARCELABLE, deviList);
	}
	
	@Override
	protected void onDestroy() {
		if (routeDeviLoader != null) {
			routeDeviLoader.kill();
			routeDeviLoader = null;
		}
		super.onDestroy();
		//Stop the tracker when it is no longer needed.
		tracker.stop();
	}
}

class RouteRealtimeLoader {
	private static final String TAG = "Trafikanten-RouteRealtimeLoader";
	private TrafikantenRealtime realtimeProvider = null;
	public GoogleAnalyticsTracker tracker;
	private Activity activity;
	private RouteAdapter routeList;
	
	public RouteRealtimeLoader(GoogleAnalyticsTracker tracker, Activity activity, RouteAdapter routeList) {
		this.tracker = tracker;
		this.activity = activity;
		this.routeList = routeList;
		
	}
	
	public void load(StationData station, final RouteData routeData) {
		kill();
		
		tracker.trackEvent("Data", "Realtime", "Data", 0);
		realtimeProvider = new TrafikantenRealtime(activity, station.stationId, new IGenericProviderHandler<RealtimeData>() {
			@Override
			public void onExtra(int what, Object obj) {
				switch (what) {
				case TrafikantenRealtime.MSG_TIMEDATA:
					routeData.timeDifference = (Long) obj;
					break;
				}
			}
			
			@Override
			public void onData(RealtimeData item) {
				if (item.line.equals(routeData.line) && item.destination.equals(routeData.destination)) {
					if (routeData.realtimeData == null) {
						routeData.realtimeData = item;
						routeList.notifyDataSetChanged();
					} else {
						routeData.realtimeData.addDeparture(item.expectedDeparture, item.realtime, item.stopVisitNote);
						routeList.notifyDataSetChanged();
					}
				}
			}

			@Override
			public void onPostExecute(Exception exception) {
				activity.setProgressBarIndeterminateVisibility(false);
				realtimeProvider = null;
				if (exception != null) {
					Log.w(TAG,"onException " + exception);
			        if (exception.getClass().getSimpleName().equals("ParseException")) {
			        	Toast.makeText(activity,R.string.trafikantenErrorParse, Toast.LENGTH_LONG).show();
			        } else {
			        	Toast.makeText(activity,R.string.trafikantenErrorOther, Toast.LENGTH_LONG).show();
			        }
				} else {
					/*
					 * No exceptions
					 */
					if (routeData.realtimeData == null) {
						/*
						 * Show info text if view is empty
						 */
						Toast.makeText(activity,R.string.realtimeEmpty, Toast.LENGTH_LONG).show();
					}
				}
				routeList.notifyDataSetChanged();
			}

			@Override
			public void onPreExecute() {
				activity.setProgressBarIndeterminateVisibility(true);
			}
		});
	}
	
	public void kill() {
		if (realtimeProvider != null) {
			realtimeProvider.kill();
		}
	}
	
}


class RouteAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private ArrayList<RouteData> items = new ArrayList<RouteData>();
	private DetailedRouteView parent;
	
	public RouteAdapter(DetailedRouteView parent) {
		inflater = LayoutInflater.from(parent);
		this.parent = parent;
	}
	
	/*
	 * Simple functions dealing with adding/setting items. 
	 */
	public ArrayList<RouteData> getList() { return items; }
	public void setList(ArrayList<RouteData> items) { this.items = items; }
	public void clear() { items.clear(); }
	
	/*
	 * Standard android.widget.Adapter items, self explanatory.
	 */
	@Override
	public int getCount() {	return items.size(); }
	@Override
	public Object getItem(int pos) { return items.get(pos); }
	@Override
	public long getItemId(int pos) { return pos; }
	
	/*
	 * Setup the view
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int pos, View convertView, ViewGroup arg2) {
		/*
		 * Setup holder, for performance and readability.
		 */
		ViewHolder holder;
		if (convertView == null) {
			/*
			 * New view, inflate and setup holder.
			 */
			convertView = inflater.inflate(R.layout.route_detailed_list, null);
			
			holder = new ViewHolder();
			holder.symbol = (ImageView) convertView.findViewById(R.id.symbol);
			holder.line = (TextView) convertView.findViewById(R.id.line);
			holder.transportDestination = (TextView) convertView.findViewById(R.id.transportDestination);
			holder.from = (TextView) convertView.findViewById(R.id.from);
			holder.fromTime = (TextView) convertView.findViewById(R.id.fromTime);
			holder.to = (TextView) convertView.findViewById(R.id.to);
			holder.toTime = (TextView) convertView.findViewById(R.id.toTime);
			holder.waittime = (TextView) convertView.findViewById(R.id.waittime);
			holder.devi = (LinearLayout) convertView.findViewById(R.id.devi);
			holder.realtimeSymbol = (ImageView) convertView.findViewById(R.id.realtimeSymbol);
			holder.departures = (TextView) convertView.findViewById(R.id.departures);
			holder.departurePlatform = (TextView) convertView.findViewById(R.id.departurePlatform);
			convertView.setTag(holder);
		} else {
			/*
			 * Old view found, we can reuse that instead of inflating.
			 */
			holder = (ViewHolder) convertView.getTag();
		}
		
		/*
		 * Render data to view.
		 */
		final RouteData routeData = items.get(pos);
		
		if (routeData.transportType == R.drawable.icon_line_walk) {
			holder.transportDestination.setText(R.string.walk);
			holder.line.setVisibility(View.GONE);
		} else {
			holder.transportDestination.setText(routeData.destination);
			holder.line.setVisibility(View.VISIBLE);
		}
		holder.line.setText(routeData.line);
				
		holder.from.setText(routeData.fromStation.stopName);
		holder.fromTime.setText(HelperFunctions.hourFormater.format(routeData.departure));
		
		holder.to.setText(routeData.toStation.stopName);
		holder.toTime.setText(HelperFunctions.hourFormater.format(routeData.arrival));
		
		/*
		 * Setup symbol.
		 */
		final int symbolImage = routeData.transportType;
		if (symbolImage > 0) {
			holder.symbol.setVisibility(View.VISIBLE);
			holder.symbol.setImageResource(symbolImage);
		} else {
			holder.symbol.setVisibility(View.GONE);
		}
		
		/*
		 * Setup realtime button
		 */
		if (routeData.fromStation.realtimeStop) {
			holder.realtimeSymbol.setVisibility(View.VISIBLE);
		} else {
			holder.realtimeSymbol.setVisibility(View.GONE);
		}
		
		/*
		 * Setup realtime view
		 */
		if (routeData.realtimeData != null) {
			holder.departures.setText(routeData.realtimeData.renderDepartures(System.currentTimeMillis() - routeData.timeDifference, parent));
			if (routeData.realtimeData.departurePlatform > 0) {
				holder.departurePlatform.setText("Plattform:" + routeData.realtimeData.departurePlatform);
				holder.departurePlatform.setVisibility(View.VISIBLE);
			} else {
				holder.departurePlatform.setVisibility(View.GONE);
			}
			holder.departures.setVisibility(View.VISIBLE);
		} else {
			holder.departures.setVisibility(View.GONE);
			holder.departurePlatform.setVisibility(View.GONE);
		}
		
		/*
		 * Setup waittime
		 */
		if (routeData.waitTime > 0) {
			holder.waittime.setText("" + parent.getText(R.string.waitTime) + " : " + routeData.waitTime + " min");
			holder.waittime.setVisibility(View.VISIBLE);
		} else {
			holder.waittime.setVisibility(View.GONE);
		}
		
		/*
		 * Setup devi
		 */
		final String deviKey = parent.deviList.getDeviKey(routeData.fromStation.stationId, routeData.line);
		final ArrayList<DeviData> deviList = parent.deviList.items.get(deviKey);
		holder.devi.removeAllViews();
		if (deviList != null) {
			/*
			 * We have devi, add them all to the devi list.
			 */
			for (DeviData deviData : deviList) {
				holder.devi.addView(GenericDeviCreator.createDefaultDeviText(parent, parent.tracker, deviData.title, deviData, true), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));				
			}
		}
		
		return convertView;
	}
	
	/*
	 * Class for caching the view.
	 */
	static class ViewHolder {
		ImageView symbol;
		TextView line;
		TextView transportDestination;
		TextView from;
		TextView fromTime;
		TextView to;
		TextView toTime;
		TextView waittime;
		
		ImageView realtimeSymbol;
		TextView departures;
		TextView departurePlatform;
		
		LinearLayout devi;
	}
}
