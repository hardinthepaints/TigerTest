package com.xanderfehsenfeld.tigertest;

import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;

/**
 * An internet speed test which downloads a file and measures how long it takes,
 * senddin out periodic messages to SpeedTestLauncher
 *
 * Once the tester has started, it either stops when the file is downloaded,
 * or when its thread is interrupted
 *
 *
 */
class SpeedTester implements Runnable {

    private static final double BYTE_TO_KILOBIT = 0.0078125;
    private static final double KILOBIT_TO_MEGABIT = 0.0009765625;

    public static final String TAG_WORKER = "WORKER";

    private SpeedTestLauncher speedTestLauncher;

    public SpeedTester(SpeedTestLauncher speedTestLauncher) {
        this.speedTestLauncher = speedTestLauncher;
    }

    @Override
    public void run() {
        InputStream stream = null;
        try {
            int bytesIn = 0;

            long startCon = System.currentTimeMillis();

            /* the file to be downloaded */
            URL url = new URL(speedTestLauncher.download_file_url);
            URLConnection con = url.openConnection();
            con.setUseCaches(false);
            long connectionLatency = System.currentTimeMillis() - startCon;
            stream = con.getInputStream();

            SpeedTestLauncher.EXPECTED_SIZE_IN_BYTES = con.getContentLength();

            Message msgUpdateConnection = Message.obtain(speedTestLauncher.mHandler, speedTestLauncher.MSG_UPDATE_CONNECTION_TIME);
            msgUpdateConnection.arg1 = (int) connectionLatency;
            speedTestLauncher.mHandler.sendMessage(msgUpdateConnection);

            long start = System.currentTimeMillis();
            int currentByte = 0;
            long updateStart = System.currentTimeMillis();
            long updateDelta = 0;
            int bytesInThreshold = 0;


            /* get max bytes to read
            * There may not be this amount of bytes available to read there
            * are rarely more available
            */
            int bytesToRead = 1024;
            byte[] readBytes = new byte[bytesToRead];

            while ((currentByte = stream.read(readBytes)) != -1) {

                if (Thread.interrupted()) {
                    Log.d(TAG_WORKER, "thread interupted. returning.");
                    /* send message complete even though its not */
                    Long downloadTime = (System.currentTimeMillis() - start);
                    Message msg = Message.obtain(speedTestLauncher.mHandler, speedTestLauncher.MSG_COMPLETE_STATUS, calculate(downloadTime, bytesIn));
                    msg.arg1 = bytesIn;

                    /* communicate this is not complete */
                    msg.arg2 = 0;
                    speedTestLauncher.mHandler.sendMessage(msg);

                    return;
                }

                //bytesIn++;
                bytesIn += currentByte;
                bytesInThreshold += currentByte;
                if (updateDelta >= SpeedTestLauncher.UPDATE_THRESHOLD) {
                    int progress = (int) ((bytesIn / (double) SpeedTestLauncher.EXPECTED_SIZE_IN_BYTES) * 100);
                    Message msg = Message.obtain(speedTestLauncher.mHandler, speedTestLauncher.MSG_UPDATE_STATUS, calculate(updateDelta, bytesInThreshold));
                    msg.arg1 = progress;
                    msg.arg2 = bytesIn;
                    speedTestLauncher.mHandler.sendMessage(msg);
                    //Reset
                    updateStart = System.currentTimeMillis();
                    bytesInThreshold = 0;

                }
                /* reassign vars */
                bytesToRead = Math.max(stream.available(), 1024);
                readBytes = new byte[bytesToRead];
                updateDelta = System.currentTimeMillis() - updateStart;
            }

            long downloadTime = (System.currentTimeMillis() - start);

            speedTestLauncher.mCountDownTimer.cancel();


            //Prevent ArithmeticException
            if (downloadTime == 0) {
                downloadTime = 1;
            }

            Message msg = Message.obtain(speedTestLauncher.mHandler, speedTestLauncher.MSG_COMPLETE_STATUS, calculate(downloadTime, bytesIn));
            msg.arg2 = 1;
            msg.arg1 = bytesIn;
            speedTestLauncher.mHandler.sendMessage(msg);
        } catch (MalformedURLException e) {

            Log.e(TAG_WORKER, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG_WORKER, e.getMessage());

        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                //Suppressed
            }
        }
    }

    /**
     *
     * 1 byte = 0.0078125 kilobits
     * 1 kilobits = 0.0009765625 megabit
     *
     * @param downloadTime in miliseconds
     * @param bytesIn number of bytes downloaded
     * @return SpeedInfo containing current speed
     */
    private static SpeedInfo calculate(final long downloadTime, final long bytesIn){
        SpeedInfo info=new SpeedInfo();
        //from mil to sec
        long bytespersecond;
        if (!(downloadTime == 0)) {
            bytespersecond = (bytesIn / downloadTime) * 1000;
        } else {
            bytespersecond = -1;
        }
        double kilobits=bytespersecond * BYTE_TO_KILOBIT;
        double megabits=kilobits  * KILOBIT_TO_MEGABIT;
        info.downspeed=bytespersecond;
        info.kilobits=kilobits;
        info.megabits=megabits;

        return info;
    }

    /**
     * Transfer Object
     * @author devil
     *
     */
    public static class SpeedInfo{
        public double kilobits=0;
        public double megabits=0;
        public double downspeed=0;
    }
}
