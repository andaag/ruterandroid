package com.neuron.trafikanten.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.NotificationData;
import com.neuron.trafikanten.dataSets.RouteDeviData;
import com.neuron.trafikanten.dataSets.RouteProposal;
import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.views.realtime.RealtimeView;
import com.neuron.trafikanten.views.route.DetailedRouteView;

/*
 * This is what gets triggered by AlarmService, what it does is trigger the notification.
 */
public class NotificationIntent extends BroadcastReceiver {
	//private static final String TAG = "Trafikanten-NotificationIntent";
	@Override
	public void onReceive(Context context, Intent recieveIntent) {
		/*
		 * This produces Class not found when unmarshalling, it seems.. unfixable.
		 * See http://osdir.com/ml/Android-Developers/2010-06/msg02498.html
				I've seen mention of this before on this list. IIRC, Android attempts
				to de-serialize the Parcelable inside the AlarmManagerService, and it
				does not have your class in the OS, just in your process. Hence, the
				operation fails.
		 */
		
		//Log.i(TAG,"onRecieve");
		
		final NotificationData notificationData = recieveIntent.getParcelableExtra(NotificationData.PARCELABLE);
    	final long departureTime = notificationData.getDepartureTime();
    	final String stopName = notificationData.getStopName();
    	final String with = notificationData.with;
    	
    	final String notificationString = with == null ? stopName : with + " " + context.getText(R.string.fromShort) + " " + stopName;
    	Notification notification = new Notification(R.drawable.icon_notification, notificationString, departureTime);
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
			bundle.putParcelable(StationData.PARCELABLE, notificationData.station);
	        intent.putExtras(bundle);
		} else {
			/*
			 * Route popup
			 */
			intent = new Intent(context, DetailedRouteView.class);
			intent.putExtra(RouteProposal.PARCELABLE, notificationData.routeProposalList);
			intent.putExtra(RouteDeviData.PARCELABLE, notificationData.deviList);
			intent.putExtra(DetailedRouteView.KEY_PROPOSALPOSITION, notificationData.proposalPosition);

	        intent.putExtras(bundle);
		}
    		
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
    	
        
        notification.setLatestEventInfo(context, context.getText(R.string.app_name), notificationString, contentIntent);
    	
    	final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    	notificationManager.notify(0, notification);
	}

}
