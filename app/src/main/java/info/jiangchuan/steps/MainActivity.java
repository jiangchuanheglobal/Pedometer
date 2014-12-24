package info.jiangchuan.steps;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.TextView;
import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

public class MainActivity extends ActionBarActivity implements SensorEventListener
{
    private final String TAG = "MainActivity";

    // view elements
    private TextView mHZ;    // sample frequency
    private TextView mPower; // power of spectrum
    private TextView mWindow; // window size
    private TextView mViewSteps; // Steps counting by this algorithm
    private TextView mFreq; // current walking frequency
    private TextView mThreshold;

    // sensor
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    // parameters of this algorithm
    private final int windowSize = 400;
    private final int incrementScale = 50;
    private final double threshold = 50;

    double[] real = new double[512];
    double[] img = new double[512];
    private FFT FFTObj = new FFT(512); // size needs to be power of 2
    private double[]mNormData = new double[windowSize];
    private double[] filter;

    private int size = -1;
    private long sampleStartTime;
    private long sampleEndTime;
    private long sampleCounter = -1;
    private long sampleFrequency = 0;
    private double totalSteps = 0;

    private boolean hasFilterCoefficient = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHZ = (TextView)findViewById(R.id.txtHZ);
        mPower = (TextView)findViewById(R.id.txtPower);
        mFreq = (TextView)findViewById(R.id.txtFreq);
        mViewSteps = (TextView)findViewById(R.id.txtSteps);
        mThreshold = (TextView)findViewById(R.id.txtThreshold);
        mThreshold.setText("50");

        // sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_GAME );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    }
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {

        updateCurrentSampelFrequency(event.timestamp);

        double x = (double) (event.values[0]);
        double y = (double) (event.values[1]);
        double z = (double) (event.values[2]);
        appendRawDataIntoBuffer(x, y, z);

        if (getRawDataBufferSize() == windowSize-1) {
            double[]pt = getFrequencyDomainPeakPointInRange(0, 30);
            mHZ.setText(Double.toString(getCurrentSampleFrequency()));
            if (pt[0] >= threshold) {
                double timeInterval = incrementScale / getCurrentSampleFrequency();
                totalSteps += pt[1]*timeInterval;
                mFreq.setText(Double.toString(pt[1]));
            } else {
                mFreq.setText("0"); // less than threshold, consider its as noise
            }
            mPower.setText(Double.toString(pt[0]));
        }
        mViewSteps.setText(Double.toString(totalSteps));
    }

    private double[] getFrequencyDomainPeakPointInRange(int left, int right) {
        double[] k = getFilter();
        double[] tmp = Filter.filter(getRawDataBuffer(), k);

        Arrays.fill(real, 0);
        Arrays.fill(img, 0);
        System.arraycopy(tmp, 0, real, 0, tmp.length);
        FFTObj.fft(real, img);

        double[] value = getAbsoluteValue(real, img);
        int rightBound = value.length/2 * 5 * 2 / (int)getCurrentSampleFrequency();
        int maxIndex = getIndexOfMaxElementInRange(value, 0, rightBound);

        double[] res = new double[2];
        res[0] = value[maxIndex]; // power of spectrum
        res[1] = getCurrentSampleFrequency()/2 * 1 / (real.length/2 + 1)*(double)maxIndex; // step frequency

        return res;
    }

    private int getIndexOfMaxElementInRange(double[] A, int left, int right) {
        double max = A[0];
        int maxI = 0;
        for ( int i = left; i < right; i++) {
            if (A[i] > max) {
                max = A[i];
                maxI = i;
            }
        }
        return maxI;
    }

    private double[] getFilter() {
        if (hasFilterCoefficient == false) {
            filter =  Filter.createBandpass(200, 0.7, 10, getCurrentSampleFrequency());
            hasFilterCoefficient = true;
        }
        return filter;
    }
    private double[] getAbsoluteValue(double[] real, double[]img) {
        double[] res = new double[real.length];
        for ( int i = 0; i < real.length; i++) {
            res[i] = Math.sqrt(real[i]*real[i] + img[i]*img[i]);
        }
        return res;
    }

    // raw data buffer
    private void appendRawDataIntoBuffer(double x, double y, double z) {
        ++size;
        double norm = Math.sqrt(x*x + y*y + z*z);
        if (size == windowSize) {
            try {
                System.arraycopy(mNormData, incrementScale, mNormData, 0, windowSize - incrementScale);
                size = windowSize - incrementScale;
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }

        mNormData[size] = norm;
    }
    // raw data buffer
    private double[] getRawDataBuffer() {
        return mNormData;
    }
    // raw data buffer
    private int getRawDataBufferSize() {
        return size;
    }

    // sampleFrequency
    private double getCurrentSampleFrequency() {
        return sampleFrequency;
    }
    // sampleFrequency
    private void updateCurrentSampelFrequency(long curSampleTimeStamp)
    {
        ++sampleCounter;
        if (sampleCounter == 0) {
            sampleStartTime = curSampleTimeStamp;
            return;
        }
        if (sampleCounter == windowSize-1) {
            sampleEndTime = curSampleTimeStamp;
            long count = windowSize-1;
            sampleFrequency = count * 1000000000/(sampleEndTime - sampleStartTime);
            sampleCounter = -1;
        }
    }
}
