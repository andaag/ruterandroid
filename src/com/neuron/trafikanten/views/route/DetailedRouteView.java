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
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.RouteProposal;
import com.neuron.trafikanten.notification.NotificationDialog;
import com.neuron.trafikanten.views.map.GenericMap;

public class DetailedRouteView extends ListActivity {
	//private final static String TAG = "Trafikanten-DetailedRouteView";
	private RouteAdapter routeList;
	private ViewHolder viewHolder = new ViewHolder();
	private GoogleAnalyticsTracker tracker;
	
	/*
	 * Context menu:
	 */
	private static final int NOTIFY_ID = Menu.FIRST;
	
	/*
	 * Option menu
	 */
	private final static int MAP_ID = Menu.FIRST + 1;
	
	/*
	 * Dialogs
	 */
	private static final int DIALOG_NOTIFICATION = 1;
	private int selectedId = 0;
	
	/*
	 * Saved instance data
	 */
	private ArrayList<RouteProposal> routeProposalList;
	private int proposalPosition;

	/*
	 * this is a list of departure dates, it's used to store next/previous button clicks.
	 * See the button onClick listeners for how this works.
	 */
	public final static String KEY_PROPOSALPOSITION = "proposalPosition";

	public static void ShowRoute(Activity activity, ArrayList<RouteProposal> routeProposalList, int proposalPosition) {
		Intent intent = new Intent(activity, DetailedRouteView.class);
		intent.putExtra(RouteProposal.PARCELABLE, routeProposalList);
		intent.putExtra(KEY_PROPOSALPOSITION, proposalPosition);
		activity.startActivity(intent);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		viewHolder.previousButton = (ImageButton) findViewById(R.id.prevButton);
		viewHolder.infoText = (TextView) findViewById(R.id.infoText);
		viewHolder.nextButton = (ImageButton) findViewById(R.id.nextButton);

		/*
		 * Setup onClick handler on previous button
		 */
		viewHolder.previousButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/*
				 * Search for previous departure date.
				 */
				proposalPosition--;
				load();
			}
		});
		
		/*
		 * Setup onClick handler on next button
		 */
		viewHolder.nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/*
				 * Simply set departureDate to our first arrival time + 1 minute, that way the search will skip that route and take the next one.
				 */
				proposalPosition++;
				load();
			}
		});
		
		/*
		 * Setup the next/previous buttons (NEW CODE)
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
		load();
		registerForContextMenu(getListView());
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
        	GenericMap.Show(this, GenericMap.getStationList(routeProposalList.get(proposalPosition).travelStageList), true, 0);
        	break;
        }
		return super.onOptionsItemSelected(item);
	}
	
	/*
	 * Refresh button.isEnabled
	 */
	private void refreshButtons() {
		viewHolder.previousButton.setEnabled(proposalPosition != 0 && routeProposalList.size() > 1);
		viewHolder.nextButton.setEnabled(proposalPosition < routeProposalList.size() - 1);
	}
	
	/*
	 * Load station data
	 */
	private void load() {
		final ArrayList<RouteData> list = routeProposalList.get(proposalPosition).travelStageList;
		routeList.setList(list);
		setListAdapter(routeList);
		refreshButtons();
        showOnScreenControls();
	}
	
	/*
	 * We need to override setListAdapter to trigger refreshing info overlay.
	 * @see android.app.ListActivity#setListAdapter(android.widget.ListAdapter)
	 */
	@Override
	public void setListAdapter(ListAdapter adapter) {
		super.setListAdapter(adapter);
		final ArrayList<RouteData> list = routeList.getList();
		/*
		 * Render info box on the bottom
		 */
		if (list.size() > 0) {
			viewHolder.infoText.setVisibility(View.VISIBLE);
			
			/*
			 * Calculate total travel time, waittime and walktime. 
			 */
			final long departure = list.get(0).departure;
			long arrival = list.get(list.size() - 1).arrival;
			int waitTime = 0;
			int walkTime = 0;
			
			if (arrival < departure) {
				/*
				 * We're arriving before we're leaving, must be a days difference.
				 */
				arrival = arrival + (HelperFunctions.HOUR * 24);
			}
			
			for (RouteData data : list) {
				waitTime = waitTime + data.waitTime;
				if (data.transportType == R.drawable.icon_walk) {
					walkTime = walkTime + (int)((data.arrival - data.departure) / HelperFunctions.MINUTE);					
				}
			}
			
			/*
			 * Setup actual text
			 */
			//Hack:
			long travelTime = (arrival - departure) / HelperFunctions.MINUTE;
			if (travelTime > HelperFunctions.HOUR * 24 / HelperFunctions.MINUTE)
				travelTime = travelTime - (HelperFunctions.HOUR * 24 / HelperFunctions.MINUTE);

			
			viewHolder.infoText.setText("" + getText(R.string.travelTime) + " " + travelTime + " min\n" +
					getText(R.string.waitTime) + " " + waitTime + " min\n" +
					getText(R.string.walkTime) + " " + walkTime + " min");
		} else {
			viewHolder.infoText.setVisibility(View.INVISIBLE);
		}
	}

	/*
	 * Dialog creation
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_NOTIFICATION:
			/*
			 * notify dialog
			 */
			return NotificationDialog.getDialog(this, tracker, 0);
		}
		return super.onCreateDialog(id);
	}

	/*
	 * Load data into dialog
	 * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		final RouteData notifyRouteData = (RouteData) routeList.getItem(selectedId);
		/*
		 * Departure is what we base our notification on, 10 minuts before departure
		 */
		final long notifyDeparture = notifyRouteData.departure;
		final String notifyWith = notifyRouteData.line.equals(notifyRouteData.destination) ? notifyRouteData.line : notifyRouteData.line + " " + notifyRouteData.destination;
		NotificationDialog.setRouteData(routeProposalList, proposalPosition, notifyDeparture, notifyWith);
		super.onPrepareDialog(id, dialog);
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
	}
    
	/*
	 * onSelected - Context menu is a popup from a longpress on a list item.
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        selectedId = info.position;
		
		switch(item.getItemId()) {
		case NOTIFY_ID:
			showDialog(DIALOG_NOTIFICATION);
			return true;
		}
		return super.onContextItemSelected(item);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(RouteProposal.PARCELABLE, routeProposalList);
		outState.putInt(KEY_PROPOSALPOSITION, proposalPosition);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Stop the tracker when it is no longer needed.
		tracker.stop();
	}
	
	/*
	 * Class for holding the view.
	 */
	static class ViewHolder {
		ImageButton previousButton;
		TextView infoText;
		ImageButton nextButton;
	}
}


class RouteAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private ArrayList<RouteData> items = new ArrayList<RouteData>();
	private Context context;
	
	public RouteAdapter(Context context) {
		inflater = LayoutInflater.from(context);
		this.context = context;
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
		
		if (routeData.transportType == R.drawable.icon_walk) {
			holder.transportDestination.setText(R.string.walk);
		} else {
			holder.transportDestination.setText(routeData.destination);
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
		 * Setup waittime
		 */
		if (routeData.waitTime > 0) {
			holder.waittime.setText("" + context.getText(R.string.waitTime) + " " +
					HelperFunctions.renderAccurate(routeData.waitTime * (HelperFunctions.MINUTE)));
			holder.waittime.setVisibility(View.VISIBLE);
		} else {
			holder.waittime.setVisibility(View.GONE);
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
	}
}
