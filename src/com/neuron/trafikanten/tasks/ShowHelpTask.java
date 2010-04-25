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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;

public class ShowHelpTask implements GenericTask {
	public final static SimpleDateFormat dateFormater = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private Activity activity;
    private Dialog dialog;
    private ClickableSpan sendMailSpan = new ClickableSpan() {
		@Override
		public void onClick(View widget) {
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND); 
			emailIntent.setType("plain/text"); 
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"aagaande@gmail.com"}); 
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Trafikanten android"); 
			activity.startActivity(emailIntent);
		}
    	
    };
    
    public ShowHelpTask(Activity activity) 
    {
        this.activity = activity;
        showDialog();
    }
    
    private void addTextSpan(SpannableStringBuilder builder, CharSequence text, Object what, int flags) {
    	int start = builder.length();
    	builder.append(text);
    	int end = builder.length();
    	builder.setSpan(what, start, end, flags);
    }
    
    private void showDialog() {
		dialog = new Dialog(activity);
		dialog.setTitle("Hjelp");
		dialog.setContentView(R.layout.dialog_progress);
		
		SpannableStringBuilder builder = new SpannableStringBuilder("Trafikanten for android er utviklet av Anders Aagaard i sammarbeid med ");
		addTextSpan(builder, "www.trafikanten.no", new URLSpan("http://www.trafikanten.no/"), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		builder.append(".\n\n");
		
		builder.append("Jeg tar gjerne tilbakemeldinger på ");
		addTextSpan(builder, "mail", sendMailSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		
		builder.append(" eller via ");
		addTextSpan(builder, "hjemmesiden", new URLSpan("http://code.google.com/p/trafikanten/"), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		builder.append(". Alle feilmeldinger/tilbakemeldinger skal gå til meg, ikke til trafikanten.\n\n");
		
		/*builder.append(" Du kan ringe trafikanten direkte på ");
		addTextSpan(builder, "177", new URLSpan("tel:177"), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		builder.append(" for å få øyeblikkelig ruteinformasjon.\n\n");*/
		
		
		
		final SharedPreferences preferences = activity.getSharedPreferences("trafikantenandroid", Context.MODE_PRIVATE);
		final double downloadMb = (double)preferences.getLong(HelperFunctions.KEY_DOWNLOADBYTE, 0) / 1024 / 1024;
		final double pris = (double)(1 * downloadMb);
		final double prisLarge = (double)(20 * downloadMb);
		/*builder.append("Din båndbredde bruk så langt er : " + new DecimalFormat("0.##").format(downloadMb) + "mb. ");
		builder.append("Med en eksempel pris på 1kr/mb har dette kostet deg : " + new DecimalFormat("0.##").format(pris) + "kr.");
		builder.append("Noen leverandører har svært høye data priser. Med en data pris på 20kr/mb (!) vil det ha kostet deg : " + new DecimalFormat("0.##").format(prisLarge) + "kr.");*/	
	
		
		
		builder.append("Din båndbredde bruk : " + new DecimalFormat("0.##").format(downloadMb) + "mb. \n");
		builder.append("Eksempel på hva dette har kostet deg:\n");
		builder.append(" - for 1kr/mb : " + new DecimalFormat("0.##").format(pris) + "kr.\n");
		builder.append(" - for 20kr (!) /mb : " + new DecimalFormat("0.##").format(prisLarge) + "kr.\n");
		builder.append("Priser pr mb varierer på mobil leverandør. For prisinformason på mobilleverandører se ");
		
		
		//builder.append("\nFor informasjon om kostnad hos din leverandør sjekk ");
		addTextSpan(builder, "www.telepriser.no", new URLSpan("http://www.telepriser.no"), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		builder.append(".");
		
        final ProgressBar progress = (ProgressBar) dialog.findViewById(android.R.id.progress);
        progress.setVisibility(View.GONE);
        final TextView message = (TextView) dialog.findViewById(R.id.message);
        message.setText(builder);
        message.setMovementMethod(LinkMovementMethod.getInstance());

		dialog.show();
    }
    
	@Override
	public void stop() {
		dialog.dismiss();
	}
}