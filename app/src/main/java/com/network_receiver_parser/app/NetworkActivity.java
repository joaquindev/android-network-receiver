package com.network_receiver_parser.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.text.DateFormat;
import java.net.URL;

/**
 * Main Activity for the application of XML parsing
 *
 * This activity does the following:
 * 1) Presents a WebView screen to users. This Webview has a lists of HTML links to the
 * latest questions tagged 'android' on Stackoverflow.com
 * 2) Parses the stackoverflow XML feed using XMLPullParser
 * 3) Uses AsyncTask to download and process the XML feed
 * 4) Monitors preferences and the device's mainmenu connection to determine whether
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
        //Register BroadcastReceiver to track connection changes
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetworkReceiver();
        this.registerReceiver(receiver, filter);
    }

    /**
     * Dispatch onStart() to all fragments.  Ensure any created loaders are
     * now started.
     * Refreshes the display if the network connection and the pref settings allow it
     */
    @Override
    protected void onStart() {
        super.onStart();
        //Gets the user's network preference settings
        SharedPreferences sharePrefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Retrieves a string value for the preferences. The second parameter is
        //the default value to use if a preference value is not found
        sPref = sharePrefs.getString("listPref", "Wi-Fi");
        updateConnectedFlags();
        //Only loads the page if refreshDisplay is true. Otherwise, keeps previous
        //display. For example, if the user has set "Wifi Only" in prefs and the
        //device loses its Wifi connection midway through the user using the app,
        //you don't want to refresh the display -- this would force the display
        //of an error page instead of stackoverflow content
        if (refreshDisplay) loadPage();
    }

    /**
     * Destroy all fragments and loaders.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) this.unregisterReceiver(receiver);
    }

    /**
     * Checks network connection and sets the wifiConnected and mobileConnected flags accordingly
     */
    private void updateConnectedFlags() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        } else {
            wifiConnected = false;
            mobileConnected = false;
        }
    }

    /**
     * Uses AsyncTask subclass to download the XML feed from stackoverflow.com
     * This avoids UI lock up. To prevent network operations from causing a delay
     * that results in a poor user experience, always perform network operations
     * on a separate thread from the UI.
     */
    private void loadPage() {
        if (((sPref.equals(ANY)) && (wifiConnected || mobileConnected))
                || ((sPref.equals(WIFI)) && (wifiConnected))) {
            new DownloadXmlTask().execute(URL);
        } else {
            showErrorPage();
        }
    }

    private void showErrorPage() {
        setContentView(R.layout.main);
        //The specified network connection is not available. So we display an error message
        WebView myWebView = (WebView) findViewById(R.id.webview);
        myWebView.loadData(getResources().getString(R.string.connection_error), "text/html", null);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsActivity = new Intent(getBaseContext(), SettingsActivity.class);//TODO: Implement this Activity
            startActivity(settingsActivity);
            return true;
        }
        if (id == R.id.refresh) {
            loadPage();
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
    public class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connMgr =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            //Checks the user prefs and the mainmenu connection. Based on the result,
            //it decides whether to refresh the display or keep the current display
            //If the userpref is Wi-Fi only, checks to see if the device has a Wi-Fi connection
            if (WIFI.equals(sPref) && networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                refreshDisplay = true; //If device has its Wi-Fi connection, sets refreshDisplay
                //to true. This causes the display to be refreshed when the user returns to the app
                Toast.makeText(context, "Wi-Fi reconnected", Toast.LENGTH_SHORT).show();
            } else if (ANY.equals(sPref) && networkInfo != null) {
                //If the setting is ANY mainmenu and there is a mainmenu connection, it is going to
                //be MOBILE so it sets refreshDisplay to true as well
                refreshDisplay = true;
                Toast.makeText(context, "Using 3G connection", Toast.LENGTH_SHORT).show();
            } else {
                //Otherwise, the app can't download content - either because there is no mainmenu
                //connection (MOBILE of Wi-Fi), or because the pref setting is WIFI, and there
                //is no Wi-Fi connection. In this case sets the refreshDisplay to false
                refreshDisplay = false;
                Toast.makeText(context, "Lost connection", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Implementation of AsyncTask used to download XML feed from stackoverflow.com
     */
    private class DownloadXmlTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                return loadXmlFromNetwork(urls[0]);
            } catch (IOException e) {
                return getResources().getString(R.string.connection_error);
            } catch (XmlPullParserException e) {
                return getResources().getString(R.string.xml_error);
            }
        }

        @Override
        protected void onPostExecute(String s) {
            setContentView(R.layout.main);
            //Display the HTML string in the UI via a WebView
            WebView myWebView = (WebView) findViewById(R.id.webview);
            myWebView.loadData(result, "text/html", null);
        }
    }

    /**
     * Downloads XML from stackoverflow.com, parses it and combines it with
     * HTML markup. Returns an HTML String page to be included in the webview
     *
     * @param urlString The URL where the XML is
     * @return The HTML string
     * @throws XmlPullParserException
     * @throws IOException
     */
    private String loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        StackOverflowXmlParser stackOverflowXmlParser = new StackOverflowXmlParser();//TODO: create class
        List<KeyStore.Entry> entries = null;//TODO> create Entry class
        String title, url, summary = null;
        Calendar rightNow = Calendar.getInstance();
        DateFormat formatter = new SimpleDateFormat("MMM dd h:mmaa");

        //Checks whether the user set the preference to include summary text
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean pref = sharedPrefs.getBoolean("summaryPref", false);

        //We start building the HTML string to be included in the webview
        StringBuilder htmlString = new StringBuilder();
        htmlString.append("<h3" + getResources().getString(R.string.page_title) + "</h3>");
        htmlString.append("<em>" + getResources().getString(R.string.updated) + " " +
                formatter.format(rightNow.getTime() + "</em>"));
        try {
            stream = downloadUrl(urlString);
            entries = stackOverflowXmlParser.parse(stream);
            //Makes sure that the InputStream is closed after the app is finished using it
        } finally {
            if (stream != null) stream.close();
        }

        /**
         * Given a string representation of a URL, sets up a connection and gets an input stream
         */
        private InputStream downloadUrl(String urlString) throws IOException{
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            //starts th query
            conn.connect();
            InputStream stream = conn.getInputStream();
            return stream;
        }

        /**
         * StackOverflowXmlParser returns a List (called "entries") of Entry objects.
         * Each Entry object represents a single post in the XML feed
         * This section processes the entries list to combine each entry with HTML markup
         * Each entry is displayed in the UI as a link that optionally includes a text summary
         */
        for(Entry entry : entries){
            htmlString.append("<p><a href='");
            htmlString.append(entry.link);
            htmlString.append("'>" + entry.title + "</a></p>");
            //If the user set the preference to include summary text, adds it to the display
            if(pref){
                htmlString.append(entry.summary);
            }
        }
        return htmlString.toString();
    }

}


