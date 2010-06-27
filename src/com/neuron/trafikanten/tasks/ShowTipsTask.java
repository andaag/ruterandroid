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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.neuron.trafikanten.R;

public class ShowTipsTask implements GenericTask {
	private final static String PREF_TIPSHOWN = "tipshown-";
    private Activity activity;
    private GoogleAnalyticsTracker tracker;
    private Dialog dialog;
    private SharedPreferences preferences;
    
    private String tipClassName;
    private int tipMessage;
    private int tipLastUpdated;
    
    
    public ShowTipsTask(Activity activity, GoogleAnalyticsTracker tracker, String tipClassName, int tipMessage, int tipLastUpdated) 
    {
        this.activity = activity;
        this.tracker = tracker;
        this.tipClassName = tipClassName;
        this.tipMessage = tipMessage;
        this.tipLastUpdated = tipLastUpdated;
        preferences = activity.getSharedPreferences("trafikantenandroid", Context.MODE_PRIVATE);
        
        /*
         * Check if this message has been shown.
         */
        
        if (shouldShow()) {
            tracker.trackPageView("/task/showTips");
        	showDialog();
        	updateLastShown();
        }
    }
    
    /*
     * This scans tipMessage 
     */
    private boolean shouldShow() {
    	int lastTipShown = preferences.getInt(PREF_TIPSHOWN + tipClassName, -1);
    	
    	if (lastTipShown < tipLastUpdated) {
    		return true;
    	}
    	return false;
    }
    
    /*
     * This sets the preferance for last shown tip
     */
    private void updateLastShown() {
    	SharedPreferences.Editor editor = preferences.edit();
    	try {
			editor.putInt(PREF_TIPSHOWN + tipClassName, activity.getPackageManager().getPackageInfo("com.neuron.trafikanten", PackageManager.GET_META_DATA).versionCode);
		} catch (NameNotFoundException e) {}
    	editor.commit();
    }

    
    private void showDialog() {
        
        /*
         * Check if we found anything at all
         */
    	
    	
    	
    	/*final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    	
    	
    	builder.setTitle("Tips");
    	builder.setPositiveButton(android.R.string.ok,null);
    	builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setMessage(tipMessage);
        dialog = builder.create();
        
    	TextView message = (TextView)((AlertDialog)dialog).findViewById(android.R.id.message);
    	message.setTextAppearance(activity, android.R.style.TextAppearance_Small);*/
    	
    	dialog = new Dialog(activity);
    	dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	
    	dialog.setContentView(R.layout.dialog_showtips);
    	final TextView message = (TextView)dialog.findViewById(R.id.message);
    	message.setText(tipMessage);
    	final TextView title = (TextView)dialog.findViewById(R.id.alertTitle);
    	title.setText("Tips");

    	final Button okButton = (Button)dialog.findViewById(R.id.okButton);
    	okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
    		
    	});
    	

        dialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				tracker.stop();
			}
        	
        });
        
        /*
         * Show dialog
         */
        dialog.show();
    }
    
	@Override
	public void stop() {
		dialog.dismiss();
	}
}