package com.dccmause.dccmause;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class CVManualFragment extends Fragment {

    private EditText mCVAddress;
    private EditText mCVValue;


    public CVManualFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_manual_cv, container, false);

        mCVAddress = (EditText)rootView.findViewById(R.id.CVAddress);
        mCVValue = (EditText)rootView.findViewById(R.id.CVValue);
        Button mCVWriteBtn = (Button) rootView.findViewById(R.id.CV_WriteBtn);
        mCVWriteBtn.setOnClickListener(CVWriteClick);
        Button mCVReadBtn = (Button) rootView.findViewById(R.id.CV_ReadBtn);
        mCVReadBtn.setOnClickListener(CVReadClick);
        Button mCancelBtn = (Button) rootView.findViewById(R.id.CV_CancelBtn);
        mCancelBtn.setOnClickListener(CancelClick);
        return rootView;
    }

    private final View.OnClickListener CVWriteClick = new View.OnClickListener() {
        public void onClick(View v) {
            MainActivity.hideSoftKeyboard(getActivity());
            short CVAddr = Short.decode(mCVAddress.getText().toString());
            short CVValue = Short.decode(mCVValue.getText().toString());
            XpressNet.setCV(CVAddr,(byte)CVValue);
        }
    };

    private final View.OnClickListener CVReadClick = new View.OnClickListener() {
        public void onClick(View v) {
            MainActivity.hideSoftKeyboard(getActivity());
            short CVAddr = Short.decode(mCVAddress.getText().toString());
            XpressNet.requestReadCV(CVAddr);
        }
    };

    private final View.OnClickListener CancelClick = new View.OnClickListener() {
        public void onClick(View v) {
            MainActivity.hideSoftKeyboard(getActivity());
            ((MainActivity)getActivity()).OpenDecoderList();
        }
    };

    void readCVcallback(boolean directMode, short CV, short value) {
        short CVAddr = Short.decode(mCVAddress.getText().toString());
        if (CV == CVAddr) mCVValue.setText(String.valueOf(value));
    }

    void readCVerrorCallback(){
        Toast toast = Toast.makeText(getActivity(), getActivity().getString(R.string.CVReadError),
                Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
