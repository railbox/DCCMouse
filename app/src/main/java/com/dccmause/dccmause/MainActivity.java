
package com.dccmause.dccmause;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements XpressNet.Callback {
    private static boolean EmergencyStop = false;

    private DB mDB;
    private SharedPreferences sPreferences;
    final static String SF_LOCOID = "locoid";

    private LocoFragment mLocoFragment;
    private LocoChooseFragment mLocoChooseFragment;
    private LocoEditorFragment mLocoEditorFragment;
    private CVChangeFragment mCVChangeFragment;
    private DecoderChooseFragment mDecoderChooseFragment;
    private CVChooseFragment mCVChooseFragment;
    private CVManualFragment mCVManualFragment;
    private TurnoutFragment mTurnoutFragment;
    private FragmentTransaction mFragmentTransaction;

    private Menu mMenu;
    private int mCurrentTab;
    private Fragment mCurrentFragment;
    private TextView mStatus;
    private Interfaces mInterfaces;

    private static final int REQUEST_ENABLE_BT = 1;

    DB getDB(){
        return mDB;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.Toolbar));
        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(getString(R.string.loco));

        sPreferences = getPreferences(MODE_PRIVATE);
        mDB = new DB(this);
        mDB.open();

        mLocoFragment = new LocoFragment();
        mLocoChooseFragment = new LocoChooseFragment();
        mLocoEditorFragment = new LocoEditorFragment();
        mTurnoutFragment = new TurnoutFragment();
        mDecoderChooseFragment = new DecoderChooseFragment();
        mCVChangeFragment = new CVChangeFragment();
        mCVChooseFragment = new CVChooseFragment();
        mCVManualFragment = new CVManualFragment();
        mCurrentTab = R.id.action_loco;
        mCurrentFragment = mLocoFragment;
        mFragmentTransaction = getSupportFragmentManager().beginTransaction();
        mFragmentTransaction.add(R.id.MainFragment, mLocoFragment);
        mFragmentTransaction.commit();

        mInterfaces = new Interfaces(getApplicationContext(),Interfaces.IType.WIFI);
        mInterfaces.setConnectionStatusCallbackHandler(mConnectionStatusCallback);
        mInterfaces.setDataReceivedHandler(mDataCallback);
        XpressNet.setCallbackHandler(this);

        mStatus = (TextView)findViewById(R.id.Status);

    }

    @Override
    public void onDestroy(){
        mDB.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        if (mCurrentFragment == mLocoChooseFragment)
            CloseLocoList(-1);
        else if (mCurrentFragment == mLocoEditorFragment)
            CloseLocoEditor();
        else if (mCurrentFragment == mCVChangeFragment)
            OpenCVList(-1);
        else if (  (mCurrentFragment == mCVChooseFragment)
                || (mCurrentFragment == mCVManualFragment)  )
            OpenDecoderList();
        else if (  (mCurrentFragment == mLocoFragment)
                || (mCurrentFragment == mDecoderChooseFragment)
                || (mCurrentFragment == mTurnoutFragment)  )
        {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle(R.string.exit);
            adb.setMessage(R.string.exit_question);
            adb.setIcon(android.R.drawable.ic_dialog_info);
            adb.setPositiveButton(R.string.yes,  new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                    System.exit(0);
                }
            });
            adb.setNegativeButton(R.string.no,  new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            adb.show();
        }
        //super.onBackPressed();
    }

    long GetLongPreference(final String param){
        return sPreferences.getLong(param, -1);
    }

    void SetLongPreference(final String param, long value){
        SharedPreferences.Editor ed = sPreferences.edit();
        ed.putLong(param,value);
        ed.apply();
    }

    public void OpenLocoList(){
        mMenu.findItem(R.id.action_cv).setVisible(false);
        mMenu.findItem(R.id.action_turnout).setVisible(false);
        SwitchToFragment(mLocoChooseFragment,getString(R.string.locoList));
    }

    public void OpenDecoderList(){
        SwitchToFragment(mDecoderChooseFragment,getString(R.string.decoderList));
    }

    public void CloseLocoList(long LocoId){
        mMenu.findItem(R.id.action_cv).setVisible(true);
        mMenu.findItem(R.id.action_turnout).setVisible(true);
        SwitchToFragment(mLocoFragment,getString(R.string.loco));
        if (LocoId != -1)
            SetLongPreference(SF_LOCOID,LocoId);
    }

    public void OpenLocoEditor(long LocoID){
        mLocoEditorFragment.setLocoID(LocoID);
        SwitchToFragment(mLocoEditorFragment,getString(R.string.locoEditor));
    }

    public void OpenCVList(long DecoderID){
        if (DecoderID >= 0)
            mCVChooseFragment.setDecoderID(DecoderID);
        SwitchToFragment(mCVChooseFragment,getString(R.string.CVList));
    }

    public void OpenCVEditor(long cvID){
        mCVChangeFragment.setCVID(cvID);
        SwitchToFragment(mCVChangeFragment,getString(R.string.CVEditor));
    }

    public void OpenManualCVEditor(){
        SwitchToFragment(mCVManualFragment,getString(R.string.CVEditor));
    }

    public void CloseLocoEditor(){
        hideSoftKeyboard(this);
        SwitchToFragment(mLocoChooseFragment,getString(R.string.locoList));
    }

    private void SwitchToFragment(Fragment newFragment,String title){
        if(getSupportActionBar() != null)
            getSupportActionBar().setTitle(title);
        mFragmentTransaction = getSupportFragmentManager().beginTransaction();
        mFragmentTransaction.remove(mCurrentFragment);
        mCurrentFragment = newFragment;
        mFragmentTransaction.add(R.id.MainFragment, mCurrentFragment);
        mFragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        assert getSupportActionBar() != null;
        switch (id) {
            case R.id.action_connect:
                mInterfaces.Connect(this);
                return true;
            case R.id.action_disconnect:
                mInterfaces.Disconnect();
                return true;
            case R.id.action_stop:
                if (!EmergencyStop) {
                    XpressNet.setPower(XpressNet.CS_TRACK_OFF);
                } else XpressNet.setPower(XpressNet.CS_NORMAL);
                return true;
            case R.id.action_turnout:
                mMenu.findItem(mCurrentTab).setVisible(true);
                mCurrentTab = R.id.action_turnout;
                mMenu.findItem(mCurrentTab).setVisible(false);
                SwitchToFragment(mTurnoutFragment,getString(R.string.accessory));
                return true;
            case R.id.action_cv:
                mMenu.findItem(mCurrentTab).setVisible(true);
                mCurrentTab = R.id.action_cv;
                mMenu.findItem(mCurrentTab).setVisible(false);
                SwitchToFragment(mDecoderChooseFragment,getString(R.string.decoderList));
                return true;
            case R.id.action_loco:
                mMenu.findItem(mCurrentTab).setVisible(true);
                mCurrentTab = R.id.action_loco;
                mMenu.findItem(mCurrentTab).setVisible(false);
                SwitchToFragment(mLocoFragment,getString(R.string.loco));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Interfaces.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            //Interfaces not enabled.
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void ChangeStatus(final String status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatus.setText(status);
            }
        });
    }

    private void setEmergencyButton(boolean state){
        if (state) {
            Animation mAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blinking_animation);
            findViewById(R.id.action_stop).startAnimation(mAnimation);
        } else findViewById(R.id.action_stop).clearAnimation();
    }

    private final Interfaces.ConnectionStatusCallback mConnectionStatusCallback = new Interfaces.ConnectionStatusCallback() {
        @Override
        public void onStatusChange(final int status) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch(status){
                        case Interfaces.STATE_DISCONNECTED:
                            mStatus.setText(getString(R.string.bt_disconnected));
                            break;
                        case Interfaces.STATE_CONNECTING:
                            mStatus.setText(getString(R.string.bt_connecting));
                            break;
                        case Interfaces.STATE_CONNECTED:
                            mStatus.setText(getString(R.string.bt_connected));
                            break;
                        case Interfaces.STATE_SEARCHING:
                            mStatus.setText(getString(R.string.bt_searching));
                            break;
                        case Interfaces.STATE_SERVICE_FOUNDED:
                            mStatus.setText(getString(R.string.bt_service_founded));
                            break;
                        case Interfaces.STATE_SERVICE_DISCOVERED:
                            mStatus.setText(getString(R.string.bt_service_discovered));
                            break;
                        case Interfaces.STATE_NOT_FOUNDED:
                            mStatus.setText(getString(R.string.bt_not_found));
                            break;
                        case Interfaces.STATE_NOT_BONDED:
                            mStatus.setText(getString(R.string.bt_not_bonded));
                            break;
                        case Interfaces.STATE_ADAPTER_ERROR:
                            mStatus.setText(getString(R.string.bt_adaptor_error));
                            break;
                    }
                }
            });
        }
    };

    private final Interfaces.DataCallback mDataCallback = new Interfaces.DataCallback() {
        @Override
        public void onDataReceived(final byte[] data) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    XpressNet.ParseReceivedData(data);
                }
            });
        }
    };

    @Override
    public boolean sendData(byte[] data) {
        return mInterfaces.SendData(data);
    }

    @Override
    public void notifyXNetPower(byte status) {
        if (status == XpressNet.CS_NORMAL){
            ChangeStatus("NORMAL");
            EmergencyStop = false;
            setEmergencyButton(false);
        }
        else if (status == XpressNet.CS_ESTOP){
            ChangeStatus("EMERGENCY STOP");
            EmergencyStop = true;
            setEmergencyButton(true);
        }
        else if (status == XpressNet.CS_TRACK_OFF)
        {
            ChangeStatus("TRACK OFF");
            EmergencyStop = true;
            setEmergencyButton(true);
        }
        else if (status == XpressNet.CS_TRACK_SHORTED){
            ChangeStatus("SHORT CIRCUIT");
            EmergencyStop = true;
            setEmergencyButton(true);
        }
        else if (status == XpressNet.CS_SERV_MODE){
            EmergencyStop = true;
            setEmergencyButton(true);
            ChangeStatus("SERVICE MODE");
        }
    }

    @Override
    public void notifyXNetExtControl(final short locoAddress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLocoFragment.SetLocoBusy(locoAddress);
            }
        });
    }

    @Override
    public void notifyXNetExtSpeed(final short locoAddress,final byte type,final short value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLocoFragment.SetLocoSpeed(locoAddress,type,value);
            }
        });
    }

    @Override
    public void notifyXNetExtFunc(final short locoAddress, final int funcMask, final int funcStatus){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLocoFragment.SetLocoFunc(locoAddress, funcMask, funcStatus);
            }
        });
    }

    @Override
    public void notifyXNetFeedback(short address, byte stateMask, byte state){

    }

    @Override
    public void notifyXNetService(boolean directMode, short CV, short value){
        if (mCurrentFragment == mCVChangeFragment)
            mCVChangeFragment.readCVcallback(directMode, CV, value);
        else if (mCurrentFragment == mCVManualFragment)
            mCVManualFragment.readCVcallback(directMode, CV, value);
    }

    @Override
    public void notifyXNetServiceError(){
        if (mCurrentFragment == mCVChangeFragment)
            mCVChangeFragment.readCVerrorCallback();
        else if (mCurrentFragment == mCVManualFragment)
            mCVManualFragment.readCVerrorCallback();
    }

    static void hideSoftKeyboard(Activity activity) {
        if (activity.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager =
                    (InputMethodManager) activity.getSystemService(
                            Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(
                    activity.getCurrentFocus().getWindowToken(), 0);
        }
    }
}
