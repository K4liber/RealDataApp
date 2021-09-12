package com.example.realdata.sender;

import android.util.Log;

import com.example.realdata.utils.Config;
import com.example.realdata.utils.State;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class ViewSender implements Runnable {
    private static final String tag = "ViewSender";

    @Override
    public void run() {
        Log.d(ViewSender.tag, "run");
        this.sendView();
    }

    private void sendView() {
        try {
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1024 * 1024;
            //todo change URL as per client ( MOST IMPORTANT )
            URL url = new URL(State.serverURL + "/view");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Allow Inputs &amp; Outputs.
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // Set HTTP method to POST.
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            FileInputStream fileInputStream;
            DataOutputStream outputStream;
            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            HashMap<String, String> values = new HashMap<>();
            values.put("device_id", State.deviceId);
            values.put("secret_key", State.secretKey);

            for(Map.Entry<String, String> entry : values.entrySet()){
                outputStream.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + lineEnd);
                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(URLEncoder.encode(entry.getValue(), "UTF-8"));
                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            }

            outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\""
                    + State.deviceId + ".jpeg\"" + lineEnd);
            outputStream.writeBytes("Content-Type: image/jpeg" + lineEnd);
            outputStream.writeBytes(lineEnd);

            fileInputStream = new FileInputStream(Config.tempImg);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // Read file
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                outputStream.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            int serverResponseCode = connection.getResponseCode();
            String result = null;

            if (serverResponseCode == 200) {

            } else {
                Log.d(tag, String.valueOf(serverResponseCode));
            }

            StringBuilder s_buffer = new StringBuilder();
            InputStream is = new BufferedInputStream(connection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                s_buffer.append(inputLine);
            }
            result = s_buffer.toString();
            fileInputStream.close();
            outputStream.flush();
            outputStream.close();

            if (result != null) {
                Log.d("result_for upload", result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
