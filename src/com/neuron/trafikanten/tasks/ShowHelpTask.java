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
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.AnalyticsUtils;
import com.neuron.trafikanten.R;

public class ShowHelpTask implements GenericTask {
	public final static SimpleDateFormat dateFormater = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private Activity activity;
    private Dialog dialog;
    
    public ShowHelpTask(Activity activity) 
    {
        this.activity = activity;
        AnalyticsUtils.getInstance(activity).trackPageView("/task/showhelp");
        showDialog();
    }
    
    private void showDialog() {
		dialog = new Dialog(activity);
		dialog.setTitle("Hjelp");
		dialog.setContentView(R.layout.dialog_progress);
		
		SpannableStringBuilder builder = new SpannableStringBuilder("Reiseplanlegger, avgangstider og kart dekker hele Østlandsområdet. I tillegg får du trafikkmeldinger for Oslo og Akershus.<br/><br/>" + 
                "Avgangstider i sanntid vises med gult klokkeslett der det er tilgjengelig. For øvrig vises avgangstider i hvitt etter planlagt rutetid.<br/><br/>" + 
                "Utvikler av applikasjonen er <a href='http://www.codebox.no'>codebox.no</a> og ansvarlig utgiver er Trafikanten AS- et datterselskap av <a href='http://www.ruter.no'>Ruter As</a>.<br/><br/>" + 
				  "For mer informasjon, se <a href='http://www.ruter.no/android'>ruter.no/android</a>.<br/><br/>");

		
		builder.append("Trafikanten på android er GPL lisensiert, og hele kildekoden kan lastes ned <a href='http://www.codebox.no'>via hjemmesiden</a>.");

		
		/* TODO : Remove logging of bandwidth 
		final SharedPreferences preferences = activity.getSharedPreferences("trafikantenandroid", Context.MODE_PRIVATE);
		final double downloadMb = (double)preferences.getLong(HelperFunctions.KEY_DOWNLOADBYTE, 0) / 1024 / 1024;
		final double pris = (double)(1 * downloadMb);
		final double prisLarge = (double)(20 * downloadMb);
		
		builder.append("Din båndbreddebruk: " + new DecimalFormat("0.##").format(downloadMb) + "MB. <br/>");
		builder.append("Eksempel på hva dette kan ha kostet deg:<br/>");
		builder.append(" - for 1kr/MB : " + new DecimalFormat("0.##").format(pris) + " kr.<br/>");
		builder.append(" - for 20kr (!) /MB : " + new DecimalFormat("0.##").format(prisLarge) + " kr.<br/>");
		builder.append("Priser pr mb varierer på mobil leverandør. For prisinformasjon på mobilleverandører se <a href='http://www.telepriser.no'>telepriser.no</a>."); */
		
        final ProgressBar progress = (ProgressBar) dialog.findViewById(android.R.id.progress);
        progress.setVisibility(View.GONE);
        final TextView message = (TextView) dialog.findViewById(R.id.message);
        message.setText(Html.fromHtml(builder.toString()));
        message.setMovementMethod(LinkMovementMethod.getInstance());

		dialog.show();
    }
    
	@Override
	public void stop() {
		dialog.dismiss();
	}
}