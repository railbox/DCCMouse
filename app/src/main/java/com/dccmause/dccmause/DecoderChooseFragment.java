package com.dccmause.dccmause;

import android.content.Context;
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
import android.widget.ListView;
import android.widget.TextView;

public class DecoderChooseFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    @SuppressWarnings("FieldCanBeLocal")
    private final int LoaderId = 2;
    private DB mDB;
    private SimpleCursorAdapter scAdapter;

    public DecoderChooseFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_decoder_list, container, false);

        mDB = ((MainActivity)getActivity()).getDB();

        String[] from = {DB.DECODER_NAME, DB.DECODER_MANUFACTURER};
        int[] to = {R.id.DecoderItemName, R.id.DecoderItemManufacturer};
        scAdapter = new SimpleCursorAdapter(getActivity(), R.layout.decoder_item, null, from, to, 1);
        ListView mListView = (ListView) rootView.findViewById(R.id.DecoderListView);
        mListView.setAdapter(scAdapter);
        mListView.setOnItemClickListener(mListOnItemClick);
        mListView.setOnItemLongClickListener(mListOnItemLongClick);
        getActivity().getSupportLoaderManager().initLoader(LoaderId, null, this);

        FloatingActionButton mAddBtn = (FloatingActionButton)rootView.findViewById(R.id.DecoderAddBtn);
        mAddBtn.setOnClickListener(mAddBtnOnClick);
        TextView mManualCVBtn = (TextView)rootView.findViewById(R.id.ManualCVBtn);
        mManualCVBtn.setOnClickListener(mManualCVBtnOnClick);
        return rootView;
    }

    private final View.OnClickListener mAddBtnOnClick = new View.OnClickListener() {
        public void onClick(View v) {
          //Perform loading from csv
        }
    };

    private final View.OnClickListener mManualCVBtnOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            ((MainActivity)getActivity()).OpenManualCVEditor();
        }
    };

    private final AdapterView.OnItemClickListener mListOnItemClick = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, final int position, final long id) {
            ((MainActivity)getActivity()).OpenCVList(id);
        }
    };

    private final AdapterView.OnItemLongClickListener mListOnItemLongClick = new AdapterView.OnItemLongClickListener() {
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, final long id) {

            return true;
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bndl) {
        return new DecoderChooseFragment.MyCursorLoader(getActivity(), mDB);
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
            return db.getAllData(DB.DECODERS_TABLE);
        }
    }
}
