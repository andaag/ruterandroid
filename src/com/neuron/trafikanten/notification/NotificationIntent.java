package com.neuron.trafikanten.notification;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.NotificationData;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.SearchStationData;
import com.neuron.trafikanten.views.RealtimeView;
import com.neuron.trafikanten.views.RouteView;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/*
 * This is what gets triggered by AlarmService, what it does is trigger the notification.
 */
public class NotificationIntent extends BroadcastReceiver {
	private static final String TAG = "NotificationIntent";
	@Override
	public void onReceive(Context context, Intent recieveIntent) {
		Log.i(TAG,"onRecieve");
		
		final NotificationData notificationData = recieveIntent.getParcelableExtra(NotificationData.PARCELABLE);
    	final long departureTime = notificationData.getDepartureTime();
    	final String stopName = notificationData.getStopName();
    	final String with = notificationData.with;
    	
    	final String notificationString = with == null ? stopName : with + " " + context.getText(R.string.fromShort) + " " + stopName;
    	Notification notification = new Notification(R.drawable.icon_bus, notificationString, departureTime);
    	//Notification.FLAG_INSISTENT
    	notification.flags |= Notification.FLAG_AUTO_CANCEL;
    	notification.defaults |= Notification.DEFAULT_ALL;
    	
		Bundle bundle = new Bundle();
		Intent intent;
		if (notificationData.departureInfo != null) {
	    	/*
			 * Setup realtime intent for popup.
			 */
			intent = new Intent(context, RealtimeView.class);
			bundle.putParcelable(SearchStationData.PARCELABLE, notificationData.station);
	        intent.putExtras(bundle);
		} else {
			/*
			 * Route popup
			 */
			final RouteData routeData = notificationData.routeData;
			intent = new Intent(context, RouteView.class);
			bundle.putParcelable(RouteData.PARCELABLE, routeData);
	        intent.putExtras(bundle);
		}
    		
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
    	
        
        notification.setLatestEventInfo(context, context.getText(R.string.app_name), notificationString, contentIntent);
    	
    	final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    	notificationManager.notify(0, notification);
	}

}
