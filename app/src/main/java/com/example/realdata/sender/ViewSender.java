package com.example.realdata.sender;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.realdata.R;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class ViewSender implements Runnable {
    private static final String tag = "ViewSender";

    @Override
    public void run() {
        Log.d(ViewSender.tag, "run()");
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
            URL urlPost = new URL(State.serverURL + "/view");
            HttpsURLConnection httpsURLConnection =
                    (HttpsURLConnection) urlPost.openConnection();
            httpsURLConnection.setSSLSocketFactory(State.sslContext.getSocketFactory());
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setUseCaches(false);
            httpsURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpsURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            FileInputStream fileInputStream;
            DataOutputStream outputStream;
            outputStream = new DataOutputStream(httpsURLConnection.getOutputStream());
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
            int serverResponseCode = httpsURLConnection.getResponseCode();
            String result = null;

            if (serverResponseCode == 200) {
                if (State.activityContext != null) {
                    TextView txtView = ((Activity) State.activityContext)
                            .findViewById(R.id.lastSend);
                    txtView.setText("Image successfully uploaded");
                }
            } else {
                if (State.activityContext != null) {
                    TextView errorView = ((Activity) State.activityContext).findViewById(R.id.error);
                    errorView.setText("Upload image error. Status = " + serverResponseCode);
                }
            }

            StringBuilder s_buffer = new StringBuilder();
            InputStream is = new BufferedInputStream(httpsURLConnection.getInputStream());
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
