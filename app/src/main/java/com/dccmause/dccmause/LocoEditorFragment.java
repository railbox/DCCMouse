package com.dccmause.dccmause;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LocoEditorFragment extends Fragment {
    private final int LoaderId = 1;
    private long currentLocoID;

    private DB mDB;
    private ImageView mLocoImage;
    private EditText mLocoName;
    private EditText mLocoAddress;
    private String NameStr;
    private String AddressStr;

    private static final int REQUEST_PHOTO = 2;
    private static final int REQUEST_IMAGE = 3;

    private final String ATT_NAME_TEXT = "text";
    private final String ATT_NAME_CHECKED = "checked";
    private final String ATT_NAME_IMAGE = "image";
    private final String ATT_NAME_TYPE = "type";
    private SimpleAdapter sAdapter;
    private final ArrayList<Map<String, Object>> data = new ArrayList<>();

    public LocoEditorFragment() {
        // Required empty public constructor
    }

    public void setLocoID(long LocoID){
        currentLocoID = LocoID;
    }

    @Override
    public void onStart(){
        mLocoName.setText(NameStr);
        mLocoAddress.setText(AddressStr);
        super.onStart();
    }

    @Override
    public void onStop(){
        NameStr = mLocoName.getText().toString();
        AddressStr = mLocoAddress.getText().toString();
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_loco_editor, container, false);

        mDB = ((MainActivity)getActivity()).getDB();

        mLocoImage = (ImageView) rootView.findViewById(R.id.EditorLocoImage);
        mLocoName = (EditText) rootView.findViewById(R.id.EditorLocoName);
        mLocoAddress = (EditText) rootView.findViewById(R.id.EditorLocoAddress);
        Button mDelete = (Button) rootView.findViewById(R.id.EditorDelete);
        Button mApply = (Button) rootView.findViewById(R.id.EditorApply);
        ListView mFunctionList = (ListView) rootView.findViewById(R.id.EditorFunctionsList);
        mDelete.setOnClickListener(mDeleteOnClick);
        mApply.setOnClickListener(mApplyOnClick);
        mLocoImage.setOnClickListener(mImageOnClick);

        String[] from = { ATT_NAME_IMAGE, ATT_NAME_TEXT, ATT_NAME_CHECKED};
        int[] to = {R.id.FuncItemImg, R.id.FuncItemName, R.id.FuncItemEn};
        sAdapter = new SimpleAdapter(getActivity(), data, R.layout.function_item, from, to);
        mFunctionList.setAdapter(sAdapter);
        mFunctionList.setOnItemClickListener(mListOnItemClick);

        data.clear();
        if (currentLocoID == -1) {
            mLocoImage.setImageDrawable(getActivity().getDrawable(R.drawable.loco_add));
            NameStr = "";
            AddressStr = "";

            for (int i = 0; i < DB.FUNC_FUNC_COUNT; i++) {
                Map<String, Object> m = new HashMap<>();
                m.put(ATT_NAME_TEXT, "Function "+i);
                m.put(ATT_NAME_CHECKED, i<10);
                m.put(ATT_NAME_IMAGE, DB.FuncGetStateImg(0,false));
                m.put(ATT_NAME_TYPE, (byte)0);
                data.add(m);
            }
        }else {
            ContentValues cv = mDB.getRec(DB.LOCO_TABLE, currentLocoID);
            byte[] ImageArray = (byte[])cv.get(DB.LOCO_IMAGE);
            mLocoImage.setImageBitmap(BitmapFactory.decodeByteArray(ImageArray,0,ImageArray.length));
            NameStr = (String)cv.get(DB.LOCO_TEXT);
            AddressStr = (String)cv.get(DB.LOCO_ADDR);
            int FunctionsEnable = (int)cv.get(DB.LOCO_FUNC_EN);
            byte[] FuncArray = (byte[])cv.get(DB.LOCO_FUNC_TYPE_ARR);
            for (byte i = 0; i < DB.FUNC_FUNC_COUNT; i++) {
                String name = new String(FuncArray, i * DB.FUNC_TYPE_ISIZE + 1, DB.FUNC_TYPE_ISIZE - 1);
                Map<String, Object> m = new HashMap<>();
                m.put(ATT_NAME_CHECKED, (FunctionsEnable & (1 << i)) != 0);
                m.put(ATT_NAME_IMAGE, DB.FuncGetStateImg(FuncArray[i * DB.FUNC_TYPE_ISIZE],false));
                m.put(ATT_NAME_TEXT, name);
                m.put(ATT_NAME_TYPE, FuncArray[i * DB.FUNC_TYPE_ISIZE]);
                data.add(m);
            }
        }
        sAdapter.notifyDataSetChanged();

        return rootView;
    }

    private final AdapterView.OnItemClickListener mListOnItemClick = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, final int position, final long id) {
            AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(getActivity());
            mAlertDialog.setTitle(getString(R.string.function));
            LayoutInflater mLayoutInflater = LayoutInflater.from(getActivity());
            View mDialogView = mLayoutInflater.inflate(R.layout.function_dialog, parent, false);

            final Spinner mFuncTypeList = (Spinner) mDialogView.findViewById(R.id.DiagFuncTypeList);
            final EditText mFuncNameEdit = (EditText) mDialogView.findViewById(R.id.DiagFuncName);
            final CheckBox mFuncEnableChk = (CheckBox) mDialogView.findViewById(R.id.DiagFuncEn);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                    R.array.FuncType, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mFuncTypeList.setAdapter(adapter);

            final Map<String, Object> m = data.get(position);
            mFuncNameEdit.setText((String)m.get(ATT_NAME_TEXT));
            mFuncTypeList.setSelection((byte)m.get(ATT_NAME_TYPE));
            mFuncEnableChk.setChecked((boolean)m.get(ATT_NAME_CHECKED));

            mAlertDialog.setView(mDialogView);
            mAlertDialog.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            mAlertDialog.setPositiveButton(getString(R.string.change), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    String mAccName = mFuncNameEdit.getText().toString();
                    byte mAccType = (byte) mFuncTypeList.getSelectedItemId();
                    boolean mAccInverted = mFuncEnableChk.isChecked();

                    m.put(ATT_NAME_TEXT, mAccName);
                    m.put(ATT_NAME_CHECKED, mAccInverted);
                    m.put(ATT_NAME_IMAGE, DB.FuncGetStateImg(mAccType,false));
                    m.put(ATT_NAME_TYPE, mAccType);
                    sAdapter.notifyDataSetChanged();
                }
            });
            mAlertDialog.show();
        }
    };

    private final View.OnClickListener mImageOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            final CharSequence[] items = new CharSequence[2];
            items[0] = getString(R.string.fromCamera);
            items[1] = getString(R.string.fromGallery);
            AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(getActivity());
            mAlertDialog.setTitle(getString(R.string.imageSource));
            mAlertDialog.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    if (item == 0) {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        //intent.putExtra(MediaStore.EXTRA_OUTPUT, generateFileUri(TYPE_PHOTO));
                        startActivityForResult(intent, REQUEST_PHOTO);
                    }
                    else if (item == 1){
                        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                        i.setType("image/*");
                        startActivityForResult(i, REQUEST_IMAGE);
                    }
                }});
            mAlertDialog.show();

        }
    };
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        //super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                if ((intent != null) && (intent.getExtras() != null)) {
                    Object obj = intent.getExtras().get("data");
                    if (obj instanceof Bitmap) {
                        Bitmap bmp = (Bitmap) obj;
                        Bitmap cropped_bmp;
                        if (bmp.getHeight() > bmp.getWidth())
                            cropped_bmp = Bitmap.createBitmap(bmp, 0, (bmp.getHeight()-bmp.getWidth())/2, bmp.getWidth(), bmp.getWidth());
                        else cropped_bmp = Bitmap.createBitmap(bmp, (bmp.getWidth()-bmp.getHeight())/2, 0, bmp.getHeight(), bmp.getHeight());
                        mLocoImage.setImageBitmap(cropped_bmp);
                    }
                }else Log.e("LocoEditor", "Request photo intent is null");
            }else Log.i("LocoEditor", "Request photo result not ok");
        }
        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                    Uri selectedImg = intent.getData();
                    try {
                        Bitmap bmp = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), selectedImg);
                        Bitmap cropped_bmp;
                        if (bmp.getHeight() > bmp.getWidth())
                            cropped_bmp = Bitmap.createBitmap(bmp, 0, (bmp.getHeight()-bmp.getWidth())/2, bmp.getWidth(), bmp.getWidth());
                        else cropped_bmp = Bitmap.createBitmap(bmp, (bmp.getWidth()-bmp.getHeight())/2, 0, bmp.getHeight(), bmp.getHeight());

                        mLocoImage.setImageBitmap(Bitmap.createScaledBitmap(cropped_bmp, 240, 240, false));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }else Log.i("LocoEditor", "Request image result not ok");
        }
    }

    private final View.OnClickListener mDeleteOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            if (currentLocoID != -1)
                mDB.delRec(DB.LOCO_TABLE,currentLocoID);
            mLocoAddress.clearFocus();
            mLocoName.clearFocus();
            getActivity().getSupportLoaderManager().getLoader(LoaderId).forceLoad();
            ((MainActivity)getActivity()).CloseLocoEditor();
        }
    };

    private final View.OnClickListener mApplyOnClick = new View.OnClickListener() {
        public void onClick(View v) {

            ContentValues cv;
            if (currentLocoID == -1) cv = new ContentValues();
            else cv = mDB.getRec(DB.LOCO_TABLE, currentLocoID);
            Bitmap bitmap = ((BitmapDrawable)mLocoImage.getDrawable()).getBitmap();
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            cv.put(DB.LOCO_IMAGE, byteStream.toByteArray());
            cv.put(DB.LOCO_TEXT, mLocoName.getText().toString());
            cv.put(DB.LOCO_ADDR, mLocoAddress.getText().toString());
            int FunctionState = 0;
            byte[] FuncArray = new byte[DB.FUNC_TYPE_ARR_SIZE];
            for (int i = 0; i < DB.FUNC_FUNC_COUNT; i++) {
                Map<String, Object> m = data.get(i);
                byte[] NameStr = ((String)m.get(ATT_NAME_TEXT)).getBytes();
                if ((boolean)m.get(ATT_NAME_CHECKED)) FunctionState |= 1<<i;
                FuncArray[i * DB.FUNC_TYPE_ISIZE] = (byte)m.get(ATT_NAME_TYPE);
                System.arraycopy(NameStr, 0, FuncArray, i * DB.FUNC_TYPE_ISIZE + 1,
                        NameStr.length < DB.FUNC_TYPE_ISIZE - 1 ? NameStr.length : DB.FUNC_TYPE_ISIZE - 1);
            }
            cv.put(DB.LOCO_FUNC_TYPE_ARR, FuncArray);
            cv.put(DB.LOCO_FUNC_EN, FunctionState);
            if (currentLocoID == -1) {
                cv.put(DB.LOCO_SPEED_POS, 0);
                cv.put(DB.LOCO_FUNC_STS, 0);
                mDB.addRec(DB.LOCO_TABLE, cv);
            }else{
                mDB.updateRec(DB.LOCO_TABLE, currentLocoID, cv);
            }
            mLocoAddress.clearFocus();
            mLocoName.clearFocus();
            getActivity().getSupportLoaderManager().getLoader(LoaderId).forceLoad();
            ((MainActivity)getActivity()).CloseLocoEditor();
        }
    };
}
