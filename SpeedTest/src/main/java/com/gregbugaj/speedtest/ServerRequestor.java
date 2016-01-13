package com.gregbugaj.speedtest;

import android.util.Log;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    /**getDownloadUrl
     *
     * @param url the url of the server
     * @return the url of the file for speedtest to download
     * @throws IOException
     */

    public static String getDownloadUrl( String url ) throws IOException {

        // init client
        httpclient = new DefaultHttpClient();

        /* store your params as name value pairs */
        List<NameValuePair> params = new ArrayList<NameValuePair>();

        /* Add in requests */
        params.add(new BasicNameValuePair("request", "download_file_url" ) );

        String paramString = URLEncodedUtils.format(params, "utf-8");

        url += "?" + paramString;

        // Prepare a request object
        HttpGet httpget = new HttpGet(url);

        System.out.println( url );

        //System.out.println( httpget.toString() );

        HttpResponse response = null;

        // Execute the request
        try {

            // Execute HTTP Post Request
            response = httpclient.execute(httpget);


        } catch (Exception e) {
            System.out.println(e.toString());
        }

        if ( response != null ){

            /* put the response to a string */
            HttpEntity responseEntity = response.getEntity();
            String responseString = EntityUtils.toString(responseEntity, String.valueOf(responseEntity.getContentEncoding( )));
            System.out.println(responseString);

        } else {

            throw new IllegalStateException( "server did not respond to urlDownloadFile request." );
        }

        return "";



    }

    public static void post(String url, HashMap<String, String> data) throws IOException {

        // init client
        httpclient = new DefaultHttpClient();

        // Prepare a request object
        HttpPost httppost = new HttpPost(url);

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
            HttpResponse response = httpclient.execute(httppost);

        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }


}
