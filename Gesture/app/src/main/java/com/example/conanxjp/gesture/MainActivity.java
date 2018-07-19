package com.example.conanxjp.gesture;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private Button trainBtn;
    private Button testBtn;
    private Button procBtn;
    private TextView outputTxt;
    private boolean training = true;

    final int SIZE = 256;
    final int FAKE = 2;
    final int NUM = 5;
    final float FACTOR = (float)1.1;
    private SensorManager accelManage;
    private Sensor senseAccel;
    float accelValuesX_A[][] = new float[NUM][SIZE];
    float accelValuesY_A[][] = new float[NUM][SIZE];
    float accelValuesZ_A[][] = new float[NUM][SIZE];
    float accelValuesX_B[][] = new float[NUM][SIZE];
    float accelValuesY_B[][] = new float[NUM][SIZE];
    float accelValuesZ_B[][] = new float[NUM][SIZE];
    float accelValuesX_C[][] = new float[NUM][SIZE];
    float accelValuesY_C[][] = new float[NUM][SIZE];
    float accelValuesZ_C[][] = new float[NUM][SIZE];
    float accelValuesX_D[][] = new float[NUM][SIZE];
    float accelValuesY_D[][] = new float[NUM][SIZE];
    float accelValuesZ_D[][] = new float[NUM][SIZE];
    float accelValuesX[] = new float[SIZE];
    float accelValuesY[] = new float[SIZE];
    float accelValuesZ[] = new float[SIZE];
    float accelValuesXT[] = new float[SIZE];
    float accelValuesYT[] = new float[SIZE];
    float accelValuesZT[] = new float[SIZE];
    int combine = NUM * (NUM - 1) / 2;
    float dtw_AX[] = new float[combine];
    float dtw_BX[] = new float[combine];
    float dtw_CX[] = new float[combine];
    float dtw_DX[] = new float[combine];
    float dtw_AY[] = new float[combine];
    float dtw_BY[] = new float[combine];
    float dtw_CY[] = new float[combine];
    float dtw_DY[] = new float[combine];
    float dtw_AZ[] = new float[combine];
    float dtw_BZ[] = new float[combine];
    float dtw_CZ[] = new float[combine];
    float dtw_DZ[] = new float[combine];
    float dtwAX = 0;
    float dtwBX = 0;
    float dtwCX = 0;
    float dtwDX = 0;
    float dtwAY = 0;
    float dtwBY = 0;
    float dtwCY = 0;
    float dtwDY = 0;
    float dtwAZ = 0;
    float dtwBZ = 0;
    float dtwCZ = 0;
    float dtwDZ = 0;
    float thresholdA[] = new float[3];
    float thresholdB[] = new float[3];;
    float thresholdC[] = new float[3];;
    float thresholdD[] = new float[3];;
    int index = 0;
    Long tsLong1 = System.currentTimeMillis();
    Long tsLong2 = System.currentTimeMillis();
    Long temp = tsLong2;
    int round = 0;
    String testName = "";

    private LineGraphSeries<DataPoint> seriesX;
    private LineGraphSeries<DataPoint> seriesY;
    private LineGraphSeries<DataPoint> seriesZ;

    private GraphView graph;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        accelManage = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senseAccel = accelManage.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onStart() {
        super.onStart();
        trainBtn = (Button) findViewById(R.id.trainBtn);
        testBtn = (Button) findViewById(R.id.testBtn);
        procBtn = (Button) findViewById(R.id.processBtn);
        outputTxt = (TextView) findViewById(R.id.outputTxt);
        graph = (GraphView) findViewById(R.id.graph);

        graph.setTitle("Plot");
        graph.setTitleTextSize((float) 50.0);
        graph.getGridLabelRenderer().setHorizontalAxisTitle("64 data points");
        graph.getGridLabelRenderer().setVerticalAxisTitle("Value");

        Viewport viewPort = graph.getViewport();
        viewPort.setXAxisBoundsManual(true);
        viewPort.setMaxX(SIZE + 1);
        viewPort.setMinX(0.0);

        viewPort.setYAxisBoundsManual(true);
        viewPort.setMaxY(21);
        viewPort.setMinY(-21);

        seriesX = new LineGraphSeries<DataPoint>();
        seriesY = new LineGraphSeries<DataPoint>();
        seriesZ = new LineGraphSeries<DataPoint>();
        seriesX.setTitle("x data");
        seriesX.setColor(Color.RED);
        seriesY.setTitle("y data");
        seriesY.setColor(Color.BLUE);
        seriesZ.setTitle("z data");
        seriesZ.setColor(Color.BLACK);
        graph.getLegendRenderer().setVisible(true);

        graph.addSeries(seriesX);
        graph.addSeries(seriesY);
        graph.addSeries(seriesZ);

        trainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (round < 5) {
                    testName = "A";
                }
                else if (round < 10) {
                    testName = "B";
                }
                else if (round < 15) {
                    testName = "C";
                }
                else {
                    testName = "D";
                }
                round ++;
                training = true;
                outputTxt.setText("Collecting Train data for " + testName);
                accelManage.registerListener(MainActivity.this, senseAccel, SensorManager.SENSOR_DELAY_FASTEST);

            }
        });

        procBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                outputTxt.setText("Processing Data.... ");
                processData(accelValuesX_A, dtw_AX, dtwAX);
                processData(accelValuesX_B, dtw_BX, dtwBX);
                processData(accelValuesX_C, dtw_CX, dtwCX);
                processData(accelValuesX_D, dtw_DX, dtwDX);
                processData(accelValuesY_A, dtw_AY, dtwAY);
                processData(accelValuesY_B, dtw_BY, dtwBY);
                processData(accelValuesY_C, dtw_CY, dtwCY);
                processData(accelValuesY_D, dtw_DY, dtwDY);
                processData(accelValuesZ_A, dtw_AZ, dtwAZ);
                processData(accelValuesZ_B, dtw_BZ, dtwBZ);
                processData(accelValuesZ_C, dtw_CZ, dtwCZ);
                processData(accelValuesZ_D, dtw_DZ, dtwDZ);

                thresholdA[0] = (dtwAX * FACTOR);
                thresholdA[1] = (dtwAY * FACTOR);
                thresholdA[2] = (dtwAZ * FACTOR);

                thresholdB[0] = (dtwBX * FACTOR);
                thresholdB[1] = (dtwBY * FACTOR);
                thresholdB[2] = (dtwBZ * FACTOR);

                thresholdC[0] = (dtwCX * FACTOR);
                thresholdC[1] = (dtwCY * FACTOR);
                thresholdC[2] = (dtwCZ * FACTOR);

                thresholdD[0] = (dtwDX * FACTOR);
                thresholdD[1] = (dtwDY * FACTOR);
                thresholdD[2] = (dtwDZ * FACTOR);
                outputTxt.setText("Process Data Done");
            }
        });

        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                training = false;
                outputTxt.setText("Collecting Test data...");
                accelManage.registerListener(MainActivity.this, senseAccel, SensorManager.SENSOR_DELAY_FASTEST);
            }
        });
    }


    private void processData(float[][] accelValues, float[] dtwArray, float dtw) {
        for (int i = 0; i < NUM; i ++) {
            normalize(accelValues[i]);
        }

        int idx = 0;
        for (int i = 0; i < NUM; i ++) {
            for (int j = i + 1; j < NUM; j ++) {
                dtwArray[idx ++] = DTW(accelValues[i], accelValues[j]);
            }
        }

        for (int i = 0; i < dtwArray.length; i ++) {
            dtw += dtwArray[i];
        }
        dtw =  dtw / dtwArray.length;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (training) {
                switch (testName) {
                    case "A":
                        collectData(accelValuesX_A, accelValuesY_A, accelValuesZ_A, event);
                        break;
                    case "B":
                        collectData(accelValuesX_B, accelValuesY_B, accelValuesZ_B, event);
                        break;
                    case "C":
                        collectData(accelValuesX_C, accelValuesY_C, accelValuesZ_C, event);
                        break;
                    case "D":
                        collectData(accelValuesX_D, accelValuesY_D, accelValuesZ_D, event);
                        break;
                }

                index++;
                if(index >= SIZE - 1){

                    outputTxt.setText("Train Data " + testName + ", Round" + round % NUM + " Done");

                    index = 0;
                    accelManage.unregisterListener(this);

                    plotdata(accelValuesX, accelValuesY, accelValuesZ);

                }

            }
            else {

                accelValuesXT[index] = event.values[0];
                accelValuesYT[index] = event.values[1];
                accelValuesZT[index] = event.values[2];
                index++;
                if(index >= SIZE - 1){

                    index = 0;

                    accelManage.unregisterListener(this);

                    normalize(accelValuesXT);
                    normalize(accelValuesYT);
                    normalize(accelValuesZT);

                    /*
                    float dtwX = DTW(accelValuesX, accelValuesXT);
                    float dtwY = DTW(accelValuesY, accelValuesYT);
                    float dtwZ = DTW(accelValuesZ, accelValuesZT);
                    */

                    outputTxt.setText("Calculating result...");
                    String result = calcResult();
                    outputTxt.setText("Result is: " + result);
                    //callFallRecognition();
                    //callGestureRecognition();
                    //accelManage.registerListener(this, senseAccel, SensorManager.SENSOR_DELAY_NORMAL);
                }
            }

        }
    }

    private String calcResult() {

        float dAX = 0, dBX = 0, dCX = 0, dDX = 0, dAY = 0, dBY = 0, dCY = 0, dDY = 0, dAZ = 0, dBZ = 0, dCZ = 0, dDZ = 0;
        float dA = 0, dB = 0, dC = 0, dD = 0;
        for (int i = 0; i < NUM; i ++) {
            dAX += DTW(accelValuesX_A[i], accelValuesXT);
        }
        for (int i = 0; i < NUM; i ++) {
            dBX += DTW(accelValuesX_B[i], accelValuesXT);
        }
        for (int i = 0; i < NUM; i ++) {
            dCX += DTW(accelValuesX_C[i], accelValuesXT);
        }
        for (int i = 0; i < NUM; i ++) {
            dDX += DTW(accelValuesX_D[i], accelValuesXT);
        }
        for (int i = 0; i < NUM; i ++) {
            dAY += DTW(accelValuesY_A[i], accelValuesYT);
        }
        for (int i = 0; i < NUM; i ++) {
            dBY += DTW(accelValuesY_B[i], accelValuesYT);
        }
        for (int i = 0; i < NUM; i ++) {
            dCY += DTW(accelValuesY_C[i], accelValuesYT);
        }
        for (int i = 0; i < NUM; i ++) {
            dDY += DTW(accelValuesY_D[i], accelValuesYT);
        }
        for (int i = 0; i < NUM; i ++) {
            dAZ += DTW(accelValuesZ_A[i], accelValuesZT);
        }
        for (int i = 0; i < NUM; i ++) {
            dBZ += DTW(accelValuesZ_B[i], accelValuesZT);
        }
        for (int i = 0; i < NUM; i ++) {
            dCZ += DTW(accelValuesZ_C[i], accelValuesZT);
        }
        for (int i = 0; i < NUM; i ++) {
            dDZ += DTW(accelValuesZ_D[i], accelValuesZT);
        }
        dAX /= NUM;
        dBX /= NUM;
        dCX /= NUM;
        dDX /= NUM;
        dAY /= NUM;
        dBY /= NUM;
        dCY /= NUM;
        dDY /= NUM;
        dAZ /= NUM;
        dBZ /= NUM;
        dCZ /= NUM;
        dDZ /= NUM;

        dA = dAX - thresholdA[0] + dAY - thresholdA[1] + dAZ - thresholdA[2];
        dB = dBX - thresholdB[0] + dBY - thresholdB[1] + dBZ - thresholdB[2];
        dC = dCX - thresholdC[0] + dCY - thresholdC[1] + dCZ - thresholdC[2];
        dD = dDX - thresholdD[0] + dDY - thresholdD[1] + dDZ - thresholdD[2];

        float result = Min(dA, dB, dC, dD);
        String character;
        if (result == dA) {
            character = "A";
        }
        else if (result == dB) {
            character = "B";
        }
        else if (result == dC) {
            character = "C";
        }
        else {
            character = "D";
        }
        return character;
    }

    private float Min(float dA, float dB, float dC, float dD) {
        float temp1 = Math.min(dA, dB);
        float temp2 = Math.min(dC, dD);
        return Math.min(temp1, temp2);
    }

    private void collectData(float[][] accelValuesx, float[][] accelValuesy, float[][] accelValuesz, SensorEvent event) {
        accelValuesx[round % NUM][index] = event.values[0];
        accelValuesy[round % NUM][index] = event.values[1];
        accelValuesz[round % NUM][index] = event.values[2];
        accelValuesX[index] = accelValuesx[round % NUM][index];
        accelValuesY[index] = accelValuesy[round % NUM][index];
        accelValuesZ[index] = accelValuesz[round % NUM][index];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*
    public float[] distance(float xdata1[], float ydata1[], float zdata1[]) {
        float dist[] = new float[SIZE - 2 * FAKE];
        float x = 0;
        float y = 0;
        float z = 0;
        for (int i = 0; i < dist.length; i ++) {
            x = (xdata1[i + FAKE] - xdata1[i +FAKE - 1]) * (xdata1[i + FAKE] - xdata1[i +FAKE - 1]);
            y = (ydata1[i + FAKE] - ydata1[i +FAKE - 1]) * (ydata1[i + FAKE] - ydata1[i +FAKE - 1]);
            z = (zdata1[i + FAKE] - zdata1[i +FAKE - 1]) * (zdata1[i + FAKE] - zdata1[i +FAKE - 1]);
            dist[i] = (float) Math.sqrt((double)(x + y + z));
        }
        return dist;
    }
    */

    public void normalize(float s[]) {
        float max = 0;
        float min = 99999;

        for (int i = 0; i < s.length; i ++) {
            if (s[i] > max) {
                max = s[i];
            }
            if (s[i] < min) {
                min = s[i];
            }
        }

        for (int i = 0; i < s.length; i++) {
            s[i] = (s[i] - min) / (max - min);
        }

    }

    public float DTW(float s1[], float s2[]) {
        int n = s1.length;
        int m = s2.length;
        float dtw[][] = new float[n + 1][m + 1];
        float cost = 0;
        for (int i = 1; i <= n; i ++) {
            dtw[i][0] = 99999;
        }
        for (int i = 1; i <= m; i ++) {
            dtw[0][i] = 99999;
        }
        dtw[0][0] = 0;

        for (int i = 1; i <= n; i ++) {
            for (int j = 1; j <= m; j ++) {
                cost = Math.abs(s1[i - 1] - s2[j - 1]);
                dtw[i][j] = cost + Math.min(Math.min(dtw[i - 1][j], dtw[i][j - 1]), dtw[i - 1][j - 1]);
            }
        }
        return dtw[n][m];
    }

    public float DTW2(float s1[], float s2[]) {
        int n = s1.length;
        int m = s2.length;
        int idx1 = 0;
        int idx2 = 0;
        float dtw[][] = new float[n + 1][m + 1];
        float cost = 0;
        for (int i = 1; i <= n; i ++) {
            dtw[i][0] = 99999;
        }
        for (int i = 1; i <= m; i ++) {
            dtw[0][i] = 99999;
        }
        dtw[0][0] = 0;

        for (int i = 1; i <= n; i ++) {
            if (i > 1) {
                idx1 = i - 2;
            }
            else {
                idx1 = i;
            }
            for (int j = 1; j <= m; j ++) {
                if (j > 1) {
                    idx2 = j - 2;
                }
                else {
                    idx2 = j;
                }

                cost = Math.abs(s1[i - 1] - s2[j - 1]);
                dtw[i][j] = cost + Math.min(Math.min(dtw[idx1][j - 1], dtw[i - 1][idx2]), dtw[i - 1][j - 1]);
            }
        }
        return dtw[n][m];
    }

    private DataPoint[] generateData(float dataSet[]) {
        DataPoint[] values = new DataPoint[SIZE];
        for (int i = 0; i < SIZE; i ++) {
            int x = i + 1;
            DataPoint v = new DataPoint(x, dataSet[i]);
            values[i] = v;
        }
        return values;
    }

    public void plotdata(float x[], float y[], float z[]) {
        seriesX.resetData(generateData(x));
        seriesY.resetData(generateData(y));
        seriesZ.resetData(generateData(z));
    }
}
