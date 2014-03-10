package com.network_receiver_parser.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * Main Activity for the application of XML parsing
 *
 * This activity does the following:
 * 1) Presents a WebView screen to users. This Webview has a lists of HTML links to the
 * latest questions tagged 'android' on Stackoverflow.com
 * 2) Parses the stackoverflow XML feed using XMLPullParser
 * 3) Uses AsyncTask to download and process the XML feed
 * 4) Monitors preferences and the device's network connection to determine whether
 * to refresh the webview content
 */
public class NetworkActivity extends ActionBarActivity {

    public static final String WIFI = "Wi-Fi";
    public static final String ANY = "Any";
    private static final String URL = "http://stackoverflow.com/feeds/tag?tagnames=android&sort=newest";
    private static boolean wifiConnected = false;
    private static boolean mobileConnected = false;
    public static boolean refreshDisplay = true;
    public static String sPref = null;
    private NetworkReceiver receiver = new NetworkReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.network, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * This BroadcastReceiver called Networkreceiver intercepts the
     * android.net.ConnectivityManager.CONNECTIVITY_ACTION, which indicates a
     * connnection change. It checks whether the type is TYPE_WIFI. If it is,
     * it checks whether Wi-Fi is connected and sets the wifiConnected flag in the
     * main activity accordingly.
     */
    public class NetworkReceiver  extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connMgr =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            //Checks the user prefs and the network connection. Based on the result,
            //it decides whether to refresh the display or keep the current display
            //If the userpref is Wi-Fi only, checks to see if the device has a Wi-Fi connection
            if(WIFI.equals(sPref) && networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                refreshDisplay = true; //If device has its Wi-Fi connection, sets refreshDisplay
                //to true. This causes the display to be refreshed when the user returns to the app
                Toast.makeText(context, "Wi-Fi reconnected", Toast.LENGTH_SHORT).show();
            }else if(ANY.equals(sPref) && networkInfo != null){
                //If the setting is ANY network and there is a network connection, it is going to
                //be MOBILE so it sets refreshDisplay to true as well
                refreshDisplay = true;
                Toast.makeText(context, "Using 3G connection", Toast.LENGTH_SHORT).show();
            }else{
                //Otherwise, the app can't download content - either because there is no network
                //connection (MOBILE of Wi-Fi), or because the pref setting is WIFI, and there
                //is no Wi-Fi connection. In this case sets the refreshDisplay to false
                refreshDisplay = false;
                Toast.makeText(context, "Lost connection", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
