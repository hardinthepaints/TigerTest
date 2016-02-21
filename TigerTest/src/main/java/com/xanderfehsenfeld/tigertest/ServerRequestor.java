package com.xanderfehsenfeld.tigertest;

import android.util.Log;

import com.xanderfehsenfeld.tigertest.LocalDB.MyDbWrapper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Xander on 11/16/15.
 *
 * Handle connection to a server and simple requests
 */
public class ServerRequestor {

    private static HttpClient httpclient;

    private static String TAG = "ServerRequestor";

    static final String ERROR_RESPONSE_READ = "error reading response";


//    /**getDownloadUrl
//     *
//     * @param serverUrl the url of the server
//     * @return the url of the file for speedtest to download
//     * @throws IOException
//     */
//
//    public static String getDownloadUrl( String serverUrl ) throws IOException {
//
//        // init client
//        httpclient = new DefaultHttpClient();
//
//        /* store your params as name value pairs */
//        List<NameValuePair> params = new ArrayList<NameValuePair>();
//
//        /* Add in requests */
//        params.add(new BasicNameValuePair("request", "download_file_url" ) );
//
//        String paramString = URLEncodedUtils.format(params, "utf-8");
//
//        serverUrl += "?" + paramString;
//
//        // Prepare a request object
//        HttpGet httpget = new HttpGet(serverUrl);
//
//        System.out.println( serverUrl );
//
//        //System.out.println( httpget.toString() );
//
//        HttpResponse response = null;
//
//        // Execute the request
//        try {
//
//            // Execute HTTP Post Request
//            response = httpclient.execute(httpget);
//
//
//        } catch (Exception e) {
//            System.out.println(e.toString());
//        }
//
//        if ( response != null ){
//
//            /* put the response to a string */
//            HttpEntity responseEntity = response.getEntity();
//            String responseString = EntityUtils.toString(responseEntity, String.valueOf(responseEntity.getContentEncoding( )));
//            System.out.println(responseString);
//
//        } else {
//
//            throw new IllegalStateException( "server did not respond to urlDownloadFile request." );
//        }
//
//        return "";
//
//
//
//    }

    public static String post(String url, HashMap<String, String> data, MyDbWrapper db) throws IOException {


        // init client
        httpclient = new DefaultHttpClient();

        // Prepare a request object
        HttpPost httppost = new HttpPost(url);
        HttpResponse response = null;

        // Execute the request
        try {


            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            /* turn hashmap into name value pairs */
            for (String key : new ArrayList<String> ( data.keySet() ) ){
                nameValuePairs.add(new BasicNameValuePair(key, data.get(key) ) );
            }

            /* store message body in post request */
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            response = httpclient.execute(httppost);

        } catch (Exception e) {
            Log.d(TAG, e.toString());

        }

        /* attempt to read response */
        if ( response != null) {
            try {
                HttpEntity responseEntity = response.getEntity();
                Log.d(TAG, "content encoding: " + responseEntity.getContentEncoding());
                //String contentEncoding = String.valueOf(responseEntity.getContentEncoding();
                String contentEncoding = "UTF-8";
                String responseString = EntityUtils.toString(responseEntity, contentEncoding);
                Log.d(TAG, "response string: " + responseString);

                /* cull out uuid */
                responseString = responseString.substring(responseString.indexOf('{'), responseString.indexOf('}'));
                /* format of response is {added:<uuid>}..<otherstuff>, and we just want the uuid */
                if (responseString.contains("{added:")) {
                    responseString = responseString.replace("{added:", "");
                    responseString = responseString.replace("}", "");
                    responseString = responseString.trim();
                }
                else if (responseString.contains("{already have:")) {
                    responseString = responseString.replace("{already have:", "");
                    responseString = responseString.replace("}", "");
                    responseString = responseString.trim();
                }

                interpretResponse( responseString, db );
                return responseString;


            } catch (Exception e){
                Log.e(TAG, "failed to decode response body: " + e.toString());

            }
            Log.d(TAG, "response status: " + response.getStatusLine().toString());
            Log.d(TAG, "response locale: " + response.getLocale());


        } else {
            Log.d(TAG, "response was null");

        }
        return ERROR_RESPONSE_READ;
    }

    /** interpretResponse
     *      if the server returns a uuid, this function will remove that record from the db
     * @param response the response string
     * @param db the db
     */
    private static void interpretResponse(String response, MyDbWrapper db){
        if (!(response.equals("") || response.equals(ServerRequestor.ERROR_RESPONSE_READ)))
            db.removeRecord(response);
//        Log.d("Server", "compare uuids: " + uuid.equals(response) + " original:|" + uuid
//                + "|final:|" + response + "|");
    }



}
