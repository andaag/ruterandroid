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

package com.neuron.trafikanten.notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TimePicker;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.dataSets.NotificationData;
import com.neuron.trafikanten.dataSets.RealtimeData;
import com.neuron.trafikanten.dataSets.RouteProposal;
import com.neuron.trafikanten.dataSets.StationData;

/*
 * Notification dialog, make sure to set realtimeData and station onPrepareDialog
 */
public class NotificationDialog {
	private static final String TAG = "Trafikanten-NotificationDialog";
	private static int notificationCode = 0;
	/*
	 * For realtime:
	 */
	private static RealtimeData sRealtimeData;
	private static StationData sStation;
	
	/*
	 * for route:
	 */
	private static ArrayList<RouteProposal> sRouteProposalList;
	private static int sProposalPosition;
	private static long sRouteDeparture;
	
	/*
	 * Shared
	 */
	private static String sWith;
	
	public static void setRealtimeData(RealtimeData realtimeData, StationData station, String with) {
		sRealtimeData = realtimeData;
		sStation = station;
		sWith = with;
		
		sRouteProposalList = null;
	}
	
	public static void setRouteData(ArrayList<RouteProposal> routeProposalList, int proposalPosition, long departure, String with) {
		sRouteProposalList = routeProposalList;
		sProposalPosition = proposalPosition;
		sRouteDeparture = departure;
		sWith = with;
		
		sRealtimeData = null;
	}
	
	public static TimePickerDialog getDialog(final Context context, final long timeDifference) {
    	return new TimePickerDialog(context, new OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				/*
				 * Convert hours/minutes to current date with set presets
				 */
				final Calendar calendar = Calendar.getInstance();
				long departure = sRealtimeData != null ? sRealtimeData.expectedDeparture : sRouteDeparture;
				assert(departure > 0);
				calendar.setTimeInMillis(departure);
				calendar.add(Calendar.HOUR_OF_DAY, -hourOfDay);
				calendar.add(Calendar.MINUTE, -minute);
				
				/*
				 * Get the data info, construct bundle, and send the data
				 */
				final Bundle bundle = new Bundle();
				
				NotificationData notificationData;
				if (sRealtimeData != null) {
					/*
					 * Realtime data
					 */
					notificationData = new NotificationData(sStation, sRealtimeData, calendar.getTimeInMillis() + timeDifference, sWith);
				} else {
					/*
					 * Route data
					 */
					notificationData = new NotificationData(sRouteProposalList, sProposalPosition, sRouteDeparture, calendar.getTimeInMillis(), sWith);
				}
				bundle.putParcelable(NotificationData.PARCELABLE, notificationData);
				
	            // Schedule the alarm
				final Intent intent = new Intent(context, NotificationIntent.class);
				intent.putExtras(bundle);
				//intent.putExtra(NotificationData.PARCELABLE, notificationData);
				
				Log.i(TAG,"Creating notification at " + HelperFunctions.renderAccurate(notificationData.notifyTime));
				final PendingIntent notificationIntent = PendingIntent.getBroadcast(context, notificationCode++, intent, PendingIntent.FLAG_ONE_SHOT);
	            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	            alarm.set(AlarmManager.RTC_WAKEUP, notificationData.notifyTime, notificationIntent);
				
	            sRealtimeData = null;
	            sRouteProposalList = null;
			}
    		
    	}, 0, 10, true);
	}
}
