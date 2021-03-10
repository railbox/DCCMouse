package com.dccmause.dccmause;

import android.content.ContentValues;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CVChangeFragment extends Fragment {
    private EditText mCV_Value;

    private DB mDB;

    private int bits_en;
    private int min_val;
    private int max_val;
    private int cur_val;
    private int cv_address;
    private static long currentCVID = 0;
    private ContentValues currentCVdata;

    private static final String ATT_BIT_DESC = "name";
    private static final String ATT_BIT_CHECKED = "checked";
    private static final String ATT_BIT_STATE = "state";
    private static final String ATT_BIT_NUM = "number";
    private SimpleAdapter sAdapter;
    private final ArrayList<Map<String, Object>> data = new ArrayList<>();

    public CVChangeFragment() {
        // Required empty public constructor
    }

    public void setCVID(long cvID){
        currentCVID = cvID;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView;
        mDB = ((MainActivity)getActivity()).getDB();

        currentCVdata = mDB.getRec(DB.CV_TABLE, currentCVID);
        bits_en = (int) currentCVdata.get(DB.CV_BITS_EN);
        min_val = (int) currentCVdata.get(DB.CV_MIN_VALUE);
        max_val = (int) currentCVdata.get(DB.CV_MAX_VALUE);
        cv_address = (int) currentCVdata.get(DB.CV_ADDRESS);
        cur_val = (int) currentCVdata.get(DB.CV_CUR_VALUE);
        int dft_val = (int) currentCVdata.get(DB.CV_DFL_VALUE);
        if (bits_en == 0) //The CV is a normal value
        {
            rootView = inflater.inflate(R.layout.fragment_cv_value, container, false);
            TextView mCV_Val_Min = (TextView)rootView.findViewById(R.id.CV_Val_Min);
            TextView mCV_Val_Max = (TextView)rootView.findViewById(R.id.CV_Val_Max);
            TextView mCV_Val_Min_Desc = (TextView)rootView.findViewById(R.id.CV_Val_Min_Desc);
            TextView mCV_Val_Max_Desc = (TextView)rootView.findViewById(R.id.CV_Val_Max_Desc);
            TextView mCV_Val_Dft = (TextView)rootView.findViewById(R.id.CV_Val_Dft);
            mCV_Val_Min.setText(String.valueOf(min_val));
            mCV_Val_Max.setText(String.valueOf(max_val));
            mCV_Val_Dft.setText(String.valueOf(dft_val));
            mCV_Val_Min_Desc.setText((String) currentCVdata.get(DB.CV_MIN_DESCRIPTION));
            mCV_Val_Max_Desc.setText((String) currentCVdata.get(DB.CV_MAX_DESCRIPTION));

            mCV_Value = (EditText) rootView.findViewById(R.id.CV_Val_Value);
            mCV_Value.setText(String.valueOf(cur_val));
        }
        else {
            rootView = inflater.inflate(R.layout.fragment_cv_bits, container, false);

            ListView mFunctionList = (ListView) rootView.findViewById(R.id.CV_Bits_List);
            String[] from = {ATT_BIT_CHECKED, ATT_BIT_STATE, ATT_BIT_DESC};
            int[] to = {R.id.CVBitsItemChk, R.id.CVBitsItemState, R.id.CVBitsItemDesc};
            sAdapter = new SimpleAdapter(getActivity(), data, R.layout.cv_bits_item, from, to);
            mFunctionList.setAdapter(sAdapter);
            mFunctionList.setOnItemClickListener(mListOnItemClick);

            data.clear();
            for (byte i = 0; i < 8; i++) {
                if ((bits_en & (1<<i)) != 0) {
                    String descCol = "bit"+i+"_desc";
                    String desc0Col = "bit"+i+"_0_desc";
                    String desc1Col = "bit"+i+"_1_desc";

                    Map<String, Object> m = new HashMap<>();
                    m.put(ATT_BIT_DESC, "bit"+i+":"+ currentCVdata.get(descCol));
                    m.put(ATT_BIT_NUM,i);
                    if ((cur_val & (1<<i)) != 0){
                        m.put(ATT_BIT_CHECKED, true);
                        m.put(ATT_BIT_STATE, currentCVdata.get(desc1Col));
                    }else{
                        m.put(ATT_BIT_CHECKED, false);
                        m.put(ATT_BIT_STATE, currentCVdata.get(desc0Col));
                    }
                    data.add(m);
                }
            }
            sAdapter.notifyDataSetChanged();
        }
        TextView mCV_Name = (TextView)rootView.findViewById(R.id.CV_Name);
        TextView mCV_Desc = (TextView)rootView.findViewById(R.id.CV_Desc);
        mCV_Name.setText((String) currentCVdata.get(DB.CV_NAME));
        mCV_Desc.setText((String) currentCVdata.get(DB.CV_DESCRIPTION));

        Button mCVWriteBtn = (Button) rootView.findViewById(R.id.CV_WriteBtn);
        mCVWriteBtn.setOnClickListener(CVWriteClick);
        Button mCVReadBtn = (Button) rootView.findViewById(R.id.CV_ReadBtn);
        mCVReadBtn.setOnClickListener(CVReadClick);
        Button mCancelBtn = (Button) rootView.findViewById(R.id.CV_CancelBtn);
        mCancelBtn.setOnClickListener(CancelClick);

        return rootView;
    }

    private final AdapterView.OnItemClickListener mListOnItemClick = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, final int position, final long id) {
            final Map<String, Object> m = data.get(position);
            byte bit_num = (byte)m.get(ATT_BIT_NUM);
            boolean checked = (boolean)m.get(ATT_BIT_CHECKED);
            checked = !checked;
            //Update local current value
            if (checked) cur_val |= 1<<bit_num;
            else cur_val  &= ~(1<<bit_num);
            m.put(ATT_BIT_CHECKED,checked);
            m.put(ATT_BIT_STATE, currentCVdata.get("bit"+bit_num+"_"+(checked?1:0)+"_desc"));
            sAdapter.notifyDataSetChanged();
        }
    };

    private final View.OnClickListener CVWriteClick = new View.OnClickListener() {
        public void onClick(View v) {
            MainActivity.hideSoftKeyboard(getActivity());
            if (bits_en == 0){
                cur_val = Short.decode(mCV_Value.getText().toString());
                if ((cur_val > max_val) || (cur_val < min_val)) { //Incorrect value provided
                    Toast toast = Toast.makeText(getActivity(), getActivity().getString(R.string.CVValueError),
                            Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return;
                }
            }

            currentCVdata.put(DB.CV_CUR_VALUE,cur_val);
            mDB.updateRec(DB.CV_TABLE, currentCVID,currentCVdata);
            XpressNet.setCV((short)cv_address,(byte)cur_val);
        }
    };

    private final View.OnClickListener CVReadClick = new View.OnClickListener() {
        public void onClick(View v) {
            MainActivity.hideSoftKeyboard(getActivity());
            XpressNet.requestReadCV((short)cv_address);
        }
    };

    private final View.OnClickListener CancelClick = new View.OnClickListener() {
        public void onClick(View v) {
            ((MainActivity)getActivity()).OpenCVList(-1);
        }
    };

    void readCVcallback(boolean directMode, short CV, short value) {
        if (CV == cv_address){
            cur_val = value;
            if (bits_en == 0) {
                mCV_Value.setText(String.valueOf(cur_val));
            }else {
                for (int i=0; i < data.size(); i++){
                    final Map<String, Object> m = data.get(i);
                    byte bit_num = (byte)m.get(ATT_BIT_NUM);
                    boolean checked = (cur_val & (1<<bit_num)) != 0;
                    m.put(ATT_BIT_CHECKED,checked);
                    m.put(ATT_BIT_STATE, currentCVdata.get("bit"+bit_num+"_"+(checked?1:0)+"_desc"));
                    sAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    void readCVerrorCallback(){
        Toast toast = Toast.makeText(getActivity(), getActivity().getString(R.string.CVReadError),
                Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
