package ucanthide.main.utils_static;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import ucanthide.main.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class Utils {
    public static int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if (k==0) return 1;
        else return k;
    }

    public static HttpsURLConnection getConnection(
            String endpoint, String method) throws IOException {
        URL urlPost = new URL(Config.serverURL + endpoint);
        HttpsURLConnection httpsURLConnection =
                (HttpsURLConnection) urlPost.openConnection();
        httpsURLConnection.setSSLSocketFactory(State.getSSLContext().getSocketFactory());
        httpsURLConnection.setRequestMethod(method);
        httpsURLConnection.setDoInput(true);

        if (method.equals("POST")) {
            httpsURLConnection.setDoOutput(true);
        }

        return httpsURLConnection;
    }

    public static SSLContext getSSLContext() throws KeyStoreException,
            CertificateException, NoSuchAlgorithmException, IOException, KeyManagementException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final InputStream in = State.appContext.getResources().openRawResource(R.raw.cert);
        Certificate ca = cf.generateCertificate(in);
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext;
    }

    public static Bitmap getResizedView(Context context) throws IOException{
        InputStream input = context.getContentResolver().openInputStream(State.imageFileUri());
        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();

        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
            return null;
        }

        int originalSize = Math.max(onlyBoundsOptions.outHeight, onlyBoundsOptions.outWidth);
        double ratio = (originalSize > Config.imgMaxPixels)
                ? (originalSize / Config.imgMaxPixels) : 1.0;
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = Utils.getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither = true;
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;
        input = context.getContentResolver().openInputStream(State.imageFileUri());
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return bitmap;
    }

    public static Bitmap getLastView() {
        try {
            HttpsURLConnection httpsURLConnection =
                    Utils.getConnection("/view?device_id=" + State.deviceId, "GET");
            httpsURLConnection.setUseCaches(false);
            return BitmapFactory.decodeStream(httpsURLConnection.getInputStream());
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }

        return null;
    }
}
