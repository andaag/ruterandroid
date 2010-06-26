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

package com.neuron.trafikanten.tasks;

import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.view.View;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataSets.DeviData;

public class ShowDeviTask implements GenericTask {
	public final static SimpleDateFormat dateFormater = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private Activity activity;
    private DeviData data;
    private Dialog dialog;
    private GoogleAnalyticsTracker tracker;
    
    public ShowDeviTask(Activity activity, GoogleAnalyticsTracker tracker, DeviData data) 
    {
        this.activity = activity;
        this.tracker = tracker;
        tracker.trackPageView("/task/showDevi");
        this.data = data;
        showDialog();
    }
    
    private CharSequence stripCode(String data) {
    	String r = data.replaceAll("<br />", "\n").replaceAll("\n\n", "\n");
    	if (r.endsWith("\n")) {
    		return r.subSequence(0, r.length() - 1);
    	}
    	return r;
    }
    
    private void showDialog() {
		dialog = new Dialog(activity, android.R.style.Theme);
		dialog.setTitle(R.string.deviTitle);
		dialog.setContentView(R.layout.dialog_showdevi);
		
		final TextView validPeriod = (TextView) dialog.findViewById(R.id.validPeriod);
		final TextView title = (TextView) dialog.findViewById(R.id.title);
		final TextView description = (TextView) dialog.findViewById(R.id.description);
		final TextView body = (TextView) dialog.findViewById(R.id.devibody);
		
		if (data.validFrom > 0) {
			validPeriod.setText(activity.getText(R.string.validFrom) + " " + dateFormater.format(data.validFrom).toString());
		} else {
			validPeriod.setVisibility(View.GONE);
		}
		final CharSequence strippedTitle = stripCode(data.title); 
		title.setText(strippedTitle);
		description.setText(stripCode(data.description));
		
		final CharSequence bodyText = stripCode(data.body);
		if (bodyText.length() < 3) {
			body.setVisibility(View.GONE);
		}
		else {
			body.setText(stripCode(data.body));
		}
		
		
        dialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				tracker.stop();				
			}	
        });
		dialog.show();
    }
    
	@Override
	public void stop() {
		dialog.dismiss();
	}

}
