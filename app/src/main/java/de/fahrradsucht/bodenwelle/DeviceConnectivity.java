package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

/** Try an available Internet-capable network; Android validation can be disabled by VPN/privacy settings. */
final class DeviceConnectivity {
    private DeviceConnectivity() {}
    static boolean canRequest(Context context) {
        ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
        NetworkCapabilities capabilities = manager == null ? null : manager.getNetworkCapabilities(manager.getActiveNetwork());
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
