package tw.nekomimi.nekogram;

import android.os.AsyncTask;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;

public class RegDate {
    private static final HashMap<Integer, Integer> map = new HashMap<>();

    public static int getRegDate(int userId) {
        Integer regDate = map.get(userId);
        return regDate == null ? 0 : regDate;
    }

    public static void getRegDate(int userId, RegDateCallback callback) {
        new MyAsyncTask().request(userId, callback).execute();
    }

    @SuppressWarnings("deprecation")
    private static class MyAsyncTask extends AsyncTask<Void, Integer, Object> {
        RegDateCallback callback;
        int userId;

        public MyAsyncTask request(int userId, RegDateCallback callback) {
            this.userId = userId;
            this.callback = callback;
            return this;
        }

        @Override
        protected Object doInBackground(Void... params) {
            try {
                return requestRegDate(userId);
            } catch (Throwable e) {
                e.printStackTrace();
                FileLog.e(e);
                return e;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result == null) {
                callback.onError(null);
            } else if (result instanceof Exception) {
                callback.onError((Exception) result);
            } else {
                map.put(userId, (Integer) result);
                callback.onSuccess((Integer) result);
            }
        }

    }

    public static String getDCLocation(int dc) {
        switch (dc) {
            case 1:
            case 3:
                return "Miami";
            case 2:
            case 4:
                return "Amsterdam";
            case 5:
                return "Singapore";
            default:
                return "Unknown";
        }
    }

    public interface RegDateCallback {
        void onSuccess(int regDate);

        void onError(Exception e);
    }

    private static int requestRegDate(int userId) throws IOException, JSONException, NumberFormatException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user_id", userId);
        jsonObject.put("owner", userId);
        jsonObject.put("data", BuildConfig.DATA);
        String response = request(jsonObject.toString());
        if (TextUtils.isEmpty(response)) {
            return 0;
        }
        return Integer.parseInt(response);
    }

    private static String request(String param) throws IOException {
        ByteArrayOutputStream outbuf;
        InputStream httpConnectionStream;
        URL downloadUrl = new URL(BuildConfig.ENDPOINT);
        HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
        httpConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        httpConnection.addRequestProperty("User-Agent", "Nicegram/7.4.2 CFNetwork/1191.2 Darwin/20.0.0");
        httpConnection.setConnectTimeout(1000);
        //httpConnection.setReadTimeout(2000);
        httpConnection.setRequestMethod("POST");
        httpConnection.setDoOutput(true);
        DataOutputStream dataOutputStream = new DataOutputStream(httpConnection.getOutputStream());
        byte[] t = param.getBytes(Charset.defaultCharset());
        dataOutputStream.write(t);
        dataOutputStream.flush();
        dataOutputStream.close();
        httpConnection.connect();
        if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            httpConnectionStream = httpConnection.getErrorStream();
        } else {
            httpConnectionStream = httpConnection.getInputStream();
        }
        outbuf = new ByteArrayOutputStream();

        byte[] data = new byte[1024 * 32];
        while (true) {
            int read = httpConnectionStream.read(data);
            if (read > 0) {
                outbuf.write(data, 0, read);
            } else if (read == -1) {
                break;
            } else {
                break;
            }
        }
        String result = outbuf.toString();
        httpConnectionStream.close();
        outbuf.close();
        return result;
    }

}
