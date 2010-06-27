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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.RouteSearchData;
import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.tasks.ShowHelpTask;
import com.neuron.trafikanten.views.route.SelectRouteAdapter.GenericRouteEntry;
import com.neuron.trafikanten.views.route.SelectRouteAdapter.SeperatorRouteEntry;
import com.neuron.trafikanten.views.route.SelectRouteAdapter.SimpleTextRouteEntry;

public class SelectRouteView extends ListActivity {
	private static final String TAG = "Trafikanten-SelectRouteView";
	private static final int ACTIVITY_SELECT_FROM = 1;
	private static final int ACTIVITY_SELECT_TO = 2;
	
	private static final int DIALOG_SELECTTIME = 1;
	private static final int DIALOG_CHANGEMARGIN = 2;
	private static final int DIALOG_PROPOSALS = 3;
	
	private SelectRouteAdapter listMenu;

	private RouteSearchData routeSearch;
	private GoogleAnalyticsTracker tracker;
	
	/*
	 * Options menu items:
	 */
	private final static int RESET_ID = Menu.FIRST;
	private final static int HELP_ID = Menu.FIRST + 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.selectroute);
		
        /*
         * Analytics
         */
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start("UA-16690738-3", this);
		tracker.trackPageView("/selectRouteView");
		
		listMenu = new SelectRouteAdapter();
		
		/*
		 * Load instance state
		 */
		if (savedInstanceState != null) {
			routeSearch = savedInstanceState.getParcelable(RouteSearchData.PARCELABLE);
		} else {
			routeSearch = new RouteSearchData();
		}
		refreshMenu();
		setListAdapter(listMenu);
	}
	
	private void resetView() {
		routeSearch = new RouteSearchData();
		refreshMenu();
	}
	
	/*
	 * This renders the entire menu
	 */
	public void refreshMenu() {
		ArrayList<GenericRouteEntry> items = new ArrayList<GenericRouteEntry>();
		/*
		 * Setup the basic menu:
		 */
		items.add(new SeperatorRouteEntry(this, R.string.selectStations));
		
		/*
		 * Setup From
		 */
		CharSequence travelFromString = null;
		if (routeSearch.fromStation.size() == 0) {
			travelFromString = getText(R.string.selectWhereToTravelFrom);
		} else {
			for (StationData station : routeSearch.fromStation) {
				if (travelFromString == null) {
					travelFromString = getText(R.string.travelingFrom) + " : " + station.stopName;
				} else {
					travelFromString = travelFromString + ", " + station.stopName;
				}
			}
		}
		items.add(new SimpleTextRouteEntry(this, R.string.travelFrom, travelFromString, 0, 0, new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SelectRouteView.this, SelectRouteStationView.class);
				startActivityForResult(intent, ACTIVITY_SELECT_FROM);
				refreshMenu();
			}
		}));
		
		/*
		 * Setup to
		 */
		CharSequence travelToString = null;
		if (routeSearch.toStation.size() == 0) {
			travelToString = getText(R.string.selectWhereToTravelTo);
		} else {
			for (StationData station : routeSearch.toStation) {
				if (travelToString == null) {
					travelToString = getText(R.string.travelingTo) + " : " + station.stopName;
				} else {
					travelToString = travelToString + ", " + station.stopName;
				}
			}
		}
		items.add(new SimpleTextRouteEntry(this, R.string.travelTo, travelToString, 0, 0, new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SelectRouteView.this, SelectRouteStationView.class);
				startActivityForResult(intent, ACTIVITY_SELECT_TO);
				refreshMenu();
			}
		}));
		
		items.add(new SeperatorRouteEntry(this, R.string.changeSettings));
		/*
		 * Setup when
		 */
		final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("EEEEEEE dd-MM-yyyy HH:mm");
		String travelTime;
		if (routeSearch.arrival == 0) {
			/*
			 * Travel type "travel at"
			 */
			travelTime = routeSearch.departure == 0 ? getText(R.string.travelAt) + " : " + getText(R.string.now) : getText(R.string.travelAt) + " : " + DATEFORMAT.format(routeSearch.departure);
		} else {
			/*
			 * Travel type "arrive before"
			 */
			travelTime = routeSearch.arrival == 0 ? getText(R.string.arriveBefore) + " : " + getText(R.string.now) : getText(R.string.arriveBefore) + " : " + DATEFORMAT.format(routeSearch.arrival);
		}
		items.add(new SimpleTextRouteEntry(this, R.string.when, travelTime, 5, 0, new OnClickListener() {
			@Override
			public void onClick(View v) {
				SelectRouteView.this.showDialog(DIALOG_SELECTTIME);
				refreshMenu();
			}
		}));
		
		
		{
			/*
			 * advanced onClickListener is shared
			 */
			final OnClickListener advancedOnClickListener = new OnClickListener() {
	
				@Override
				public void onClick(View v) {
					routeSearch.advancedOptionsEnabled = !routeSearch.advancedOptionsEnabled;
					refreshMenu();
				}
				
			};
			
			if (routeSearch.advancedOptionsEnabled) {
				items.add(new SimpleTextRouteEntry(this, R.string.advancedOptions,getText(R.string.disable) + " " + getText(R.string.advancedOptions), 0, 0, advancedOnClickListener));
				
				/*
				 * Add advanced options
				 */
				/*items.add(new CheckboxRouteEntry(this, R.string.preferDirect, routeSearch.preferDirect, 10, new OnClickListener() {
					@Override
					public void onClick(View v) {
						routeSearch.preferDirect = !routeSearch.preferDirect;
						refreshMenu();
					}
				}));
				items.add(new CheckboxRouteEntry(this, R.string.avoidWalking, routeSearch.avoidWalking, 10, new OnClickListener() {
					@Override
					public void onClick(View v) {
						routeSearch.avoidWalking = !routeSearch.avoidWalking;
						refreshMenu();
					}
				}));*/
				
				items.add(new SimpleTextRouteEntry(this, getText(R.string.changeMargin) + " : " + routeSearch.changeMargin + "m", getText(R.string.safeMargin), 10, 0, new OnClickListener() {
					@Override
					public void onClick(View v) {
						SelectRouteView.this.showDialog(DIALOG_CHANGEMARGIN);
						refreshMenu();						
					}
				}));
				
				items.add(new SimpleTextRouteEntry(this, getText(R.string.proposals) + " : " + routeSearch.proposals, getText(R.string.maxProposals), 10, 0, new OnClickListener() {
					@Override
					public void onClick(View v) {
						SelectRouteView.this.showDialog(DIALOG_PROPOSALS);
						refreshMenu();
					}
				}));
				
			} else {
				items.add(new SimpleTextRouteEntry(this, R.string.advancedOptions, getText(R.string.enable) + " " + getText(R.string.advancedOptions), 0, 0, advancedOnClickListener));
			}
		}
		
		items.add(new SeperatorRouteEntry(this, R.string.searchForRoutes));
		items.add(new SimpleTextRouteEntry(this, R.string.search, getText(R.string.calculateRouteResults), 0, R.drawable.ic_btn_search, new OnClickListener() {

			@Override
			public void onClick(View v) {
				onSearch();
			}
			
		}));
		
		listMenu.setList(items);
		listMenu.notifyDataSetChanged();
	}
	
	/*
	 * Options menu, visible on menu button.
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuItem favorites = menu.add(0, RESET_ID, 0, R.string.reset);
		favorites.setIcon(android.R.drawable.ic_menu_revert);
		
		final MenuItem help = menu.add(0, HELP_ID, 0, R.string.help);
		help.setIcon(android.R.drawable.ic_menu_help);
		return true;
	}

	/*
	 * Options menu item selected, options menu visible on menu button.
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case RESET_ID:
        	resetView();
        	break;
        case HELP_ID:
        	new ShowHelpTask(this, tracker);
        	break;
        default:
        	Log.e(TAG, "onOptionsItemSelected unknown id " + item.getItemId());
        }
		return super.onOptionsItemSelected(item);
	}
	
	/*
	 * Do actual search
	 */
	public void onSearch() {
		if (routeSearch.fromStation.size() == 0) {
			Toast.makeText(this, R.string.pleaseSelectFrom, Toast.LENGTH_SHORT).show();
			return;
		}
		if (routeSearch.toStation.size() == 0) {
			Toast.makeText(this, R.string.pleaseSelectTo, Toast.LENGTH_SHORT).show();
			return;
		}
		
		OverviewRouteView.ShowRoute(this, routeSearch);
	}
	
	/*
	 * Get get results from opened activities
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != RESULT_OK) return;
		
		switch(requestCode) {
		case ACTIVITY_SELECT_FROM:
		case ACTIVITY_SELECT_TO:
			if (data.hasExtra(StationData.PARCELABLE)) {
				/*
				 * Return data is a single station
				 */
				final StationData station = data.getParcelableExtra(StationData.PARCELABLE);
				if (requestCode == ACTIVITY_SELECT_FROM) {
					routeSearch.fromStation.clear();
					routeSearch.fromStation.add(station);
				} else {
					routeSearch.toStation.clear();
					routeSearch.toStation.add(station);
				}
			} else {
				/*
				 * Return data is a set of stations
				 */
				final ArrayList<StationData> stationList = data.getParcelableArrayListExtra(SelectRouteStationView.STATIONLIST_PARCELABLE);
				if (requestCode == ACTIVITY_SELECT_FROM) {
					routeSearch.fromStation = stationList;
				} else {
					routeSearch.toStation = stationList;
				}
			}
			break;
		default:
			Log.e(TAG, "Unknown request code " + requestCode);
		}
		refreshMenu();
	}
	
	
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_SELECTTIME:
			/*
			 * Create a dialog for selecting travel day and time.
			 */
			final Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.selectroute_dialog_timepick);

			/*
			 * Setup the travel at/arrive before spinner.
			 */
			final ArrayList<CharSequence> travelAtArriveBeforeItems = new ArrayList<CharSequence>();
			travelAtArriveBeforeItems.add(getText(R.string.travelAt));
			travelAtArriveBeforeItems.add(getText(R.string.arriveBefore));
			final Spinner travelAtArriveBeforeSpinner = (Spinner) dialog.findViewById(R.id.travelAtArriveBefore);
			final ArrayAdapter<CharSequence> travelAtArriveBeforeAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, travelAtArriveBeforeItems);
			travelAtArriveBeforeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			travelAtArriveBeforeSpinner.setAdapter(travelAtArriveBeforeAdapter);
			
			/*
			 * Setup the time picker
			 */
			final TimePicker timePicker = (TimePicker) dialog.findViewById(R.id.timePicker);
			timePicker.setIs24HourView(true);
			
			/*
			 * Setup list of days in "Select day to travel" dropdown.
			 */
			final Spinner dayList = (Spinner) dialog.findViewById(R.id.dayList);
			final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("EEEEEEE dd-MM-yyyy");
			ArrayList<String> dateList = new ArrayList<String>();
			
			Calendar date = Calendar.getInstance();
			for (int i = 0; i < 14; i++) {
				dateList.add(DATEFORMAT.format(date.getTime()));
				date.add(Calendar.DAY_OF_YEAR, 1);
			}
			
			final ArrayAdapter<String> dayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, dateList);
			dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			dayList.setAdapter(dayAdapter);
			
			/*
			 * Setup button
			 */
			final Button okButton = (Button) dialog.findViewById(R.id.okButton);
			okButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						boolean travelAt = travelAtArriveBeforeSpinner.getSelectedItemPosition() == 0;
						
						Date date = DATEFORMAT.parse(dayAdapter.getItem(dayList.getSelectedItemPosition()));
		                date.setHours(timePicker.getCurrentHour());
		                date.setMinutes(timePicker.getCurrentMinute());
		                if (travelAt) {
		                	routeSearch.departure = date.getTime();
		                	routeSearch.arrival = 0;
		                } else {
		                	routeSearch.departure = 0;
		                	routeSearch.arrival = date.getTime();	                	
		                }
						refreshMenu();
						dialog.dismiss();
					} catch (ParseException e) {
						// This can't happen.
					}					
				}
				
			});
			
			return dialog;
		case DIALOG_CHANGEMARGIN:
			final CharSequence[] changeMarginItems = {"1m", "2m", "3m", "4m", "5m"};
			
			AlertDialog.Builder changeBuilder = new AlertDialog.Builder(this);
			changeBuilder.setTitle(R.string.setChangeMargin);
			changeBuilder.setItems(changeMarginItems, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	routeSearch.changeMargin = item + 1;
			        refreshMenu();
			    }
			});
			return changeBuilder.create();
		case DIALOG_PROPOSALS:
			final CharSequence[] proposalItems = {"1", "2", "3", "4", "5", "10"};
			
			AlertDialog.Builder changeProposals = new AlertDialog.Builder(this);
			changeProposals.setTitle(R.string.setProposals);
			changeProposals.setItems(proposalItems, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	if (item == 5) {
			    		routeSearch.proposals = 10; 
			    	} else {
			    		routeSearch.proposals = item + 1;
			    	}
			        refreshMenu();
			    }
			});
			return changeProposals.create();
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch(id) {
		case DIALOG_SELECTTIME:
			/*
			 * Simply recreate this dialog.
			 */
			removeDialog(DIALOG_SELECTTIME);
			dialog = onCreateDialog(DIALOG_SELECTTIME);
			break;
		}
		super.onPrepareDialog(id, dialog);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		listMenu.click(position);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(RouteSearchData.PARCELABLE, routeSearch);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Stop the tracker when it is no longer needed.
		tracker.stop();
	}
}


class SelectRouteAdapter extends BaseAdapter
{
	private ArrayList<GenericRouteEntry> items = new ArrayList<GenericRouteEntry>();
	
	public void setList(ArrayList<GenericRouteEntry> items) {
		this.items = items;
	}
	
	public void click(int pos) {
		items.get(pos).onClick();
	}
	
	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public GenericRouteEntry getItem(int pos) {
		return items.get(pos);
	}

	@Override
	public long getItemId(int pos) {
		return pos;
	}

	@Override
	public View getView(int pos, View arg1, ViewGroup arg2) {
		return items.get(pos).mView;
	}
	
	@Override
	public boolean isEnabled(int position) {
		return !items.get(position).getClass().equals(SeperatorRouteEntry.class);
	}




	/*
	 * Fairly complicated view to render everything in the select route list.
	 *   - Supports Linearlayout with titles and extra text
	 *   - Supports checkboxes
	 *   - Supports expandable, and having a parent to see if it's been expanded.
	 */
	public static class GenericRouteEntry {
        public View mView;
        private OnClickListener onClickListener;
        
        public GenericRouteEntry(OnClickListener onClickListener) {
        	this.onClickListener = onClickListener;
        }
        
        public void onClick() {
        	if (onClickListener != null)
        		onClickListener.onClick(mView);
        }
    }
	
	public static class SeperatorRouteEntry extends GenericRouteEntry {
		private TextView mText;
		
		public SeperatorRouteEntry(Context context, int text) {
			super(null);
			mText = new TextView(context, null);
			mText.setText(text);
			mText.setGravity(Gravity.CENTER);
			mText.setBackgroundResource(R.drawable.skin_platform);
			mText.setTextColor(Color.WHITE);
			mView = mText;
			
		}
	}
	
	public static class SimpleTextRouteEntry extends GenericRouteEntry {
        private TextView mTitle;
        private TextView mSubText;
        private ImageView mSymbol;
        
        private void setup(Context context, int indent, int logoResource) {
			mView = LayoutInflater.from(context).inflate(R.layout.selectroute_textrouteentry, null);
			if (indent > 0) {
				mView.setPadding(mView.getPaddingLeft() + indent, mView.getPaddingTop(), mView.getPaddingRight(), mView.getPaddingBottom());
			}
			mTitle = (TextView) mView.findViewById(R.id.title);
			mSubText = (TextView) mView.findViewById(R.id.subText);
			
			if (logoResource > 0) {
				mSymbol = (ImageView) mView.findViewById(R.id.symbol);
				mSymbol.setVisibility(View.VISIBLE);
				mSymbol.setImageResource(logoResource);
			}
        }
        
		public SimpleTextRouteEntry(Context context, int title, CharSequence extra, int indent, int logoResource, OnClickListener onClickListener) {
			super(onClickListener);
			setup(context, indent, logoResource);
			
            mTitle.setText(title);
            mSubText.setText(extra);
        }
        
		public SimpleTextRouteEntry(Context context, CharSequence title, CharSequence extra, int indent, int logoResource, OnClickListener onClickListener) {
			super(onClickListener);
			setup(context, indent, logoResource);
			
            mTitle.setText(title);
            mSubText.setText(extra);
        }
	}
	
	public static class CheckboxRouteEntry extends GenericRouteEntry {
        private CheckBox mCheckBox;
		/*
		 * This is a checkbox layout.
		 */
		public CheckboxRouteEntry(Context context, int title, boolean checked, int paddingLeft, OnClickListener onClickListener) {
			super(onClickListener);
			
        	LinearLayout linearLayout = new LinearLayout(context);
        	linearLayout.setPadding(paddingLeft + 2, 2, 2, 2);
        	linearLayout.setOrientation(LinearLayout.VERTICAL);
			
			mCheckBox = new CheckBox(context);
			mCheckBox.setText(title);
			mCheckBox.setChecked(checked);
			mCheckBox.setTextColor(context.getResources().getColorStateList(R.color.skin_stoppested));
			
			linearLayout.addView(mCheckBox, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			mView = linearLayout;
		}
	}
	
}


