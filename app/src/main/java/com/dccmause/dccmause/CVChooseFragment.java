package com.dccmause.dccmause;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
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

public class CVChooseFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    @SuppressWarnings("FieldCanBeLocal")
    private final int LoaderId = 3;
    private DB mDB;
    private SimpleCursorAdapter scAdapter;
    private static long currentDecoderID = 0;

    public CVChooseFragment() {
        // Required empty public constructor
    }

    public void setDecoderID(long decoderID){
        currentDecoderID = decoderID;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_cv_list, container, false);

        mDB = ((MainActivity)getActivity()).getDB();

        String[] from = {DB.CV_ADDRESS, DB.CV_NAME};
        int[] to = {R.id.CVItemAddress, R.id.CVItemName};
        scAdapter = new SimpleCursorAdapter(getActivity(), R.layout.cv_item, null, from, to, 1);
        ListView mListView = (ListView) rootView.findViewById(R.id.CVListView);
        mListView.setAdapter(scAdapter);
        mListView.setOnItemClickListener(mListOnItemClick);
        getActivity().getSupportLoaderManager().restartLoader(LoaderId, null, this);

        return rootView;
    }


    private final AdapterView.OnItemClickListener mListOnItemClick = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, final int position, final long id) {
            ((MainActivity)getActivity()).OpenCVEditor(id);
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bndl) {
        return new CVChooseFragment.MyCursorLoader(getActivity(), mDB);
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
            return db.getAllDataWithQuery(DB.CV_TABLE, DB.CV_DECODER_ID + " = " + currentDecoderID);
        }
    }
}
