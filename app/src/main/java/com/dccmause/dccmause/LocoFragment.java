package com.dccmause.dccmause;

import android.content.ContentValues;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.abs;

public class LocoFragment extends Fragment {
    private DB mDB;
    private ImageView mLocoImg;
    private ImageView mLocoNextImg;
    private ImageView mLocoPrevImg;
    private TextView mLocoName,mLocoSpeed;
    private ImageView mRotary;
    private ImageView mLocoLeft;
    private ImageView mLocoRight;
    private GridView mFuncGrid;
    private SimpleAdapter sAdapter;

    private long CurrentLocoDBid;
    private static final int ROT_MAX_ANGLE = 240;
    private static final int ROT_MIN_ANGLE = 10;
    private static final int ROT_ANGLE_SHIFT = 10;
    private static final int LOCO_MAX_SPEED = 128;
    private short CurRotaryAngle = 0;
    private byte Direction = 1;
    private byte LastLocoSpeed = 0;
    private short LocoAddr = 0;
    private boolean ExternalControl = false;
    private final ArrayList<Map<String, Object>> data = new ArrayList<>();

    private final String ATT_NAME_TEXT = "text";
    private final String ATT_NAME_IMAGE = "image";
    private final String ATT_NAME_ID = "id";
    private final String ATT_NAME_TYPE = "type";

    public LocoFragment() {
        // Required empty public constructor
    }

    void SetLocoBusy(short locoAddress){
        if ((LocoAddr == locoAddress) && (!ExternalControl)){
            ExternalControl = true;
            /*AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
            alertDialog.setTitle(getString(R.string.Warning));
            alertDialog.setMessage(getString(R.string.ExtControl));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();*/
        }
    }

    void SetLocoSpeed(short locoAddress, byte steps, short value){
        if (LocoAddr == locoAddress){
            if ((value & 0x7F) != 0) {
                CurRotaryAngle = (short) (ROT_MIN_ANGLE + (ROT_MAX_ANGLE - ROT_MIN_ANGLE) * (value & 0x7F) / (steps - 1));
                if ((value & 0x80) == 0) SetDirLeft();
                else SetDirRight();
            }else CurRotaryAngle = 0;
            SetSpeed();
            mRotary.setRotation(CurRotaryAngle+ROT_ANGLE_SHIFT);
        }
    }

    void SetLocoFunc(short locoAddress, int funcMask, int funcStatus){
        if (LocoAddr == locoAddress){
            //Update value in DB
            ContentValues cv = mDB.getRec(DB.LOCO_TABLE, CurrentLocoDBid);
            int FunctionStates = (int)cv.get(DB.LOCO_FUNC_STS);
            int NewFunctionStates = FunctionStates & (~funcMask);
            NewFunctionStates |= funcStatus;
            cv.put(DB.LOCO_FUNC_STS, NewFunctionStates);
            mDB.updateRec(DB.LOCO_TABLE, CurrentLocoDBid, cv);
            // Update picture
            int FunctionEnable = (int)cv.get(DB.LOCO_FUNC_EN);
            int FunctionChanged = NewFunctionStates ^ FunctionStates;
            byte position=0;
            for (byte i = 0; i < DB.FUNC_FUNC_COUNT; i++) {
                if ((FunctionEnable & (1<<i)) != 0) {
                    if ((FunctionChanged & (1<<i)) != 0) {
                        Map<String, Object> m = data.get(position);
                        byte FuncType = (byte) m.get(ATT_NAME_TYPE);
                        m.put(ATT_NAME_IMAGE, DB.FuncGetStateImg(FuncType, (NewFunctionStates & (1<<i)) != 0));
                    }
                    position++;
                }
            }
            sAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_loco, container, false);

        mDB = ((MainActivity)getActivity()).getDB();

        mFuncGrid = (GridView) rootView.findViewById(R.id.FuncGridView);

        String[] from = {ATT_NAME_IMAGE, ATT_NAME_TEXT};
        int[] to = {R.id.MainFuncImg, R.id.MainFuncText};
        sAdapter = new SimpleAdapter(getActivity(), data, R.layout.function_main_item, from, to);
        mFuncGrid.setAdapter(sAdapter);
        mFuncGrid.setOnItemClickListener(mFunctionOnClick);

        mLocoName = (TextView)rootView.findViewById(R.id.LocoName);
        mLocoSpeed = (TextView)rootView.findViewById(R.id.LocoSpeed);
        mLocoImg = (ImageView)rootView.findViewById(R.id.LocoImg);
        mLocoPrevImg = (ImageView)rootView.findViewById(R.id.LocoPrevImg);
        mLocoNextImg = (ImageView)rootView.findViewById(R.id.LocoNextImg);
        mLocoLeft = (ImageView)rootView.findViewById(R.id.btn_left);
        mLocoRight = (ImageView)rootView.findViewById(R.id.btn_right);
        mLocoImg.setOnClickListener(mLocoImgClick);
        mLocoPrevImg.setOnClickListener(mLocoPrevImgClick);
        mLocoNextImg.setOnClickListener(mLocoNextImgClick);
        mLocoLeft.setOnClickListener(mLocoLeftClick);
        mLocoRight.setOnClickListener(mLocoRightClick);

        mRotary = (ImageView)rootView.findViewById(R.id.Rotary);
        mRotary.setOnTouchListener(mRotaryOnTouch);

        LoadLoco(((MainActivity)getActivity()).GetLongPreference(MainActivity.SF_LOCOID));

        return rootView;
    }

    private final View.OnTouchListener mRotaryOnTouch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction() == MotionEvent.ACTION_MOVE){

                double r = Math.atan2(event.getX() - mRotary.getWidth() / 2, mRotary.getHeight() / 2 - event.getY());
                CurRotaryAngle += (int) Math.toDegrees(r)+130;
                if (CurRotaryAngle > ROT_MAX_ANGLE) CurRotaryAngle = ROT_MAX_ANGLE;
                else if (CurRotaryAngle < ROT_MIN_ANGLE) CurRotaryAngle = 0;
                mRotary.setRotation(CurRotaryAngle+ROT_ANGLE_SHIFT);
                SetSpeed();
                byte CurLocoSpeed;
                if (CurRotaryAngle > ROT_MIN_ANGLE)
                    CurLocoSpeed = (byte)((abs(CurRotaryAngle) - ROT_MIN_ANGLE)*(LOCO_MAX_SPEED-1)/(ROT_MAX_ANGLE-ROT_MIN_ANGLE));
                else CurLocoSpeed = 0;
                if (Direction <= 0) CurLocoSpeed |= 0x80;
                if (CurLocoSpeed != LastLocoSpeed){
                    LastLocoSpeed = CurLocoSpeed;
                    SendSpeed();
                }
            }
            return true;
        }
    };

    private final GridView.OnItemClickListener mFunctionOnClick = new GridView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v,
                                int position, long id) {

            Map<String, Object> m = data.get(position);
            byte FuncID = (byte)m.get(ATT_NAME_ID);
            byte FuncType = (byte)m.get(ATT_NAME_TYPE);
            ContentValues cv = mDB.getRec(DB.LOCO_TABLE, CurrentLocoDBid);
            int FunctionStates = (int)cv.get(DB.LOCO_FUNC_STS);
            boolean State = (FunctionStates  & (1<<FuncID)) != 0;
            State = !State;
            if (State) FunctionStates |= 1<<FuncID;
            else FunctionStates &=~ 1<<FuncID;
            cv.put(DB.LOCO_FUNC_STS, FunctionStates);
            mDB.updateRec(DB.LOCO_TABLE, CurrentLocoDBid, cv);

            m.put(ATT_NAME_IMAGE,DB.FuncGetStateImg(FuncType,State));
            sAdapter.notifyDataSetChanged();
            XpressNet.setLocoFunc(LocoAddr, FuncID, FunctionStates);
            ExternalControl = false;
        }
    };

    private final View.OnClickListener mLocoImgClick = new View.OnClickListener() {
        public void onClick(View v) {
            ((MainActivity)getActivity()).OpenLocoList();
        }
    };

    private final View.OnClickListener mLocoPrevImgClick = new View.OnClickListener() {
        public void onClick(View v) {
            long currentId = ((MainActivity)getActivity()).GetLongPreference(MainActivity.SF_LOCOID);
            currentId--;
            ContentValues cv = mDB.getRec(DB.LOCO_TABLE, currentId);
            if (cv != null) {
                ((MainActivity)getActivity()).SetLongPreference(MainActivity.SF_LOCOID, currentId);
                LoadLoco(currentId);
            }
        }
    };

    private final View.OnClickListener mLocoNextImgClick = new View.OnClickListener() {
        public void onClick(View v) {
            long currentId = ((MainActivity)getActivity()).GetLongPreference(MainActivity.SF_LOCOID);
            currentId++;
            ContentValues cv = mDB.getRec(DB.LOCO_TABLE, currentId);
            if (cv != null) {
                ((MainActivity)getActivity()).SetLongPreference(MainActivity.SF_LOCOID, currentId);
                LoadLoco(currentId);
            }
        }
    };

    private void SendSpeed(){
        if (Direction <= 0) LastLocoSpeed |= 0x80;
        else LastLocoSpeed &= 0x7F;
        XpressNet.setSpeed(LocoAddr,(byte)LOCO_MAX_SPEED,LastLocoSpeed);

        ContentValues cv = mDB.getRec(DB.LOCO_TABLE, CurrentLocoDBid);
        if (cv!=null) {
            cv.put(DB.LOCO_SPEED_POS, CurRotaryAngle * Direction);
            mDB.updateRec(DB.LOCO_TABLE, CurrentLocoDBid, cv);
        }
        ExternalControl = false;
    }


    private void SetSpeed(){
        byte SpeedPercentage = 0;
        if (CurRotaryAngle != 0)
            SpeedPercentage = (byte)((abs(CurRotaryAngle) - ROT_MIN_ANGLE)*100/(ROT_MAX_ANGLE-ROT_MIN_ANGLE));
        String str = String.valueOf(SpeedPercentage)+'%';
        mLocoSpeed.setText(str);
    }

    private void SetDirLeft(){
        Direction = 1;
        mLocoLeft.setImageDrawable(getActivity().getDrawable(R.drawable.btn_left_on));
        mLocoRight.setImageDrawable(getActivity().getDrawable(R.drawable.btn_right_off));
    }

    private void SetDirRight() {
        Direction = -1;
        mLocoRight.setImageDrawable(getActivity().getDrawable(R.drawable.btn_right_on));
        mLocoLeft.setImageDrawable(getActivity().getDrawable(R.drawable.btn_left_off));
    }


    private final View.OnClickListener mLocoLeftClick = new View.OnClickListener() {
        public void onClick(View v) {
            SetDirLeft();
            SendSpeed();
        }
    };

    private final View.OnClickListener mLocoRightClick = new View.OnClickListener() {
        public void onClick(View v) {
            SetDirRight();
            SendSpeed();
        }
    };

    private void LoadLoco(long id){
        boolean Loaded = false;
        if (id != -1) {
            ContentValues cv = mDB.getRec(DB.LOCO_TABLE, id);
            if (cv != null) {
                LocoAddr = Short.decode((String) cv.get(DB.LOCO_ADDR));
                byte[] ImageArray = (byte[])cv.get(DB.LOCO_IMAGE);
                mLocoImg.setImageBitmap(BitmapFactory.decodeByteArray(ImageArray,0,ImageArray.length));
                mLocoName.setText((String) cv.get(DB.LOCO_TEXT));
                int FunctionStates = (int)cv.get(DB.LOCO_FUNC_STS);
                int FunctionEnable = (int)cv.get(DB.LOCO_FUNC_EN);
                int SpeedPos = (int)cv.get(DB.LOCO_SPEED_POS);
                byte[] FuncArray = (byte[])cv.get(DB.LOCO_FUNC_TYPE_ARR);

                data.clear();
                for (byte i = 0; i < DB.FUNC_FUNC_COUNT; i++) {
                    if ((FunctionEnable & (1<<i)) != 0) {
                        boolean FuncState = (FunctionStates & (1<<i)) != 0;
                        Map<String, Object> m = new HashMap<>();
                        m.put(ATT_NAME_TEXT, "F" + i);
                        m.put(ATT_NAME_IMAGE, DB.FuncGetStateImg(FuncArray[i * DB.FUNC_TYPE_ISIZE],FuncState));
                        m.put(ATT_NAME_ID, i);
                        m.put(ATT_NAME_TYPE, FuncArray[i * DB.FUNC_TYPE_ISIZE]);
                        data.add(m);
                    }
                }
                sAdapter.notifyDataSetChanged();

                mFuncGrid.setVisibility(View.VISIBLE);
                mRotary.setVisibility(View.VISIBLE);
                mLocoLeft.setVisibility(View.VISIBLE);
                mLocoRight.setVisibility(View.VISIBLE);
                CurRotaryAngle = (short)abs(SpeedPos);
                if (SpeedPos > 0) SetDirLeft();
                else SetDirRight();
                SetSpeed();
                mRotary.setRotation(CurRotaryAngle+ROT_ANGLE_SHIFT);
                CurrentLocoDBid = id;
                Loaded = true;
            }
            cv = mDB.getRec(DB.LOCO_TABLE, id-1);
            if (cv != null) {
                byte[] ImageArray = (byte[])cv.get(DB.LOCO_IMAGE);
                mLocoPrevImg.setImageBitmap(BitmapFactory.decodeByteArray(ImageArray,0,ImageArray.length));
            }
            else mLocoPrevImg.setImageResource(0);
            cv = mDB.getRec(DB.LOCO_TABLE, id+1);
            if (cv != null) {
                byte[] ImageArray = (byte[])cv.get(DB.LOCO_IMAGE);
                mLocoNextImg.setImageBitmap(BitmapFactory.decodeByteArray(ImageArray,0,ImageArray.length));
            }
            else mLocoNextImg.setImageResource(0);
        }
        if (!Loaded) {
            CurrentLocoDBid = -1;
            mLocoImg.setImageDrawable(getActivity().getDrawable(R.drawable.loco_add));
            mLocoName.setText("");
            mFuncGrid.setVisibility(View.INVISIBLE);
            mRotary.setVisibility(View.INVISIBLE);
            mLocoLeft.setVisibility(View.INVISIBLE);
            mLocoRight.setVisibility(View.INVISIBLE);
        }
    }
}
