package com.adafruit.bluefruit.le.connect.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;


import com.adafruit.bluefruit.le.connect.R;
import com.adafruit.bluefruit.le.connect.app.settings.ConnectedSettingsActivity;
import com.adafruit.bluefruit.le.connect.ble.BleManager;
import com.adafruit.bluefruit.le.connect.ui.utils.ExpandableHeightExpandableListView;
import com.adafruit.bluefruit.le.connect.ui.utils.ExpandableHeightListView;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;



import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ControllerActivity extends UartInterfaceActivity implements AdapterView.OnItemSelectedListener, ColorPicker.OnColorChangedListener, SeekBar.OnSeekBarChangeListener {
    // Config
    private final static boolean kKeepUpdatingParentValuesInChildActivities = true;

    // Log
    private final static String TAG = ControllerActivity.class.getSimpleName();

    // Activity request codes (used for onActivityResult)
    private static final int kActivityRequestCode_ConnectedSettingsActivity = 0;
    private static final int kActivityRequestCode_PadActivity = 1;
    private static final int kActivityRequestCode_ColorPickerActivity = 2;

    // Constants
    private final static String kPreferences = "ControllerActivity_prefs";
    private final static String kPreferences_uartToolTip = "uarttooltip";

    // Constants
    private final static int kSendDataInterval = 500;   // milliseconds

    // UI
    private ExpandableHeightExpandableListView mControllerListView;
    private ExpandableListAdapter mControllerListAdapter;

    private ViewGroup mUartTooltipViewGroup;

    // Data
    private Handler sendDataHandler = new Handler();

    private float[] mRotation = new float[9];
    private float[] mOrientation = new float[3];
    private float[] mQuaternion = new float[4];


    private boolean isSensorPollingEnabled = false;


    private int state;


    //color picker
    // Constants
    private final static boolean kPersistValues = true;
    //private final static String kPreferences = "ColorPickerActivity_prefs";
    private final static String kPreferences_color = "color";

    private final static int kFirstTimeColor = 0x0000ff;

    // UI
    private ColorPicker mColorPicker;
    private View mRgbColorView;
    private TextView mRgbTextView;

    private int mSelectedColor;

    //rainbow
    private final static int kFirstTimeSpeed=100;

    private SeekBar speedbar;
    private TextView speedTextView;
    private int animspeed;

    private final static String kPreferences_speed = "speed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        //Log.d(TAG, "onCreate");

        mBleManager = BleManager.getInstance(this);


        // UI
        //mControllerListView = (ExpandableHeightExpandableListView) findViewById(R.id.controllerListView);
        //mControllerListAdapter = new ExpandableListAdapter(this, mSensorData);
        //mControllerListView.setAdapter(mControllerListAdapter);
        //mControllerListView.setExpanded(true);

        Spinner animSpinner = (Spinner) findViewById(R.id.animSpinner);
        String[] items = new String[]{"Static", "Rainbow", "Fade"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items);
        animSpinner.setAdapter(adapter);
        animSpinner.setOnItemSelectedListener(this);
/*
        ExpandableHeightListView interfaceListView = (ExpandableHeightListView) findViewById(R.id.interfaceListView);
        ArrayAdapter<String> interfaceListAdapter = new ArrayAdapter<>(this, R.layout.layout_controller_interface_title, R.id.titleTextView, getResources().getStringArray(R.array.controller_interface_items));
        assert interfaceListView != null;
        interfaceListView.setAdapter(interfaceListAdapter);
        interfaceListView.setExpanded(true);
        interfaceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    Intent intent = new Intent(ControllerActivity.this, ColorPickerActivity.class);
                    startActivityForResult(intent, kActivityRequestCode_ColorPickerActivity);
                } else {
                    Intent intent = new Intent(ControllerActivity.this, PadActivity.class);
                    startActivityForResult(intent, kActivityRequestCode_PadActivity);
                }
            }
        });
*/

        // Sensors
        // mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        state=0;

        // Start services
        onServicesDiscovered();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart");

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");

        if (kPersistValues) {
            SharedPreferences settings = getSharedPreferences(kPreferences, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(kPreferences_color, mSelectedColor);
            editor.apply();
        }

        super.onStop();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");

        super.onResume();

        // Setup listeners
        mBleManager.setBleListener(this);


    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

        super.onPause();

    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        // Retain data


        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_controller, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_help) {
            startHelp();
            return true;
        } else if (id == R.id.action_connected_settings) {
            startConnectedSettings();
            return true;
        } else if (id == R.id.action_refreshcache) {
            if (mBleManager != null) {
                mBleManager.refreshDeviceCache();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void startConnectedSettings() {
        // Launch connected settings activity
        Intent intent = new Intent(this, ConnectedSettingsActivity.class);
        startActivityForResult(intent, kActivityRequestCode_ConnectedSettingsActivity);
    }

    private void startHelp() {
        // Launch app help activity
        Intent intent = new Intent(this, CommonHelpActivity.class);
        intent.putExtra("title", getString(R.string.controller_help_title));
        intent.putExtra("help", "controller_help.html");
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0) {
            if (resultCode < 0) {       // Unexpected disconnect
                setResult(resultCode);
                finish();
            }
        }
    }


    @Override
    public void onDisconnected() {
        super.onDisconnected();
        Log.d(TAG, "Disconnected. Back to previous activity");
        setResult(-1);      // Unexpected Disconnect
        finishActivity(kActivityRequestCode_PadActivity);
        finishActivity(kActivityRequestCode_ColorPickerActivity);
        finish();
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
        LinearLayout myLayout = (LinearLayout)findViewById(R.id.inflateLayout);

        switch (pos){

            case 0: // Color Picker
                //save rainbow state
                if (kPersistValues&&state!=0) {
                    SharedPreferences settings = getSharedPreferences(kPreferences, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt(kPreferences_speed, speedbar.getProgress());
                    editor.apply();
                }

                myLayout.removeAllViews();

                LinearLayout colorpicklayout = (LinearLayout)findViewById(R.id.colorPickerLayout);
                if(colorpicklayout==null){
                    //LinearLayout myLayout = (LinearLayout)findViewById(R.id.inflateLayout);
                    View hiddenInfo = getLayoutInflater().inflate(R.layout.activity_color_picker, myLayout, false);
                    if(hiddenInfo!=null) {
                        myLayout.addView(hiddenInfo);
                    }

                    //color picker
                    // UI
                    mRgbColorView = findViewById(R.id.rgbColorView);
                    mRgbTextView = (TextView) findViewById(R.id.rgbTextView);

                    SaturationBar mSaturationBar = (SaturationBar) findViewById(R.id.saturationbar);
                    ValueBar mValueBar = (ValueBar) findViewById(R.id.valuebar);
                    mColorPicker = (ColorPicker) findViewById(R.id.colorPicker);
                    if (mColorPicker != null) {
                        mColorPicker.addSaturationBar(mSaturationBar);
                        mColorPicker.addValueBar(mValueBar);
                        mColorPicker.setOnColorChangedListener((ColorPicker.OnColorChangedListener) this);
                    }

                    if (kPersistValues) {
                        SharedPreferences preferences = getSharedPreferences(kPreferences, Context.MODE_PRIVATE);
                        mSelectedColor = preferences.getInt(kPreferences_color, kFirstTimeColor);
                    } else {
                        mSelectedColor = kFirstTimeColor;
                    }

                    mColorPicker.setOldCenterColor(mSelectedColor);
                    mColorPicker.setColor(mSelectedColor);
                    onColorChanged(mSelectedColor);
                }
                state=0;

                break;

            case 1: //Rainbow
                //save colorpicker state
                if (kPersistValues&&state!=1) {
                    SharedPreferences settings = getSharedPreferences(kPreferences, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt(kPreferences_color, mSelectedColor);
                    editor.apply();
                }
                //remove current view
                myLayout.removeAllViews();

                LinearLayout rainbowpicklayout = (LinearLayout)findViewById(R.id.rainbowLayout);
                if(rainbowpicklayout==null){
                    //LinearLayout myLayout = (LinearLayout)findViewById(R.id.inflateLayout);
                    View hiddenInfo = getLayoutInflater().inflate(R.layout.layout_rainbow, myLayout, false);
                    if(hiddenInfo!=null) {
                        myLayout.addView(hiddenInfo);
                    }
                }

                speedTextView = (TextView) findViewById(R.id.speedText);
                speedbar = (SeekBar)findViewById(R.id.speedBar);
                if(speedbar!=null){
                    speedbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
                    speedbar.setMax(500);
                }
                if (kPersistValues) {
                    SharedPreferences preferences = getSharedPreferences(kPreferences, Context.MODE_PRIVATE);
                    speedbar.setProgress(preferences.getInt(kPreferences_speed, kFirstTimeSpeed));
                } else {
                    speedbar.setProgress(kFirstTimeSpeed);
                }
                onProgressChanged(speedbar,speedbar.getProgress(),false);

                state=1;

                break;

        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // not possible
    }




    //COLOR PICKER
    @Override
    public void onColorChanged(int color) {
        // Save selected color
        mSelectedColor = color;

        // Update UI
        mRgbColorView.setBackgroundColor(color);

        final int r = (color >> 16) & 0xFF;
        final int g = (color >> 8) & 0xFF;
        final int b = (color >> 0) & 0xFF;
        final String text = String.format(getString(R.string.colorpicker_rgbformat), r, g, b);
        mRgbTextView.setText(text);
    }


    //seekbar

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        animspeed=progress*(-1)+530;
        final String text = String.format("%1$d",animspeed);
        speedTextView.setText(text);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void onClickSend(View view) {
        ByteBuffer buffer=null;
        String prefix;
        switch (state) {
            case 0:
                // Set the old color
                mColorPicker.setOldCenterColor(mSelectedColor);

                // Send selected color !Crgb
                byte r = (byte) ((mSelectedColor >> 16) & 0xFF);
                byte g = (byte) ((mSelectedColor >> 8) & 0xFF);
                byte b = (byte) ((mSelectedColor >> 0) & 0xFF);

                buffer = ByteBuffer.allocate(2 + 3 * 1).order(java.nio.ByteOrder.LITTLE_ENDIAN);

                // prefix
                prefix = "!C";
                buffer.put(prefix.getBytes());

                // values
                buffer.put(r);
                buffer.put(g);
                buffer.put(b);
                break;

            case 1:
                buffer = ByteBuffer.allocate(2+4*1).order(ByteOrder.LITTLE_ENDIAN);
                prefix = "!R";
                buffer.put(prefix.getBytes());
                buffer.put(intToByteArray(animspeed));
                break;
        }

        byte[] result = buffer.array();
        sendDataWithCRC(result);
    }

    public static byte[] intToByteArray(int a)
    {
        byte[] ret = new byte[2];
        ret[1] = (byte) (a & 0xFF);
        ret[0] = (byte) ((a >> 8) & 0xFF);
        return ret;
    }

}