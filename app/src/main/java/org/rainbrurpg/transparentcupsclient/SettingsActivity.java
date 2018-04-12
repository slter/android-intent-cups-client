/*
 * Copyright 2018 Jérôme Pasquier
 *
 * This file is part of transparent-cups-client.
 *
 * transparent-cups-client is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of the License, 
 * or (at your option) any later version.
 *
 * transparent-cups-client is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with transparent-cups-client.  If not, 
 * see <http://www.gnu.org/licenses/>.
 *
 */

package org.rainbrurpg.transparentcupsclient;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.ActionBar;
import android.support.annotation.NonNull;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.MenuItem;
import android.util.Log;
import android.app.Activity;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

import org.rainbrurpg.transparentcupsclient.detect.MdnsServices;
import org.rainbrurpg.transparentcupsclient.detect.PrinterRec;
import org.rainbrurpg.transparentcupsclient.detect.PrinterResult;

import org.rainbrurpg.transparentcupsclient.app.HostNotVerifiedActivity;
import org.rainbrurpg.transparentcupsclient.app.UntrustedCertActivity;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity{

    //    NewHostDialog nhd = new NewHostDialog(this, null);

    
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
		      index >= 0
		      ? listPreference.getEntries()[index]
		      : null);

		// Will verify that the host is known
		if (listPreference.getKey().equals("printer_list") &&
		    !stringValue.equals("NONE")){
		    
		    try {
			PrinterList.verifyHost(stringValue);
			URI tmpUri = new URI(stringValue);
			String h = tmpUri.getHost();
			Context ctx = CupsPrintApp.getInstance()
			    .getApplicationContext();
			if (!HostNotVerifiedActivity.isHostnameTrusted(ctx, h)){
			    Intent intent=new Intent(ctx,
						     HostNotVerifiedActivity.
						     class);
			    intent.putExtra(HostNotVerifiedActivity.KEY_HOST, h);
			    ctx.startActivity(intent);
			}
			if (!PrinterList.isCertificateTrusted(ctx,stringValue)) {
			    /*    Intent dialog = new Intent(ctx, UntrustedCertActivity.class);
			    //			dialog.putExtra(UntrustedCertActivity.KEY_CERT, mServerCerts[0]);
			    //dialog.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			    ctx.startActivity(dialog);
			    */
			}
			

		    } catch (URISyntaxException e) {
			L.e("Cannot get hostName :", e);
		    }
		}
		
            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

   
    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);


            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("example_text"));
            bindPreferenceSummaryToValue(findPreference("printer_list"));

	    ListPreference pl = (ListPreference)findPreference("printer_list");
	    PrinterList.addNone(pl);
	    
	    changePrinterListSummary("Recherche d'imprimantes en cours, " + 
				     "veullez patienter...");
	    feedPrinterList();

	}

	private void changePrinterListSummary(String str) {

	    ListPreference pl = (ListPreference)findPreference("printer_list");
	    pl.setSummary(str);
	}
	
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    

	private void  feedPrinterList(){
	    new AsyncTask<Void, Void, Map<String, String>>() {
		@Override
		protected Map<String, String> doInBackground(Void... params) {
		    return scanPrinters();
		}
		
		@Override
		protected void onPostExecute(Map<String, String> printers) {
		    //		    onPrintersDiscovered(printers);
		    ListPreference pl = (ListPreference)
			findPreference("printer_list");
		    PrinterList.updateList(pl, printers);

		    SharedPreferences myPreference=PreferenceManager.
			getDefaultSharedPreferences(pl.getContext());
		    String cp = myPreference.getString("printer_list", "NONE");
		    L.w("Getting printer default value : " + cp);
		    int index = pl.findIndexOfValue(cp);
		    L.d("Getting PL entry for index " + index);
		    CharSequence[ ] entries = pl.getEntries();
		    if (index > 0 && index < entries.length) {
			String summ = (String)entries[index];
			changePrinterListSummary(summ);
		    } else {
			changePrinterListSummary("AUCUNE");
		    }
		    
		}
	    }.execute();
	}

    /**
     * Ran in background thread. Will do an mDNS scan of local printers
     *
     * @return The list of printers as {@link PrinterRec}
     */
    @NonNull
    Map<String, String> scanPrinters() {
        final MdnsServices mdns = new MdnsServices();
        PrinterResult result = mdns.scan();

        //TODO: check for errors
        Map<String, String> printers = new HashMap<>();
        String url, name;

        // Add the printers found by mDNS
        for (PrinterRec rec : result.getPrinters()) {
            url = rec.getProtocol() + "://" + rec.getHost() + ":" + rec.getPort() + "/printers/" + rec.getQueue();
            printers.put(url, rec.getNickname());
	    L.w("Found printer " + rec.getNickname());
        }

        return printers;
    }
	
    }
}

