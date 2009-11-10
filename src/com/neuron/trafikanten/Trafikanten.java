package com.neuron.trafikanten;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;

import com.neuron.trafikanten.views.realtime.SelectRealtimeStationView;
import com.neuron.trafikanten.views.route.SelectRouteView;


public class Trafikanten extends TabActivity {
	private static Activity activity;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Trafikanten.activity = this;
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	 	final TabHost tabHost = getTabHost();
	 	
	    tabHost.addTab(tabHost.newTabSpec("RealtimeTab")
	 			.setIndicator(getText(R.string.realtime), getResources().getDrawable(R.drawable.ic_menu_recent_history))
	 			.setContent(new Intent(this, SelectRealtimeStationView.class)));
	     
	    tabHost.addTab(tabHost.newTabSpec("RouteTab")
	 			.setIndicator(getText(R.string.route), getResources().getDrawable(R.drawable.ic_menu_directions))
	 			.setContent(new Intent(this, SelectRouteView.class)));
	}
	
	/*
	 * Function is always used from one of the tab views, therefor this is perfectly fine.
	 */
	public static void tabHostSetProgressBarIndeterminateVisibility(boolean value)
	{
		activity.setProgressBarIndeterminateVisibility(value);
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