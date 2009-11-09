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

import com.neuron.trafikanten.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.widget.TextView;

public abstract class GenericTask extends Activity {
	public final static String KEY_MESSAGE = "message";
	public TextView message;
	
	public abstract int getlayoutId();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(getlayoutId());
		message = (TextView) findViewById(R.id.message);
	}
	
	/*
	 * Handler for calling a task onCreate.
	 * You can't call startActivityForResult onCreate, so we postMessage to our own handler and call it there.
	 */
	private static Activity handlerActivity;
	private static Intent handlerIntent;
	public static void StartGenericTask(Activity activity, Intent intent, int what) {
		handlerActivity = activity;
		handlerIntent = intent;
		onCreateHandler.sendEmptyMessage(what);
	}
    public static final Handler onCreateHandler = new Handler() {
        public void handleMessage(Message msg) {
        	if (handlerActivity != null) {
	        	handlerActivity.startActivityForResult(handlerIntent, msg.what);
	        	handlerActivity = null;
	        	handlerIntent = null;
        	}
        }
    };
	

    /*
     * A Generic handler that passes things to parent.
     */
    public final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
        	/*
        	 * Simply pass the message to the caller.
        	 */
        	Intent intent = new Intent();
        	intent.putExtra(KEY_MESSAGE, msg);
        	setResult(RESULT_OK, intent);
        	finish();
        }
    };
}
