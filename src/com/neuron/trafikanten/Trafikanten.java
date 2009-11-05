package com.neuron.trafikanten;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

import com.neuron.trafikanten.views.realtime.SelectRealtimeStationView;
import com.neuron.trafikanten.views.route.SelectRouteView;


//TODO : Go through variable naming for every class, check for int|string|etc.*m[A-Z]
//TODO FUTURE : Instead of tabs, use a "slide right for route view" like yr.no? (ViewFlipper)

public class Trafikanten extends TabActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	 	final TabHost tabHost = getTabHost();
	 	
	    tabHost.addTab(tabHost.newTabSpec("RealtimeTab")
	 			.setIndicator(getText(R.string.realtime), getResources().getDrawable(R.drawable.ic_menu_recent_history))
	 			.setContent(new Intent(this, SelectRealtimeStationView.class)));
	     
	    tabHost.addTab(tabHost.newTabSpec("RouteTab")
	 			.setIndicator(getText(R.string.route), getResources().getDrawable(R.drawable.ic_menu_directions))
	 			.setContent(new Intent(this, SelectRouteView.class)));
	}
	
	/*
	 * onPause - stop anything needed.
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
	}

	/*
	 * Resume state, refresh settings
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
	}
}