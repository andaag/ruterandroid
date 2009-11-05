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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.SearchStationData;

public class SelectRouteView extends Activity {
	private static final String TAG = "Trafikanten-SelectRouteView";
	private static final int ACTIVITY_SELECT_FROM = 1;
	private static final int ACTIVITY_SELECT_TO = 2;
	
	private ViewHolder viewHolder = new ViewHolder();
	private RouteData routeData = new RouteData();
	
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
		 * Setup viewHolder
		 */
		viewHolder.fromButton = (Button) findViewById(R.id.fromButton);
		viewHolder.toButton = (Button) findViewById(R.id.toButton);
		viewHolder.timePicker = (TimePicker) findViewById(R.id.timePicker);
		viewHolder.departureDay = (Spinner) findViewById(R.id.departureDay);
		viewHolder.searchButton = (Button) findViewById(R.id.searchButton);
		
		/*
		 * Setup time picker:
		 */
        {
            viewHolder.timePicker.setIs24HourView(true);
            final Calendar calendar = Calendar.getInstance();
            viewHolder.timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
            viewHolder.timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
    }
		
		/*
		 * Load instance state
		 */
		if (savedInstanceState != null) {
			routeData = savedInstanceState.getParcelable(RouteData.PARCELABLE);
		}
		
		/*
		 * Fill list of days and refresh button list
		 */
		fillDayList();
		refreshButtons();
		
		/*
		 * Setup fromButton
		 */
		viewHolder.fromButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SelectRouteView.this, SelectRouteStationView.class);
				startActivityForResult(intent, ACTIVITY_SELECT_FROM);
			}
		});
		
		/*
		 * Setup toButton
		 */
		viewHolder.toButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SelectRouteView.this, SelectRouteStationView.class);
				startActivityForResult(intent, ACTIVITY_SELECT_TO);
			}
		});
		
		/*
		 * Setup searchButton
		 */
		viewHolder.searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				search();
			}
		});
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
	 * Reset view (used by RESET_ID and show settings)
	 */
	private void resetView() {
    	/*
    	 * Reset view
    	 */
        viewHolder.timePicker.setCurrentHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        viewHolder.timePicker.setCurrentMinute(Calendar.getInstance().get(Calendar.MINUTE));
		routeData = new RouteData();
		fillDayList();
		refreshButtons();		
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
        	final Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("http://code.google.com/p/trafikanten/"));
        	startActivity(intent);
        	break;
        default:
        	Log.e(TAG, "onOptionsItemSelected unknown id " + item.getItemId());
        }
		return super.onOptionsItemSelected(item);
	}
	
	/*
	 * Refresh station information
	 */
	private void refreshButtons() {
		/*
		 * FromButton
		 */
		if (routeData.fromStation == null) {
			viewHolder.fromButton.setText(R.string.travelFrom);
		} else {
			viewHolder.fromButton.setText(routeData.fromStation.stopName);			
		}
		
		/*
		 * ToButton
		 */
		if (routeData.toStation == null) {
			viewHolder.toButton.setText(R.string.travelTo);
		} else {
			viewHolder.toButton.setText(routeData.toStation.stopName);			
		}
	}
	
	/*
	 * Do actual search
	 */
	private void search() {
		if (routeData.fromStation == null) {
			Toast.makeText(this, R.string.pleaseSelectFrom, Toast.LENGTH_SHORT).show();
			return;
		}
		if (routeData.toStation == null) {
			Toast.makeText(this, R.string.pleaseSelectTo, Toast.LENGTH_SHORT).show();
			return;
		}
		

        /*
         * Convert TimePicker + departureDay holder to a (long) Date
         */
        try {
                Date date = DATEFORMAT.parse((String) viewHolder.departureDay.getSelectedItem());
                date.setHours(viewHolder.timePicker.getCurrentHour());
                date.setMinutes(viewHolder.timePicker.getCurrentMinute());
                routeData.departure = date.getTime();
        } catch (ParseException e) {
                // This should NEVER happen...
                Log.e(TAG,"ParseException " + e);
                Toast.makeText(this, "ParseException parsing day (this should NEVER happen, report!)", Toast.LENGTH_LONG).show();
        }
		
		
		DetailedRouteView.ShowRoute(this, routeData);
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
			final SearchStationData station = data.getParcelableExtra(SearchStationData.PARCELABLE);
			if (requestCode == ACTIVITY_SELECT_FROM) {
				routeData.fromStation = station;
			} else {
				routeData.toStation = station;
			}
			refreshButtons();
			break;
		default:
			Log.e(TAG, "Unknown request code " + requestCode);
		}
	}

	/*
	 * Setup list of days in "Select day to travel" dropdown.
	 */
	private final static SimpleDateFormat DATEFORMAT = new SimpleDateFormat("EEEEEEE dd-MM-yyyy");
	private void fillDayList() {
		ArrayList<String> dateList = new ArrayList<String>();
		
		Calendar date = Calendar.getInstance();
		for (int i = 0; i < 14; i++) {
			dateList.add(DATEFORMAT.format(date.getTime()));
			date.add(Calendar.DAY_OF_YEAR, 1);
		}
		
		ArrayAdapter<String> dayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, dateList);
		dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		viewHolder.departureDay.setAdapter(dayAdapter);
	}
	
	
	/*
	 * onPause
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
	}

	/*
	 * Resume state, restart search.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
        /*
         * Check current time, if it's too old, reset it.
         */
        Date date;
        try {
                date = DATEFORMAT.parse((String) viewHolder.departureDay.getSelectedItem());
                date.setHours(viewHolder.timePicker.getCurrentHour());
                date.setMinutes(viewHolder.timePicker.getCurrentMinute());
                
                Calendar currentDate = Calendar.getInstance();
                currentDate.roll(Calendar.MINUTE, -30);
                if (date.before(currentDate.getTime())) {
                        /*
                         * Date is timepicker is more than 30 minutes old, reset it.
                         */
                        viewHolder.timePicker.setCurrentHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
                        viewHolder.timePicker.setCurrentMinute(Calendar.getInstance().get(Calendar.MINUTE));
                }

        } catch (ParseException e) {
                Log.e(TAG,"ParseException onResume, this should never happen " + e);
        }
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(RouteData.PARCELABLE, routeData);
	}
	
	/*
	 * Static class for holding the gui elements
	 */
	static class ViewHolder {
		Button fromButton;
		Button toButton;
		TimePicker timePicker;
		Spinner departureDay;
		Button searchButton;
	}

}
