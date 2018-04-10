package org.rainbrurpg.transparentcupsclient.ssl;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.rainbrurpg.transparentcupsclient.CupsPrintApp;
import org.rainbrurpg.transparentcupsclient.app.HostNotVerifiedActivity;

import org.rainbrurpg.transparentcupsclient.L;

/**
 * Used with {@link HostNotVerifiedActivity} to trust certain hosts
 */
public class AndroidCupsHostnameVerifier implements HostnameVerifier {
    @Override
    public boolean verify(String hostname, SSLSession session) {
	L.d("Verifying SSL for host " + hostname);
        return HostNotVerifiedActivity.isHostnameTrusted(CupsPrintApp.getContext(), hostname);
    }
}
