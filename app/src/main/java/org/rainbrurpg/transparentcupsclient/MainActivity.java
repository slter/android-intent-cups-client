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

import android.Manifest;
import android.content.Context;
import android.app.Activity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.widget.TextView;
import android.print.PrintManager;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.net.Uri;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.SocketTimeoutException;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.content.pm.PackageManager;

import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;
import org.cups4j.PrintRequestResult;

import org.rainbrurpg.transparentcupsclient.detect.MdnsServices;
import org.rainbrurpg.transparentcupsclient.detect.PrinterRec;
import org.rainbrurpg.transparentcupsclient.detect.PrinterResult;

public class MainActivity extends Activity {

    String printer;   // The printer URL
    String photoPath; // The filename of the photo to be printed
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
    }

    protected void onStart()
    {
	L.w("Starting MainActivity");
	super.onStart();
	TextView tv = (TextView)findViewById(R.id.textView);

	// Get last selected printer
	SharedPreferences myPreference=PreferenceManager.
	    getDefaultSharedPreferences(getApplicationContext());
	this.printer = myPreference.getString("printer_list", "NONE");
	L.w("Main activity : getting printer default value : " + printer);

	if (printer == "NONE"){
	    tv.setText("Aucun imprimante sélectionnée!");
	    return;
	}
	else{
	    tv.setText("Impression en cours...");
	}
	
    
	Bundle bundle = getIntent().getExtras();
	Uri uri = (Uri)bundle.get("android.intent.extra.STREAM");
	this.photoPath = uri.getPath();

	// Intent extra debugging
	/*
	L.w("Got intent extra URI : ");
	if (bundle != null) {
	    for (String key : bundle.keySet()) {
		Object value = bundle.get(key);
		L.w(String.format("[%s: %s (%s)", key, value,
				  value.getClass().getName()));
	    }
	}
	*/
	
	checkPermissions();
    }


    /** Try to get external storage permission
     *
     * The first maunch of the app, we will show a confirmation dialog.
     *
     */
    protected void checkPermissions(){
	if (ContextCompat.
	    checkSelfPermission(this,
				Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
	    L.w("Can't access ext. storage, requesting permission!");

	    ActivityCompat.
		requestPermissions(this,
				   new String[]{Manifest.permission.
						READ_EXTERNAL_STORAGE},
				   123);

	} else {
	    printFile();
	}
    }

    @Override
public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
        case 123: {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED)     {
                L.w("Permission granted!");
            } else {

                checkPermissions();
            }
            return;
        }
    }
    }

    protected void printFile(){
	L.w("Photo file is : '" + this.photoPath + "'");

        try {
	    URI tmpUri = new URI(this.printer);
	    String schemeHostPort = tmpUri.getScheme() + "://" + tmpUri.getHost() + ":" + tmpUri.getPort();
	    
	    
	    // Prepare job
	    final URL printerURL = new URL(this.printer);
	    final URL clientURL = new URL(schemeHostPort);
	    L.w("Printer URL is " + printerURL);
	    L.w("Client URL is " + clientURL);

	    // Get the selected printer : since we cannot make network work
	    // in Main thread, we have to use AsyncTask
	    new AsyncTask<Void, Void, Void>() {
		Exception mException;
		
		@Override
		protected Void doInBackground(Void... urls) {
		    try{
			printDocument(clientURL, printerURL, photoPath);
		    } catch (Exception e) {
			mException = e;
		    }
		    return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
		    if (mException != null) {
			handleJobException(mException);
		    } else {
			//                        onPrintJobSent(printJob);
		    }
		    
		}
	    }.execute();
        } catch (MalformedURLException e) {
            L.e("Couldn't queue print job: ", e);
        } catch (URISyntaxException e) {
            L.e("Couldn't parse URI: " + this.printer, e);
        } catch (Exception e) {
	    L.e("Unknown exception when trying to print ",  e);
	}
	    
    }

    /**
     * Called from the UI thread.
     * Handle the exception (e.g. log or send it to crashlytics?), and inform the user of what happened
     *
     * @param e     The exception that occurred
     */
    void handleJobException(Exception e) {
	if (e instanceof SocketTimeoutException) {
            Toast.makeText(this, "Socket timeout", Toast.LENGTH_LONG).show();
        } else if (e instanceof NullPrinterException) {
            Toast.makeText(this, "Null printer", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Erreur d'impression", Toast.LENGTH_LONG).show();
	    L.e("Printing job exception : ", e);
        }
	waitAndFinish();
    }

    
    void waitAndFinish() {
	// Wait and finish
	Handler handler = new Handler();
	handler.postDelayed(new Runnable() {
                public void run() {
                    finish();
                }
	    }, 3000); // 3 seconds
    }
	
    /**
     * Called from a background thread, when the print job has to be sent to the printer.
     *
     * @param clientURL  The client URL
     * @param printerURL The printer URL
     * @param fd         The document to print, as a {@link FileDescriptor}
     */
    void printDocument(URL clientURL, URL printerURL,
		       String photopath) throws Exception {

	// Get photo file descriptor
	File f = new File(photopath);	
	L.w("Could we read this file ? " + f.canRead());
	L.w("Does this file exist ? " + f.exists());
	
	L.w("Must test for clientURL : " + clientURL);
	    
	CupsPrinter printer = new CupsPrinter(printerURL, "AZE", true);
	L.w("About to print document on :" + printer);
	
	FileInputStream fis = new FileInputStream(f);
	org.cups4j.PrintJob job = new org.cups4j.PrintJob.Builder(fis).build();
	L.w("PrintJob is :" + job);
	
	PrintRequestResult result = printer.print(job);
	L.w("PrintResult :" + result);
	if (result.isSuccessfulResult()) {
	    finish();
	} else {
            Toast.makeText(this, result.getResultDescription(),
			   Toast.LENGTH_LONG).show();
	    waitAndFinish();
	}
    }

    private static class NullPrinterException extends Exception {
        NullPrinterException() {
            super("Printer is null when trying to print: printer no longer available?");
        }
    }

    
}
