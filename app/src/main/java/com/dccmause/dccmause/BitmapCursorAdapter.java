package com.dccmause.dccmause;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

class BitmapCursorAdapter extends SimpleCursorAdapter {

    private Cursor c;
    private final Context context;
    private final String[] from;
    private final int[] to;
    private final int layout;

    BitmapCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        this.context = context;
        this.layout = layout;
        this.c = c;
        this.from = from;
        this.to = to;
    }

    @Override
    public Cursor swapCursor(Cursor c){
        this.c = c;
        return super.swapCursor(c);
    }

    @Override
    public View getView(int pos, View inView, ViewGroup parent) {
        View v = inView;
        if (v == null) {
            v = ((LayoutInflater)context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, parent, false);
        }
        if ((c != null) && (c.moveToPosition(pos))) {
            for (int i=0;i<to.length;i++)
            {
                int column = c.getColumnIndex(from[i]);
                switch (c.getType(column)) {
                    case Cursor.FIELD_TYPE_STRING:
                        ((TextView)v.findViewById(to[i])).setText(c.getString(column));
                        break;
                    case Cursor.FIELD_TYPE_BLOB:
                        byte[] image = c.getBlob(column);
                        if (image != null) {
                            ((ImageView) v.findViewById(to[i])).setImageBitmap(
                                    BitmapFactory.decodeByteArray(image, 0, image.length));
                        }
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        ((ImageView) v.findViewById(to[i])).setImageDrawable(context.getDrawable(c.getInt(column)));
                        break;
                    case Cursor.FIELD_TYPE_NULL:
                    case Cursor.FIELD_TYPE_FLOAT:
                        break;
                }
            }
        }else Log.e("CursorAdapter", "cursor is null or empty");

        return (v);
    }
}