package com.example.conanxjp.brainet;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class DBActivity extends AppCompatActivity {

    /*--------------------------
     Constant fields
    ---------------------------*/
    private static final String dbFileName = "brianet_db_group3";
    private static final String recordTableName = "brainwaverecord";
    private static final String userTableName = "brainwaveuser";
    private static final String ADMIN = "Admin";
    private static final String DATAPATH = Environment.getExternalStorageDirectory().toString() + "/BraiNet_Group3";
    private static final int DATASIZE = 1000;
    private static final int XPADDING = 100;
    private static final int XMINRANGE = 0;
    private static final int XMAXRANGE = DATASIZE + XPADDING;
    private static final int YMAX = 1000;
    private static final int YPADDING = 500;
    private static final int YMINRANGE = 0;
    private static final int YMAXRANGE = YMAX + YPADDING;
    private static final int SCARSE = 200;
    private static final int DELAY = 2;
    private static final String DATEFORMAT = "yyyy-MM-dd";
    private static final String serverUri = "https://impact.asu.edu/CSE535Spring17Folder/UploadToServer.php";
    private static final String downloadUri = "https://impact.asu.edu/CSE535Spring17Folder/";
    ProgressDialog dialog;

    /*--------------------------
     Database and schema fields
    ---------------------------*/
    private int sessionId;
    private String pUser = "";
    private String date = "";
    private String pName = "";
    private String pAge = "";
    private String pGender = "";
    private float compressRatio;
    private int maxIndex;
    private int index = 0;
    private Random rng = new Random();
    private int[] dataInt = new int[DATASIZE];
    private byte[] dataByte = new byte[DATASIZE];
    private SQLiteDatabase db;
    private CollectData dataCollection;

    /*--------------------------
     Text fields
    ---------------------------*/
    private EditText userNameTxt;
    private EditText nameTxt;
    private EditText ageTxt;
    private RadioGroup genderRadioGrp;
    private RadioButton genderRadionBtn;
    private TextView sessionIdLbl;
    private TextView userNameLbl;
    private TextView nameLbl;
    private TextView ageLbl;
    private TextView genderLbl;
    private TextView dateLbl;
    private TextView messageLbl;

    /*--------------------------
     Real-time graph fields
    ---------------------------*/
    private GraphView realTimeGraph;
    private LineGraphSeries<DataPoint> realTimeSeries;
    private Handler realTimeHandler = new Handler();

    /*--------------------------
     Button fields
    ---------------------------*/
    private Button startBtn;
    private Button stopBtn;
    private Button uploadBtn;
    private Button viewHistoryBtn;

    /*--------------------------------------------
      fields related to the write permission on
      external storage
     --------------------------------------------*/
    private static String TAG = "PermissionDemo";
    private static final int REQUEST_WRITE_STORAGE = 112;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db);

        userNameLbl = (TextView) findViewById(R.id.spUser_lbl);
        sessionIdLbl = (TextView) findViewById(R.id.sSessionID_lbl);
        nameLbl = (TextView) findViewById(R.id.spName_lbl);
        ageLbl = (TextView) findViewById(R.id.spAge_lbl);
        genderLbl = (TextView) findViewById(R.id.spGender_lbl);
        dateLbl = (TextView) findViewById(R.id.sDate_lbl);
        messageLbl = (TextView) findViewById(R.id.msg_lbl);
        userNameTxt = (EditText) findViewById(R.id.pUser_txt);
        nameTxt = (EditText) findViewById(R.id.pName_txt);
        ageTxt = (EditText) findViewById(R.id.pAge_txt);
        genderRadioGrp = (RadioGroup) findViewById(R.id.pGender_rgrp);
        genderRadionBtn = (RadioButton) findViewById(genderRadioGrp.getCheckedRadioButtonId());

        startBtn = (Button) findViewById(R.id.start_btn);
        stopBtn = (Button) findViewById(R.id.stop_btn);
        uploadBtn = (Button) findViewById(R.id.upload_btn);
        viewHistoryBtn = (Button) findViewById(R.id.history_btn);
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateSessionInformation();

        userNameTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                updateSessionInformation();
                return false;
            }
        });

        nameTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                updateSessionInformation();
                return false;
            }
        });

        ageTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                updateSessionInformation();
                return false;
            }
        });

        genderRadioGrp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {

                genderRadionBtn = (RadioButton) findViewById(genderRadioGrp.getCheckedRadioButtonId());
                pGender = genderRadionBtn.getText().toString();
                updateSessionInformation();
            }
        });


        /*--------------------------
         Real Time Graph Settings
        ---------------------------*/
        realTimeGraph = (GraphView) findViewById(R.id.realtime_graph);
        // set graph parameters
        realTimeGraph.setTitle("Real Time Graph");
        realTimeGraph.setTitleTextSize((float) 50.0);
        realTimeGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time (ms)");
        realTimeGraph.getGridLabelRenderer().setVerticalAxisTitle("Intensity");

        // set viewPort of the graph
        Viewport viewPort = realTimeGraph.getViewport();
        viewPort.setXAxisBoundsManual(true);
        viewPort.setMaxX(XMAXRANGE);
        viewPort.setMinX(XMINRANGE);

        viewPort.setYAxisBoundsManual(true);
        viewPort.setMaxY(YMAXRANGE);
        viewPort.setMinY(YMINRANGE);

        realTimeSeries = new LineGraphSeries<DataPoint>();
        realTimeGraph.addSeries(realTimeSeries);


        /*-----------------------------------------------------
          Request user input to grant write permission on the
          external storage
        ------------------------------------------------------*/
        int permission = ContextCompat.checkSelfPermission(DBActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(DBActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DBActivity.this);
                builder.setMessage("Permission to access the SD-CARD is required for this app to Download PDF.").setTitle("Permission required");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        makeRequest();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();

            } else {
                makeRequest();
            }
        }

        /*-----------------------------------------------------
          End of permission request, if permission is granted
          then proceed to create the database file and table
         ------------------------------------------------------*/
        if (permission == PackageManager.PERMISSION_GRANTED)
        {
            File dbDir = new File(DATAPATH);
            if (!dbDir.exists())
            {
                dbDir.mkdirs();
            }
            db = SQLiteDatabase.openOrCreateDatabase(dbDir + "/" + dbFileName, null);

            db.beginTransaction();
            try
            {
                // db.execSQL("drop table " + userTableName);    // only for testing purpose
                // create the table using the table name
                db.execSQL("CREATE TABLE IF NOT EXISTS "
                        + userTableName
                        + " ( username text PRIMARY KEY, "
                        + " name text, "
                        + " age integer, "
                        + " gender text "
                        + "); " );
                db.setTransactionSuccessful();;
            }
            catch (SQLiteException e) {
                //report problem
                Log.e("Error in transactions", e.getMessage());
            } finally {
                db.endTransaction();
            }

            db.beginTransaction();
            try
            {
                // db.execSQL("drop table " + recordTableName);    // only for testing purpose
                // create the table using the table name
                db.execSQL("CREATE TABLE IF NOT EXISTS "
                        + recordTableName
                        + " ( sessionID integer PRIMARY KEY AUTOINCREMENT, "
                        + " date text, "
                        + " username text, "
                        + " ratio real, "
                        + " data blob, "
                        + " FOREIGN KEY ( username ) REFERENCES "
                        + userTableName
                        + "( username )"
                        + "); " );
                db.setTransactionSuccessful();;
            }
            catch (SQLiteException e) {
                //report problem
                Log.e("Error in transactions", e.getMessage());
            } finally {
                db.endTransaction();
            }
        }

         /*---------------------------------------------------
           Start Button setonclicklistener, to start collecting
           the data and plot it in the graph.
         -----------------------------------------------------*/
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                db.beginTransaction();
                try {

                    String query = "select count(*) from " + userTableName
                            + " where username = ?";
                    Cursor cursor = db.rawQuery(query, new String[] {pUser});

                    String insertQuery;
                    if (cursor.moveToFirst())
                    {
                        if (cursor.getInt(0) > 0)
                        {
                            Log.i("Cannot insert: ", "the username " + userNameTxt.getText() + " exists.");
                        }
                        else
                        {
                            insertQuery = "insert into " + userTableName +
                                    "( username, name, age, gender) values ('"
                                    + pUser + "', '" + pName + "', '"
                                    + pAge + "', '" + pGender + "');";
                            db.execSQL(insertQuery);
                        }
                    }
                    cursor.close();
                    db.setTransactionSuccessful();
                } catch (SQLiteException e) {
                    //report problem
                    Log.e("Error in transactions", e.getMessage());
                } finally {

                    db.endTransaction();
                    //db.close();
                }

                dataCollection = new CollectData(DBActivity.this);
                //Toast.makeText(this, "Collection Started!", Toast.LENGTH_LONG).show();
                dataCollection.execute("");
            }
        });

        /*--------------------------------------
          stop button will stop collecting data
          and stop the real-time plot and not
          saving any data.
         ---------------------------------------*/
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dataCollection != null)
                {
                    dataCollection.cancel(true);
                    realTimeHandler.removeCallbacks(plotRunnable);
                }
            }
        });

        viewHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent historyIntent = new Intent(DBActivity.this, DBActivity_User.class);

                historyIntent.putExtra("userName", pUser);
                historyIntent.putExtra("name", pName);
                startActivity(historyIntent);
            }
        });

         /*-----------------------------------------------------
          Upload button will stop the service and upload the
          database file onto the server
        ------------------------------------------------------*/
        // upload button click action
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog = ProgressDialog.show(DBActivity.this,"","Uploading File..." + dbFileName,true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        uploadFile(DATAPATH + "/" + dbFileName);
                    }
                }).start();

            }
        });

    }

    private class CollectData extends AsyncTask<String, Integer, String>
    {
        private Context context;

        public CollectData(Context context)
        {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            realTimeGraph.addSeries(realTimeSeries);
            realTimeHandler.postDelayed(plotRunnable, DELAY);
            db.beginTransaction();
            try
            {
                // select record from the record table
                String selectQuery = "select * from " + recordTableName
                        + " where username = '" + pUser + "' order by sessionID DESC ";

                Cursor cursor = db.rawQuery(selectQuery, null);
                if (cursor.moveToFirst())
                {
                    sessionId = cursor.getInt(cursor.getColumnIndex("sessionID")) + 1;
                }
                sessionIdLbl.setText("Session ID: " + Integer.toString(sessionId));
                cursor.close();
                db.setTransactionSuccessful();
            }
            catch (SQLiteException e) {
                //report problem
                Log.e("Error in transactions", e.getMessage());
            } finally {
                db.endTransaction();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            index = 0;
            while (index != DATASIZE)
            {
                try {
                    // delay by 2 ms to simulate the signal collection
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                generateData();
                // if canceled, stop generating data
                if (isCancelled())
                {
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            compressInt();

            ContentValues cv = new ContentValues();
            cv.put("date", date);
            cv.put("username", pUser);
            cv.put("ratio", compressRatio);
            cv.put("data", dataByte);
            try
            {
                db.insert(recordTableName, null, cv);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }

    /*--------------------------
      All helper functions
    ---------------------------*/
    private void updateSessionInformation()
    {
        /*--------------------------
         Set session information
        ---------------------------*/
        pName = nameTxt.getText().toString();
        pUser = userNameTxt.getText().toString();
        pAge = ageTxt.getText().toString();
        pGender = genderRadionBtn.getText().toString();
        messageLbl.setText("Welcome, " + pName + "!");
        userNameLbl.setText("Username: " + pUser);
        nameLbl.setText("Name: " + pName);
        ageLbl.setText("Age: " + pAge);

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat(DATEFORMAT);
        String formattedDate = df.format(c.getTime());
        date = formattedDate;

        //date = "2017-07-02";
        dateLbl.setText("Date: " + date);
        genderLbl.setText("Gender: " + pGender);
    }



    private void generateData()
    {
        // randomize data generation to simulate the EEG signal
        dataInt[index ++] = (rng.nextInt((YMAX - YMINRANGE) + 1) + YMINRANGE) / (rng.nextInt((rng.nextInt(SCARSE)) + 1) + 1) ;
    }


    private int Max(int[] data)
    {
        int max = 0;
        for (int i = 0; i < DATASIZE; i ++)
        {
            if (max < data[i])
            {
                max = data[i];
                maxIndex = i;
            }
        }
        return max;
    }

    private void compressInt ()
    {
        int max = Max(dataInt);
        for (int i = 0; i < DATASIZE; i ++)
        {
            double b = (((double)(dataInt[i] - YMINRANGE) / (double)(max - YMINRANGE)) * Byte.MAX_VALUE);
            dataByte[i] = (byte)b;
        }
        compressRatio = (float)max/dataByte[maxIndex];
    }

    private void deCompressByte (int[] intArray, byte[] byteArray, float ratio)
    {
        for (int i = 0; i < DATASIZE; i ++)
        {
            intArray[i] = (int) (byteArray[i] * ratio);
        }

    }


    private DataPoint[] generateDataPoint(int[] dataArray, Integer range)
    {
        int size = 0;
        if (range == null)
        {
            size = index;
        }
        else
        {
            size = range;
        }
        DataPoint[] values = new DataPoint[size];
        for (int i = 0; i < size; i ++) {
            int x = i + 1;
            DataPoint v = new DataPoint(x, dataArray[i]);
            values[i] = v;
        }
        return values;
    }

    // plot function
    Runnable plotRunnable = new Runnable() {
        @Override
        public void run() {
            // recursive call
            realTimeSeries.resetData(generateDataPoint(dataInt, null));
            // delay by 2 ms to simulate the signal collection
            realTimeHandler.postDelayed(this, DELAY);
        }
    };

    /*-----------------------------------------------------
      The following uploadFile method is mainly cited from
      the following source:
      http://www.coderefer.com/android-upload-file-to-server/
      The method of uploading any file to a server using
      php script is pretty standard, so I used this method
      almost untouched from the source. In addtion, only
      a snippet of code of the example code of AppDownloader
      is used to set the certification.
    ------------------------------------------------------*/

    public int uploadFile(String selectedFilePath) {

        int serverResponseCode = 0;

        HttpsURLConnection connection;
        DataOutputStream dataOutputStream;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";


        int bytesRead,bytesAvailable,bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File selectedFile = new File(selectedFilePath);

        /*-----------------------------------------------------
          The following is the snippet of code of the example
          code of AppDownloader used to set the certification.
        ------------------------------------------------------*/
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }
        } };

        try {
            SSLContext sc = SSLContext.getInstance("TLS");

            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        if (!selectedFile.isFile()){
            dialog.dismiss();

            /*
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvFileName.setText("Source File Doesn't Exist: " + selectedFilePath);
                }
            });
            */
            return 0;
        }else{
            try{
                FileInputStream fileInputStream = new FileInputStream(selectedFile);
                URL url = new URL(serverUri);
                connection = (HttpsURLConnection) url.openConnection();
                connection.setDoInput(true);//Allow Inputs
                connection.setDoOutput(true);//Allow Outputs
                connection.setUseCaches(false);//Don't use a cached Copy
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                connection.setRequestProperty("uploaded_file", selectedFilePath);


                //creating new dataoutputstream
                dataOutputStream = new DataOutputStream(connection.getOutputStream());

                //writing bytes to data outputstream
                dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
                dataOutputStream.writeBytes("Content-Disposition: form-data; name=uploaded_file;" + "filename="
                        + selectedFilePath + " " + lineEnd);

                dataOutputStream.writeBytes(lineEnd);

                //returns no. of bytes present in fileInputStream
                bytesAvailable = fileInputStream.available();
                //selecting the buffer size as minimum of available bytes or 1 MB
                bufferSize = Math.min(bytesAvailable,maxBufferSize);
                //setting the buffer as byte array of size of bufferSize
                buffer = new byte[bufferSize];

                //reads bytes from FileInputStream(from 0th index of buffer to buffersize)
                bytesRead = fileInputStream.read(buffer,0,bufferSize);

                //loop repeats till bytesRead = -1, i.e., no bytes are left to read
                while (bytesRead > 0){
                    //write the bytes read from inputstream
                    dataOutputStream.write(buffer,0,bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable,maxBufferSize);
                    bytesRead = fileInputStream.read(buffer,0,bufferSize);
                }

                dataOutputStream.writeBytes(lineEnd);
                dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                serverResponseCode = connection.getResponseCode();
                String serverResponseMessage = connection.getResponseMessage();
                InputStream is = connection.getInputStream();

                Log.i(TAG, "Server Response is: " + serverResponseMessage + ": " + serverResponseCode);

                //response code of 200 indicates the server status OK
                if(serverResponseCode == 200){

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"), 8192);
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    String result = sb.toString();
                    Log.i(TAG, result);
                    /*
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvFileName.setText("File Upload completed.\n\n You can see the uploaded file here: \n\n" + "http://coderefer.com/extras/uploads/"+ fileName);
                        }
                    });
                    */
                }

                //closing the input and output streams
                fileInputStream.close();
                dataOutputStream.flush();
                dataOutputStream.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(DBActivity.this,"File Not Found",Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Toast.makeText(DBActivity.this, "URL error!", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(DBActivity.this, "Cannot Read/Write File!", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
            return serverResponseCode;
        }

    }

    protected void makeRequest ()
    {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                return;
            }
        }
    }

}
