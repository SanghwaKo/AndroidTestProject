package mobiletest.smaato.com.smaatoproject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kshtest.testlib.DataFromWeb;
import com.kshtest.testlib.MainClass;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    // static String values
    private static final String TAG_IMG = "img";
    private static final String TAG_TEXT = "text";

    //Layouts
    private LinearLayout mNormalLayout;
    private LinearLayout mNoResultLayout;

    // UI Components
    private ImageView mImgView;
    private TextView mTxtView;
    private TextView mCreatedTxtView;
    private TextView mUserNameTxtView;
    private TextView mUserCountryTxtView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mNormalLayout = (LinearLayout)findViewById(R.id.layout_with_data);
        mNoResultLayout = (LinearLayout)findViewById(R.id.layout_wo_data);

        mImgView = (ImageView)findViewById(R.id.data_img);
        mTxtView = (TextView)findViewById(R.id.data_text);
        mCreatedTxtView = (TextView)findViewById(R.id.data_created);
        mUserNameTxtView = (TextView)findViewById(R.id.data_user_name);
        mUserCountryTxtView = (TextView)findViewById(R.id.data_user_country);

        ConnectivityManager connManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        if(networkInfo != null && networkInfo.isConnected()){
            GettingDataThread gettingDataThread = new GettingDataThread(MainActivity.this);
            gettingDataThread.start();
        }else{
            // There is no active network connection.
            Toast.makeText(this, R.string.msg_warning_connection, Toast.LENGTH_LONG).show();

            mNormalLayout.setVisibility(View.GONE);
            mNoResultLayout.setVisibility(View.VISIBLE);
        }
    }

    private class GettingDataThread extends Thread{
        private ProgressDialog progressDialog;

        public GettingDataThread(Context context){
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(getString(R.string.msg_wait));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        public void run() {
            MainClass mainClass = new MainClass();
            boolean toBeContinued = true;

            while(toBeContinued){
                if(mainClass.getIsParsedData()){
                    toBeContinued = false;
                }else{
                    try{
                        Thread.sleep(1000);
                    }catch (Exception ex){
                        if(Debug.DEBUG){
                            Log.d(TAG, "Error in GettingDataThread " + ex.getMessage());
                        }
                    }
                }
            }

            progressDialog.dismiss();
            progressDialog = null;

            Message msg = handler.obtainMessage();
            msg.obj = mainClass.getOneData();
            handler.sendMessage(msg);
        }
    }

    final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.obj instanceof DataFromWeb){
                updateUI((DataFromWeb)msg.obj);
            }
        }
    };

    private void updateUI(DataFromWeb dataFromWeb){
        if(dataFromWeb == null){
            // Could not get data.
            mNormalLayout.setVisibility(View.GONE);
            mNoResultLayout.setVisibility(View.VISIBLE);
        }else{
            mNoResultLayout.setVisibility(View.GONE);
            mNormalLayout.setVisibility(View.VISIBLE);

            if(dataFromWeb.getType().equalsIgnoreCase(TAG_IMG)){
                mImgView.setVisibility(View.VISIBLE);
                mTxtView.setVisibility(View.GONE);

                LoadingImageTask imageLoading = new LoadingImageTask(mImgView);
                imageLoading.execute(dataFromWeb.getDetailData());
            } else if (dataFromWeb.getType().equalsIgnoreCase(TAG_TEXT)) {
                mImgView.setVisibility(View.GONE);
                mTxtView.setVisibility(View.VISIBLE);

                mTxtView.setText(dataFromWeb.getDetailData());
            }

            // Calculated created date.
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, dataFromWeb.getCreated());

            SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
            String date = format.format(calendar.getTime());

            mCreatedTxtView.setText(date);
            mUserNameTxtView.setText(dataFromWeb.getUserName());
            mUserCountryTxtView.setText(dataFromWeb.getUserCountry());
        }
    }

    // Loading the image from the url and set it in the ImageView.
    private class LoadingImageTask extends AsyncTask<String, Void, Bitmap>{
        private ImageView imageView;

        public LoadingImageTask(ImageView imageView){
            this.imageView = imageView;
        }

        protected Bitmap doInBackground(String... urls){
            String urlOfImage = urls[0];
            Bitmap bitmap = null;

            try{
                InputStream is = new URL(urlOfImage).openStream();
                bitmap = BitmapFactory.decodeStream(is);
            }catch (Exception ex){
                if(Debug.DEBUG){
                    Log.d(TAG, "Error in LoadingImageTask " + ex.getMessage());
                }
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap result){
            imageView.setImageBitmap(result);
        }
    }
}
