package com.example.conanxjp.brainet;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class DBActivity_User extends AppCompatActivity {

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
    private static final String DATEFORMAT = "yyyy-mm-dd";
    private static final String downloadUri = "https://impact.asu.edu/CSE535Spring17Folder/";
    private static final String localDownloadFilePath = Environment.getExternalStorageDirectory().toString() + "/BraiNet_Group3";

    /*--------------------------
     Database and schema fields
    ---------------------------*/
    private boolean isAdmin;
    private int sessionId;
    private String pUser = "";
    private String date = "";
    private String pName = "";
    private String pAge = "";
    private String pGender = "";
    private float compressRatio;
    private int[] dataInt = new int[DATASIZE];
    private byte[] dataByte = new byte[DATASIZE];
    private SQLiteDatabase db;
    private Cursor userCursor;
    private Cursor recordCursor;
    ProgressDialog mProgressDialog;

    /*--------------------------
     Text fields
    ---------------------------*/
    private TextView welcomeMsgLbl;
    private EditText userNameTxt;
    private EditText dateTxt;
    private EditText sessionIdTxt;
    private TextView sDateLbl;
    private TextView sSessionIdLbl;
    private TextView spNameLbl;
    private TextView spUserLbl;
    private TextView spAgeLbl;
    private TextView spGenderLbl;

    /*--------------------------
     Button fields
    ---------------------------*/
    private Button searchBtn;
    private ImageButton preDateBtn;
    private ImageButton nextDateBtn;
    private ImageButton preSessionBtn;
    private ImageButton nextSessionBtn;
    private Button displayBtn;
    private Button downloadBtn;

    /*--------------------------
     History graph fields
    ---------------------------*/
    private GraphView historyGraph;
    private LineGraphSeries<DataPoint> historySeries = new LineGraphSeries<DataPoint>();;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_user);

        welcomeMsgLbl = (TextView) findViewById(R.id.msg_lbl);
        userNameTxt = (EditText) findViewById(R.id.pUser_txt);
        dateTxt = (EditText) findViewById(R.id.date_txt);
        sessionIdTxt = (EditText) findViewById(R.id.sessionId_txt);
        sDateLbl = (TextView) findViewById(R.id.sDate_lbl);
        sSessionIdLbl = (TextView) findViewById(R.id.sSessionID_lbl);
        spUserLbl = (TextView) findViewById(R.id.spUser_lbl);
        spNameLbl = (TextView) findViewById(R.id.spName_lbl);
        spAgeLbl = (TextView) findViewById(R.id.spAge_lbl);
        spGenderLbl = (TextView) findViewById(R.id.spGender_lbl);

        searchBtn = (Button) findViewById(R.id.searchUser_btn);
        preDateBtn = (ImageButton) findViewById(R.id.preDate_btn);
        nextDateBtn = (ImageButton) findViewById(R.id.nextDate_btn);
        preSessionBtn = (ImageButton) findViewById(R.id.preSession_btn);
        nextSessionBtn = (ImageButton) findViewById(R.id.nextSession_btn);
        downloadBtn = (Button) findViewById(R.id.download_btn);

         /*--------------------------
           History Graph Settings
         ---------------------------*/
        historyGraph = (GraphView) findViewById(R.id.history_graph);
        // set graph parameters
        historyGraph.setTitle("History Graph");
        historyGraph.setTitleTextSize((float) 50.0);
        historyGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time (ms)");
        historyGraph.getGridLabelRenderer().setVerticalAxisTitle("Intensity");

        // set viewPort of the graph
        Viewport viewPort = historyGraph.getViewport();
        viewPort.setXAxisBoundsManual(true);
        viewPort.setMaxX(XMAXRANGE);
        viewPort.setMinX(XMINRANGE);

        viewPort.setYAxisBoundsManual(true);
        viewPort.setMaxY(YMAXRANGE);
        viewPort.setMinY(YMINRANGE);

        mProgressDialog = new ProgressDialog(DBActivity_User.this);
        mProgressDialog.setMessage("A message");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);


        Intent historyIntent = getIntent();
        pName = historyIntent.getStringExtra("name");
        pUser = historyIntent.getStringExtra("userName");
        isAdmin = pUser.equals(ADMIN) ? true : false;

    }

    @Override
    protected void onStart() {
        super.onStart();

        File dbDir = new File(DATAPATH);
        db = SQLiteDatabase.openOrCreateDatabase(dbDir + "/" + dbFileName, null);

        if (!isAdmin)
        {
            userNameTxt.setEnabled(false);
            searchBtn.setEnabled(false);
            welcomeMsgLbl.setTextColor(Color.BLUE);

            welcomeMsgLbl.setText("Welcome " + pName);
            userNameTxt.setText(pUser);
            spUserLbl.setText("Username: " + pUser);
            spNameLbl.setText("Name: " + pName);

            db.beginTransaction();
            try
            {
                // select user information from the use info table
                String selectUserQuery = "select * from " + userTableName
                        + " where username = '" + pUser + "'";
                userCursor = db.rawQuery(selectUserQuery, null);
                if (userCursor.moveToFirst())
                {
                    pAge = Integer.toString(userCursor.getInt(userCursor.getColumnIndex("age")));
                    pGender = userCursor.getString(userCursor.getColumnIndex("gender"));
                }

                userCursor.close();
            }
            catch (SQLiteException e) {
                //report problem
                Log.e("Error in transactions", e.getMessage());
            } finally {
                db.endTransaction();
            }

            spAgeLbl.setText("Age: " + pAge);
            spGenderLbl.setText("Gender: " + pGender);

            db.beginTransaction();
            try
            {
                // select record from the record table

                String selectRecordQuery = "select * from " + recordTableName
                        + " where username = '" + pUser + "' order by sessionID DESC ";

                recordCursor = db.rawQuery(selectRecordQuery, null);
                if (recordCursor.moveToFirst())
                {
                    sessionId = recordCursor.getInt(recordCursor.getColumnIndex("sessionID"));
                    date = recordCursor.getString(recordCursor.getColumnIndex("date"));
                    dataByte = recordCursor.getBlob(recordCursor.getColumnIndex("data"));
                    compressRatio = recordCursor.getFloat(recordCursor.getColumnIndex("ratio"));
                    deCompressByte(dataInt, dataByte, compressRatio);
                    plotHistory();
                }

                //recordCursor.close();
                db.setTransactionSuccessful();
            }
            catch (SQLiteException e) {
                //report problem
                Log.e("Error in transactions", e.getMessage());
            } finally {
                db.endTransaction();
            }
        }
        else
        {
            /*----------------------------
             Enforce the order of workflow
            ------------------------------*/
            preDateBtn.setEnabled(false);
            preSessionBtn.setEnabled(false);
            nextDateBtn.setEnabled(false);
            nextSessionBtn.setEnabled(false);
            dateTxt.setEnabled(false);
            sessionIdTxt.setEnabled(false);
        }

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pUser = userNameTxt.getText().toString();
                if (!pUser.equals(""))
                {
                    db.beginTransaction();
                    try
                    {
                        // select user information from the use info table
                        String selectUserQuery = "select * from " + userTableName
                                + " where username = '" + pUser + "'";
                        userCursor = db.rawQuery(selectUserQuery, null);
                        if (userCursor.moveToFirst())
                        {
                            pAge = Integer.toString(userCursor.getInt(userCursor.getColumnIndex("age")));
                            pGender = userCursor.getString(userCursor.getColumnIndex("gender"));
                            pName = userCursor.getString(userCursor.getColumnIndex("name"));

                            spAgeLbl.setText("Age: " + pAge);
                            spGenderLbl.setText("Gender: " + pGender);
                            spNameLbl.setText("Name: " + pName);
                            spUserLbl.setText("Username: " + pUser);

                            db.beginTransaction();
                            try
                            {
                                // select record from the record table

                                String selectRecordQuery = "select * from " + recordTableName
                                        + " where username = '" + pUser + "' order by sessionID DESC ";

                                recordCursor = db.rawQuery(selectRecordQuery, null);
                                if (recordCursor.moveToFirst())
                                {
                                    sessionId = recordCursor.getInt(recordCursor.getColumnIndex("sessionID"));
                                    date = recordCursor.getString(recordCursor.getColumnIndex("date"));
                                    dataByte = recordCursor.getBlob(recordCursor.getColumnIndex("data"));
                                    compressRatio = recordCursor.getFloat(recordCursor.getColumnIndex("ratio"));
                                    deCompressByte(dataInt, dataByte, compressRatio);
                                    updateSessionInformation();
                                    plotHistory();
                                }

                                //recordCursor.close();
                                db.setTransactionSuccessful();
                            }
                            catch (SQLiteException e) {
                                //report problem
                                Log.e("Error in transactions", e.getMessage());
                            } finally {
                                db.endTransaction();
                            }
                        }
                        else
                        {
                            Toast.makeText(DBActivity_User.this, "Username: " + pUser + "cannot be found.", Toast.LENGTH_LONG).show();
                        }

                        userCursor.close();
                    }
                    catch (SQLiteException e) {
                        //report problem
                        Log.e("Error in transactions", e.getMessage());
                    } finally {
                        db.endTransaction();
                    }
                }
                preDateBtn.setEnabled(true);
                preSessionBtn.setEnabled(true);
                nextDateBtn.setEnabled(true);
                nextSessionBtn.setEnabled(true);
                dateTxt.setEnabled(true);
                sessionIdTxt.setEnabled(true);
            }
        });


        dateTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(DBActivity_User.this, dateCal, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        sessionIdTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditText)v).setSelection(0, sessionIdTxt.getText().length());
            }
        });
        sessionIdTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                sessionIdTxt.setText("Session ID: " + getSessionID());
                selectSessionIdQuery();
                return false;
            }
        });


        dateTxt.setText(date);
        sessionIdTxt.setText("Session ID: " + Integer.toString(sessionId));
        sSessionIdLbl.setText("Session ID: " + sessionId);
        sDateLbl.setText("Date: " + date);

        preDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanPreviousDate();
            }
        });

        nextDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanNextDate();
            }
        });

        preSessionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanPreviousSession();
            }
        });

        nextSessionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanNextSession();
            }
        });

        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final DownloadTask downloadTask = new DownloadTask(DBActivity_User.this);
                downloadTask.execute(downloadUri + "/" + dbFileName, dbFileName);

                mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        downloadTask.cancel(true);
                    }
                });
            }
        });

    }

    /*--------------------------
      All helper functions
    ---------------------------*/
    private void deCompressByte (int[] intArray, byte[] byteArray, float ratio)
    {
        for (int i = 0; i < DATASIZE; i ++)
        {
            intArray[i] = (int) (byteArray[i] * ratio);
        }

    }

    Calendar myCalendar = Calendar.getInstance();

    DatePickerDialog.OnDateSetListener dateCal = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            // TODO Auto-generated method stub
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel();
            selectDateQuery();
        }

    };

    private void updateLabel() {

        String myFormat = DATEFORMAT; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

        dateTxt.setText(sdf.format(myCalendar.getTime()));
    }

    private void updateSessionInformation()
    {
        sSessionIdLbl.setText("Session ID: " + sessionId);
        sDateLbl.setText("Date: " + date);
        dateTxt.setText(date);
        sessionIdTxt.setText("Session ID: " + Integer.toString(sessionId));
    }

    private void plotHistory ()
    {
        historySeries.resetData(generateDataPoint(dataInt));
        historyGraph.addSeries(historySeries);
    }

    private DataPoint[] generateDataPoint(int[] dataArray)
    {

        DataPoint[] values = new DataPoint[DATASIZE];
        for (int i = 0; i < DATASIZE; i ++) {
            int x = i + 1;
            DataPoint v = new DataPoint(x, dataArray[i]);
            values[i] = v;
        }
        return values;
    }

    private void selectDateQuery ()
    {
        String selectQueryDate = "select * from (select * from " + recordTableName
                + " where username = '" + pUser + "' order by sessionID DESC) where date >= '" + dateTxt.getText() + "'";
        Cursor cursor = db.rawQuery(selectQueryDate, null);
        if (cursor.moveToFirst())
        {
            sessionId = cursor.getInt(cursor.getColumnIndex("sessionID"));
            date = cursor.getString(cursor.getColumnIndex("date"));
            if (!date.equals(dateTxt.getText().toString()))
            {
                Toast.makeText(DBActivity_User.this, "Date: " + dateTxt.getText() + " is not found for " + pName + ", closest record is displayed", Toast.LENGTH_LONG).show();
            }
            dataByte = cursor.getBlob(cursor.getColumnIndex("data"));
            compressRatio = cursor.getFloat(cursor.getColumnIndex("ratio"));
            deCompressByte(dataInt, dataByte, compressRatio);
            updateSessionInformation();
            plotHistory();
        }
        else
        {
            selectQueryDate = "select * from (select * from " + recordTableName
                    + " where username = '" + pUser + "' order by sessionID ASC) where date < '" + dateTxt.getText() + "'";
            cursor = db.rawQuery(selectQueryDate, null);
            if (cursor.moveToFirst())
            {
                sessionId = cursor.getInt(cursor.getColumnIndex("sessionID"));
                date = cursor.getString(cursor.getColumnIndex("date"));
                if (!date.equals(dateTxt.getText().toString()))
                {
                    Toast.makeText(DBActivity_User.this, "Date: " + dateTxt.getText() + " is not found for " + pName + ", closest record is displayed", Toast.LENGTH_LONG).show();
                }
                dataByte = cursor.getBlob(cursor.getColumnIndex("data"));
                compressRatio = cursor.getFloat(cursor.getColumnIndex("ratio"));
                deCompressByte(dataInt, dataByte, compressRatio);
                updateSessionInformation();
                plotHistory();
            }
            else
            {
                Toast.makeText(DBActivity_User.this, "No Record for " + pName + " is found", Toast.LENGTH_LONG).show();
                dateTxt.setText(date);
            }

        }
        cursor.close();
        if (recordCursor.moveToFirst())
        {
            do {
                if (recordCursor.getInt(recordCursor.getColumnIndex("sessionID")) == sessionId)
                    break;
            } while (recordCursor.moveToNext());
        }
    }

    private void selectSessionIdQuery()
    {
        String selectQueryID = "select * from (select * from " + recordTableName
                + " where username = '" + pUser + "' order by sessionID DESC) where sessionID <= '" + getSessionID() + "'";
        Cursor cursor = db.rawQuery(selectQueryID, null);
        if (cursor.moveToFirst())
        {
            sessionId = cursor.getInt(cursor.getColumnIndex("sessionID"));
            if (!Integer.toString(sessionId).equals(getSessionID()))
            {
                Toast.makeText(DBActivity_User.this, "Session ID: " + getSessionID() + " is not found for " + pName + ", closest record is displayed", Toast.LENGTH_LONG).show();
            }
            date = cursor.getString(cursor.getColumnIndex("date"));
            dataByte = cursor.getBlob(cursor.getColumnIndex("data"));
            compressRatio = cursor.getFloat(cursor.getColumnIndex("ratio"));
            deCompressByte(dataInt, dataByte, compressRatio);
            updateSessionInformation();
            plotHistory();
        }
        else
        {
            selectQueryID = "select * from (select * from " + recordTableName
                    + " where username = '" + pUser + "' order by sessionID ASC) where sessionID > '" + getSessionID() + "'";
            cursor = db.rawQuery(selectQueryID, null);
            cursor.moveToFirst();
            sessionId = cursor.getInt(cursor.getColumnIndex("sessionID"));
            if (!Integer.toString(sessionId).equals(getSessionID()))
            {
                Toast.makeText(DBActivity_User.this, "Session ID: " + getSessionID() + " is not found for " + pName + ", closest record is displayed", Toast.LENGTH_LONG).show();
            }
            date = cursor.getString(cursor.getColumnIndex("date"));
            dataByte = cursor.getBlob(cursor.getColumnIndex("data"));
            compressRatio = cursor.getFloat(cursor.getColumnIndex("ratio"));
            deCompressByte(dataInt, dataByte, compressRatio);
            updateSessionInformation();
            plotHistory();

        }
        cursor.close();
        if (recordCursor.moveToFirst()) {
            do {
                if (recordCursor.getInt(recordCursor.getColumnIndex("sessionID")) == sessionId)
                    break;
            } while (recordCursor.moveToNext());
        }
    }

    private String getSessionID()
    {
        String session = sessionIdTxt.getText().toString();
        if (session.contains("Session ID: "))
        {
            return session.substring("Session ID: ".length(), session.length());
        }
        else
        {
            return session;
        }

    }

    private void scanPreviousSession()
    {
        if (recordCursor.moveToNext())
        {
            sessionId = recordCursor.getInt(recordCursor.getColumnIndex("sessionID"));
            date = recordCursor.getString(recordCursor.getColumnIndex("date"));
            dataByte = recordCursor.getBlob(recordCursor.getColumnIndex("data"));
            compressRatio = recordCursor.getFloat(recordCursor.getColumnIndex("ratio"));
            deCompressByte(dataInt, dataByte, compressRatio);
            updateSessionInformation();
            plotHistory();
        }
        else
        {
            recordCursor.moveToPrevious();
        }

    }

    private void scanNextDate()
    {
        String nextDate = changeDate(1);
        String selectQuery = "select * from " + recordTableName + " where username = '" + pUser + "' and date >= '" + nextDate + "'";
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst())
        {
            sessionId = cursor.getInt(cursor.getColumnIndex("sessionID"));
            date = cursor.getString(cursor.getColumnIndex("date"));
            dataByte = cursor.getBlob(cursor.getColumnIndex("data"));
            compressRatio = cursor.getFloat(cursor.getColumnIndex("ratio"));
            deCompressByte(dataInt, dataByte, compressRatio);
            updateSessionInformation();
            plotHistory();
        }
        else
        {
            selectQuery = "select * from " + recordTableName + " where username = '" + pUser + "' and date < '" + nextDate + "'";
            cursor = db.rawQuery(selectQuery, null);
            if (cursor.moveToFirst())
            {
                sessionId = cursor.getInt(cursor.getColumnIndex("sessionID"));
                date = cursor.getString(cursor.getColumnIndex("date"));
                dataByte = cursor.getBlob(cursor.getColumnIndex("data"));
                compressRatio = cursor.getFloat(cursor.getColumnIndex("ratio"));
                deCompressByte(dataInt, dataByte, compressRatio);
                updateSessionInformation();
                plotHistory();
            }
            else
            {
                Toast.makeText(DBActivity_User.this, "No Record for " + pName + " is found", Toast.LENGTH_LONG).show();
            }
        }
        cursor.close();
        if (recordCursor.moveToFirst()) {
            do {
                if (recordCursor.getInt(recordCursor.getColumnIndex("sessionID")) == sessionId)
                    break;
            } while (recordCursor.moveToNext());
        }
    }

    private void scanPreviousDate()
    {
        String nextDate = changeDate(-1);
        String selectQuery = "select * from " + recordTableName + " where username = '" + pUser + "' and date >= '" + nextDate + "'";
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst())
        {
            sessionId = cursor.getInt(cursor.getColumnIndex("sessionID"));
            date = cursor.getString(cursor.getColumnIndex("date"));
            dataByte = cursor.getBlob(cursor.getColumnIndex("data"));
            compressRatio = cursor.getFloat(cursor.getColumnIndex("ratio"));
            deCompressByte(dataInt, dataByte, compressRatio);
            updateSessionInformation();
            plotHistory();
        }
        else
        {
            selectQuery = "select * from " + recordTableName + " where username = '" + pUser + "' and date < '" + nextDate + "'";
            cursor = db.rawQuery(selectQuery, null);
            if (cursor.moveToFirst())
            {
                sessionId = cursor.getInt(cursor.getColumnIndex("sessionID"));
                date = cursor.getString(cursor.getColumnIndex("date"));
                dataByte = cursor.getBlob(cursor.getColumnIndex("data"));
                compressRatio = cursor.getFloat(cursor.getColumnIndex("ratio"));
                deCompressByte(dataInt, dataByte, compressRatio);
                updateSessionInformation();
                plotHistory();
            }
            else
            {
                Toast.makeText(DBActivity_User.this, "No Record for " + pName + " is found", Toast.LENGTH_LONG).show();
            }
        }
        cursor.close();
        if (recordCursor.moveToFirst()) {
            do {
                if (recordCursor.getInt(recordCursor.getColumnIndex("sessionID")) == sessionId)
                    break;
            } while (recordCursor.moveToNext());
        }
    }

    private String changeDate(int step)
    {

        SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(sdf.parse(dateTxt.getText().toString()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        c.add(Calendar.DATE, step);
        return sdf.format(c.getTime());
    }

    private void scanNextSession()
    {
        if (recordCursor.moveToPrevious())
        {
            sessionId = recordCursor.getInt(recordCursor.getColumnIndex("sessionID"));
            date = recordCursor.getString(recordCursor.getColumnIndex("date"));
            dataByte = recordCursor.getBlob(recordCursor.getColumnIndex("data"));
            compressRatio = recordCursor.getFloat(recordCursor.getColumnIndex("ratio"));
            deCompressByte(dataInt, dataByte, compressRatio);
            updateSessionInformation();
            plotHistory();
        }
        else
        {
            recordCursor.moveToNext();
        }
    }

    class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        /**
         * Before starting background thread Show Progress Bar Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        /**
         * Downloading file in background thread
         */
        @Override
        protected String doInBackground(String... sUrl) {
            File dbDir = new File(localDownloadFilePath);
            if (!dbDir.exists())
            {
                dbDir.mkdirs();
            }

            InputStream input = null;
            OutputStream output = null;
            HttpsURLConnection connection = null;
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
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
            }};

            try {
                SSLContext sc = SSLContext.getInstance("TLS");

                sc.init(null, trustAllCerts, new java.security.SecureRandom());

                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpsURLConnection) url.openConnection();

                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                //downloadButton.setText(Integer.toString(fileLength));
                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(localDownloadFilePath + "/" + sUrl[1]);
                //downloadButton.setText("Connecting .....");
                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;

        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }


        /**
         * After completing background task Dismiss the progress dialog
         **/
        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null) {
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();


            } else {
                Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
            }


            File dbDir = new File(DATAPATH);
            db = SQLiteDatabase.openOrCreateDatabase(dbDir + "/" + dbFileName, null);
            db.beginTransaction();
            try
            {
                // select user information from the use info table
                String selectUserQuery = "select * from " + userTableName
                        + " where username = '" + pUser + "'";
                userCursor = db.rawQuery(selectUserQuery, null);
                if (userCursor.moveToFirst())
                {
                    pAge = Integer.toString(userCursor.getInt(userCursor.getColumnIndex("age")));
                    pGender = userCursor.getString(userCursor.getColumnIndex("gender"));
                }

                userCursor.close();
            }
            catch (SQLiteException e) {
                //report problem
                Log.e("Error in transactions", e.getMessage());
            } finally {
                db.endTransaction();
            }

            spAgeLbl.setText("Age: " + pAge);
            spGenderLbl.setText("Gender: " + pGender);

            db.beginTransaction();
            try
            {
                // select record from the record table

                String selectRecordQuery = "select * from " + recordTableName
                        + " where username = '" + pUser + "' order by sessionID DESC ";

                recordCursor = db.rawQuery(selectRecordQuery, null);
                if (recordCursor.moveToFirst())
                {
                    sessionId = recordCursor.getInt(recordCursor.getColumnIndex("sessionID"));
                    date = recordCursor.getString(recordCursor.getColumnIndex("date"));
                    dataByte = recordCursor.getBlob(recordCursor.getColumnIndex("data"));
                    compressRatio = recordCursor.getFloat(recordCursor.getColumnIndex("ratio"));
                    deCompressByte(dataInt, dataByte, compressRatio);
                    plotHistory();
                }

                //recordCursor.close();
                db.setTransactionSuccessful();
            }
            catch (SQLiteException e) {
                //report problem
                Log.e("Error in transactions", e.getMessage());
            } finally {
                db.endTransaction();
            }

            updateSessionInformation();
        }

    }
}
