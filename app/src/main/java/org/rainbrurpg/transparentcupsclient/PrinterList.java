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

/** A class used to retrieve all connected USB devices and list them
 * in a ListPreference
 *
 */

package org.rainbrurpg.transparentcupsclient;

import android.os.AsyncTask;

import android.content.Context;

import android.preference.ListPreference;
import android.content.Intent;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
    
import android.content.Context;
import android.app.Activity;

import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;
import org.cups4j.WhichJobsEnum;
import org.cups4j.PrintJobAttributes;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.rainbrurpg.transparentcupsclient.app.UntrustedCertActivity;


class PrinterList {
    /** Here is the Printer List key, used both to get the 
     * ListPreference object and to get the Shared preference one.
     */
    public static final String PRINTER_LIST = "printer_list";

    PrinterList(Context c){
	
    }

        // Verify host finished for the given printer
    public void processFinish(boolean certException, URL printerURL ) {
	L.d("processFinish(" + printerURL + ") : " + certException);
    }
    
    
    static void addNone(ListPreference pl){
	List<CharSequence> entries = new ArrayList<CharSequence>();
	List<CharSequence> entryValues = new ArrayList<CharSequence>();

	entries.add("Aucune");
	entryValues.add("NONE");

	pl.setEntries(entries.toArray(new CharSequence[entries.size()]));
	pl.setEntryValues(entryValues.toArray(new CharSequence[entryValues.
							       size()]));

    }
    
    /** Update the list with deviceList
     *
     */
    static void updateList(ListPreference pl, Map<String, String> printers){
	List<CharSequence> entries = new ArrayList<CharSequence>();
	List<CharSequence> entryValues = new ArrayList<CharSequence>();

	// Re-add the None value
	entries.add("Aucune");
	entryValues.add("NONE");

	// Get all USB devices
	for (Map.Entry<String, String> entry : printers.entrySet()) {
	    entryValues.add(entry.getKey());
	    entries.add(entry.getValue());
	}

	pl.setEntries(entries.toArray(new CharSequence[entries.size()]));
	pl.setEntryValues(entryValues.toArray(new CharSequence[entryValues.
							       size()]));
	
    }
    
    /** This function verify that the printer's host is trusted
     *
     */
    static void verifyHost(String printerUrl){
	L.d("Should show HostNotVerified if not known : " +
	    printerUrl);

	String host;
	
	try {
	    URI tmpUri = new URI(printerUrl);
	    host = tmpUri.getScheme() + "://" +	tmpUri.getHost() + ":" +
		tmpUri.getPort();

	// Get CupsPrinter
	final String pstr = printerUrl;
	new AsyncTask<Void, Void, Void>() {
	    Exception mException;
	    boolean certException = false;
	    URL purl = null;
	    
	    @Override
	    protected Void doInBackground(Void... urls){
		
		try{
		    purl = new URL(pstr);
		    CupsPrinter printer = new CupsPrinter(purl,"AZE",true);
		    
		    // Get the printer's JobList to verify host
		    List<PrintJobAttributes> jobs = printer.
			    getJobs(WhichJobsEnum.ALL, null, true);
		}
		catch (SSLException | CertificateException e) {
		    certException = true;
		} catch (Exception e) {
		    mException = e;
		    L.e("Error while getting jobs : ", e);
		}
		return null;
	    }
	    
	    @Override
	    protected void onPostExecute(Void result) {
		
	    }
	}.execute();

	}
	catch (URISyntaxException e){
	    L.e("Failed to parse printer's URI", e);
	}
	catch (Exception e){
	    L.e("Failed to verify host", e);
	}
    }

    static boolean isCertificateTrusted(Context context, String printerUrl){
	
	final String pstr = printerUrl;
	new AsyncTask<Void, Void, Void>() {
	    Exception mException;
	    boolean certException = false;
	    URL purl = null;

	    X509Certificate[] certs;
	    
	    @Override
	    protected Void doInBackground(Void... urls){
		CupsClient client = null;
		try {
		    URI tmpUri = new URI(pstr);
		    String schemeHostPort = tmpUri.getScheme() + "://"
			+ tmpUri.getHost() + ":" + tmpUri.getPort();
		    L.d("Getting certs of " + schemeHostPort);
		    URL curl = new URL(schemeHostPort);
		    URL purl = new URL(schemeHostPort);
		    client = new CupsClient(curl);
		    // We must get printers before getting the certificate
		    CupsPrinter p = client.getPrinter(purl);
		} catch (SSLException | CertificateException e) {
		    certs = client.getServerCerts();
		    L.d("About to save certs : " + certs);
		    if (HttpConnectionManagement.saveCertificates(certs)){
			L.d("Server certificates corretcly saved");
		    }
		} catch (NoRouteToHostException e) {
		    L.d("No route to host for " + pstr);
		    
		} catch (Exception e) {
		    mException = e;
		    L.e("Error while getting jobs : ", e);
		}
		return null;
	    }
	    
	    @Override
	    protected void onPostExecute(Void result) {

	    }
	}.execute();
	return false;

    }
}

