package com.dccmause.dccmause;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

public class TurnoutFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private final int LoaderId = 0;
    private DB mDB;
    private SimpleCursorAdapter scAdapter;

    public TurnoutFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_turnout, container, false);

        mDB = ((MainActivity)getActivity()).getDB();

        String[] from = {DB.ACC_IMAGE, DB.ACC_TEXT, DB.ACC_ADDR, DB.ACC_S_IMAGE };
        int[] to = {R.id.AccItemImg, R.id.AccItemName, R.id.AccItemAddress, R.id.AccItemState};
        scAdapter = new SimpleCursorAdapter(getActivity(), R.layout.acc_item, null, from, to, 1);
        ListView mListView = (ListView) rootView.findViewById(R.id.AccListView);
        mListView.setAdapter(scAdapter);
        mListView.setOnItemClickListener(mListOnItemClick);
        mListView.setOnItemLongClickListener(mListOnItemLongClick);
        getActivity().getSupportLoaderManager().initLoader(LoaderId, null, this);

        //registerForContextMenu(mListView);

        FloatingActionButton mAddBtn = (FloatingActionButton)rootView.findViewById(R.id.AccAddBtn);
        mAddBtn.setOnClickListener(mAddBtnOnClick);
        return rootView;
    }

    /*
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        Log.i("MyApp", "onCreateContextMenu");
        menu.add(0, CM_DELETE_ID, 0, "Удалить запись");
    }*/

    private final View.OnClickListener mAddBtnOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(getActivity());
            mAlertDialog.setTitle(getString(R.string.newAccessory));
            LayoutInflater mLayoutInflater = LayoutInflater.from(getActivity());
            final ViewGroup nullParent = null;
            View mDialogView = mLayoutInflater.inflate(R.layout.accessory_dialog, nullParent);

            final Spinner mAccTypeList = (Spinner)mDialogView.findViewById(R.id.DiagAccTypeList);
            final EditText mAccNameEdit = (EditText) mDialogView.findViewById(R.id.DiagAccName);
            final EditText mAccAddressEdit = (EditText) mDialogView.findViewById(R.id.DiagAccAddress);
            final CheckBox mAccInvertedChk = (CheckBox) mDialogView.findViewById(R.id.DiagAccInverted);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                    R.array.AccType, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mAccTypeList.setAdapter(adapter);

            mAlertDialog.setView(mDialogView);
            mAlertDialog.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            mAlertDialog.setPositiveButton(getString(R.string.create), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    String mAccName = mAccNameEdit.getText().toString();
                    String mAccAddress = mAccAddressEdit.getText().toString();
                    int mAccType = (int)mAccTypeList.getSelectedItemId();
                    boolean mAccInverted = mAccInvertedChk.isChecked();

                    ContentValues cv = new ContentValues();
                    cv.put(DB.ACC_IMAGE, DB.AccGetTypeImg(mAccType));
                    cv.put(DB.ACC_TEXT, mAccName);
                    cv.put(DB.ACC_ADDR, mAccAddress);
                    cv.put(DB.ACC_TYPE, mAccType);
                    cv.put(DB.ACC_STATE, 0);
                    cv.put(DB.ACC_S_IMAGE, DB.AccGetStateImg(mAccType, false));
                    cv.put(DB.ACC_INVERTED, mAccInverted ? 1 : 0);
                    mDB.addRec(DB.ACC_TABLE, cv);
                    getActivity().getSupportLoaderManager().getLoader(LoaderId).forceLoad();
                }
            });
            mAlertDialog.show();
        }
    };

    private final AdapterView.OnItemClickListener mListOnItemClick = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, final int position, final long id) {
            ContentValues cv = mDB.getRec(DB.ACC_TABLE, id);
            boolean CurState = (int)cv.get(DB.ACC_STATE) != 0;
            String AccAddressStr = (String)cv.get(DB.ACC_ADDR);
            boolean Inverted = (int)cv.get(DB.ACC_INVERTED) != 0;
            int mAccType = (int)cv.get(DB.ACC_TYPE);
            CurState = !CurState;
            cv.put(DB.ACC_STATE, CurState);
            cv.put(DB.ACC_S_IMAGE, DB.AccGetStateImg(mAccType, CurState));
            mDB.updateRec(DB.ACC_TABLE, id, cv);
            getActivity().getSupportLoaderManager().getLoader(LoaderId).forceLoad();

            short AccAddress = (short)Integer.parseInt(AccAddressStr);
            if (Inverted) CurState = !CurState;
            XpressNet.setTrntPos(AccAddress,CurState,true);
            XpressNet.setTrntPos(AccAddress,CurState,false);
        }
    };

    private final AdapterView.OnItemLongClickListener mListOnItemLongClick = new AdapterView.OnItemLongClickListener() {
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, final long id) {
            AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(getActivity());
            mAlertDialog.setTitle(getString(R.string.accessory));
            LayoutInflater mLayoutInflater = LayoutInflater.from(getActivity());
            View mDialogView = mLayoutInflater.inflate(R.layout.accessory_dialog, parent, false);

            final Spinner mAccTypeList = (Spinner) mDialogView.findViewById(R.id.DiagAccTypeList);
            final EditText mAccNameEdit = (EditText) mDialogView.findViewById(R.id.DiagAccName);
            final EditText mAccAddressEdit = (EditText) mDialogView.findViewById(R.id.DiagAccAddress);
            final CheckBox mAccInvertedChk = (CheckBox) mDialogView.findViewById(R.id.DiagAccInverted);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                    R.array.AccType, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mAccTypeList.setAdapter(adapter);

            ContentValues cv = mDB.getRec(DB.ACC_TABLE, id);
            mAccNameEdit.setText((String)cv.get(DB.ACC_TEXT));
            mAccAddressEdit.setText((String)cv.get(DB.ACC_ADDR));
            mAccTypeList.setSelection((int)cv.get(DB.ACC_TYPE));
            mAccInvertedChk.setChecked((int)cv.get(DB.ACC_INVERTED) != 0);

            mAlertDialog.setView(mDialogView);
            mAlertDialog.setNegativeButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    mDB.delRec(DB.ACC_TABLE,id);
                    getActivity().getSupportLoaderManager().getLoader(LoaderId).forceLoad();
                }
            });
            mAlertDialog.setPositiveButton(getString(R.string.change), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    String mAccName = mAccNameEdit.getText().toString();
                    String mAccAddress = mAccAddressEdit.getText().toString();
                    int mAccType = (int) mAccTypeList.getSelectedItemId();
                    boolean mAccInverted = mAccInvertedChk.isChecked();

                    ContentValues cv_local = new ContentValues();
                    cv_local.put(DB.ACC_IMAGE, DB.AccGetTypeImg(mAccType));
                    cv_local.put(DB.ACC_TEXT, mAccName);
                    cv_local.put(DB.ACC_ADDR, mAccAddress);
                    cv_local.put(DB.ACC_TYPE, mAccType);
                    cv_local.put(DB.ACC_STATE, 0);
                    cv_local.put(DB.ACC_S_IMAGE, DB.AccGetStateImg(mAccType,false));
                    cv_local.put(DB.ACC_INVERTED, mAccInverted ? 1 : 0);
                    mDB.updateRec(DB.ACC_TABLE, id, cv_local);
                    getActivity().getSupportLoaderManager().getLoader(LoaderId).forceLoad();
                }
            });
            mAlertDialog.show();
            return true;
        }
    };

    /*@Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == CM_DELETE_ID) {
            // получаем инфу о пункте списка

            return true;
        }
        return super.onContextItemSelected(item);
    }*/
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bndl) {
        return new MyCursorLoader(getActivity(), mDB);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        scAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private static class MyCursorLoader extends CursorLoader {
        final DB db;

        MyCursorLoader(Context context, DB db) {
            super(context);
            this.db = db;
        }

        @Override
        public Cursor loadInBackground() {

            return db.getAllData(DB.ACC_TABLE);
        }

    }
}
