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

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.neuron.trafikanten.dataSets.DeviData;

/*
 * This class takes a devi data set, and returns nothing.
 */

public class SelectDeviTask implements GenericTask {
    //private static final String TAG = "Trafikanten-SelectDeviTask";
    private Activity activity;
    private GoogleAnalyticsTracker tracker;
    private ArrayList<DeviData> devi;
    private Dialog dialog;
    
    public SelectDeviTask(Activity activity, GoogleAnalyticsTracker tracker, ArrayList<DeviData> devi) 
    {
        this.activity = activity;
        this.tracker = tracker;
        tracker.trackPageView("/task/selectDevi");
        this.devi = devi;
        showDialog();
    }
    
    /*
     * Setup dialog for selecting devi
     */
    private void showDialog() {
        /*
         * Setup list of devi titles
         */
        ArrayList<String> deviList = new ArrayList<String>();
        for (DeviData deviData : devi) {
        	deviList.add(deviData.title);
        }
        
        /*
         * Setup select devi dialog
         */
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        //builder.setTitle("Select Devi");
        final String[] items = new String[deviList.size()];
        deviList.toArray(items);
        
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                    dialog.dismiss();
                    deviSelected(devi.get(item));
            }
        });
        
        dialog = builder.create();
        
        /*
         * Show dialog
         */
        dialog.show();
    }
    
    private void deviSelected(DeviData deviData) {
    	new ShowDeviTask(activity, tracker, deviData);
    }

	@Override
	public void stop() {
		dialog.dismiss();
	}
}