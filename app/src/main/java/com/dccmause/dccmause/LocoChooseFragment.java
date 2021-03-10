package com.dccmause.dccmause;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class LocoChooseFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    @SuppressWarnings("FieldCanBeLocal")
    private final int LoaderId = 1;
    private DB mDB;
    private BitmapCursorAdapter scAdapter;
    // private SimpleCursorAdapter scAdapter;

    public LocoChooseFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_loco_list, container, false);

        mDB = ((MainActivity)getActivity()).getDB();

        String[] from = {DB.LOCO_IMAGE, DB.LOCO_TEXT, DB.LOCO_ADDR };
        int[] to = {R.id.LocoItemImg, R.id.LocoItemName, R.id.LocoItemAddr};
        scAdapter = new BitmapCursorAdapter(getActivity(), R.layout.loco_item, null, from, to, 1);
        ListView mListView = (ListView) rootView.findViewById(R.id.LocoListView);
        mListView.setAdapter(scAdapter);
        mListView.setOnItemClickListener(mListOnItemClick);
        mListView.setOnItemLongClickListener(mListOnItemLongClick);
        getActivity().getSupportLoaderManager().initLoader(LoaderId, null, this);

        FloatingActionButton mAddBtn = (FloatingActionButton)rootView.findViewById(R.id.LocoAddBtn);
        mAddBtn.setOnClickListener(mAddBtnOnClick);
        return rootView;
    }

    private final View.OnClickListener mAddBtnOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            ((MainActivity)getActivity()).OpenLocoEditor(-1);
        }
    };

    private final AdapterView.OnItemClickListener mListOnItemClick = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, final int position, final long id) {
            ((MainActivity)getActivity()).CloseLocoList(id);
        }
    };

    private final AdapterView.OnItemLongClickListener mListOnItemLongClick = new AdapterView.OnItemLongClickListener() {
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, final long id) {
            ((MainActivity)getActivity()).OpenLocoEditor(id);
            return true;
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bndl) {
        return new LocoChooseFragment.MyCursorLoader(getActivity(), mDB);
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
            return db.getAllData(DB.LOCO_TABLE);
        }
    }
}
