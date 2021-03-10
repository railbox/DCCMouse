package com.dccmause.dccmause;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


class CSVTransfer {

    private static final String CV_columns[][] = {
            {DB.CV_ADDRESS, "1"},
            {DB.CV_NAME, ""},
            {DB.CV_DESCRIPTION, ""},
            {DB.CV_READ_ONLY, "1"},
            {DB.CV_MIN_VALUE, "1"},
            {DB.CV_MIN_DESCRIPTION, ""},
            {DB.CV_MAX_VALUE, "1"},
            {DB.CV_MAX_DESCRIPTION, ""},
            {DB.CV_DFL_VALUE, "1"},
            {DB.CV_BITS_EN, "1"},
            {DB.CV_BIT0_DESCRIPTION, ""},
            {DB.CV_BIT0_0_DESCRIPTION, ""},
            {DB.CV_BIT0_1_DESCRIPTION, ""},
            {DB.CV_BIT1_DESCRIPTION, ""},
            {DB.CV_BIT1_0_DESCRIPTION, ""},
            {DB.CV_BIT1_1_DESCRIPTION, ""},
            {DB.CV_BIT2_DESCRIPTION, ""},
            {DB.CV_BIT2_0_DESCRIPTION, ""},
            {DB.CV_BIT2_1_DESCRIPTION, ""},
            {DB.CV_BIT3_DESCRIPTION, ""},
            {DB.CV_BIT3_0_DESCRIPTION, ""},
            {DB.CV_BIT3_1_DESCRIPTION, ""},
            {DB.CV_BIT4_DESCRIPTION, ""},
            {DB.CV_BIT4_0_DESCRIPTION, ""},
            {DB.CV_BIT4_1_DESCRIPTION, ""},
            {DB.CV_BIT5_DESCRIPTION, ""},
            {DB.CV_BIT5_0_DESCRIPTION, ""},
            {DB.CV_BIT5_1_DESCRIPTION, ""},
            {DB.CV_BIT6_DESCRIPTION, ""},
            {DB.CV_BIT6_0_DESCRIPTION, ""},
            {DB.CV_BIT6_1_DESCRIPTION, ""},
            {DB.CV_BIT7_DESCRIPTION, ""},
            {DB.CV_BIT7_0_DESCRIPTION, ""},
            {DB.CV_BIT7_1_DESCRIPTION, ""},
    };
    private static final String Decoder_columns[] = {
            DB.DECODER_TYPE,
            DB.DECODER_NAME,
            DB.DECODER_DESC,
            DB.DECODER_MANUFACTURER
    };

    @SuppressWarnings("WeakerAccess")
    static boolean LoadCVs(String filePath, SQLiteDatabase db, Context context){
        AssetManager manager = context.getAssets();
        InputStream inStream = null;
        try {
            inStream = manager.open(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (inStream != null) {
            try {
                BufferedReader buffer = new BufferedReader(new InputStreamReader(inStream, "Windows-1251"));
                String line;
                long decoder_id = -1;
                for (int lineNum=0; (line = buffer.readLine()) != null; lineNum++){
                    String[] columns = line.split(";",-1);
                    if (lineNum == 1) {
                        if (columns.length < Decoder_columns.length) {
                            Log.d("CSVTransfer", "Line "+lineNum+": The Columns length read = " + columns.length + " instead of " + Decoder_columns.length);
                            continue;
                        }
                        String DecoderName = "";
                        ContentValues cv;
                        boolean decoderExists = false;
                        //Find the decoder name in the file
                        for (int i = 0; i < Decoder_columns.length; i++) {
                            if (Decoder_columns[i].equals(DB.DECODER_NAME)) {
                                DecoderName = columns[i];
                                break;
                            }
                        }
                        //Ask database for this name
                        Cursor cursor = db.query(DB.DECODERS_TABLE, null, DB.DECODER_NAME + " = '" + DecoderName + "'", null, null, null, null);
                        if (cursor.moveToFirst()) {
                            cv = DB.cursorRowToContentValues(cursor);
                            decoderExists = true;
                            decoder_id = (int) cv.get(DB.ROW_ID);
                        }
                        else cv = new ContentValues(Decoder_columns.length);
                        //Update values in row
                        for (int i = 0; i < Decoder_columns.length; i++) {
                                cv.put(Decoder_columns[i], columns[i]);
                        }
                        //Update the database
                        if (decoderExists) {
                            db.update(DB.DECODERS_TABLE, cv, DB.ROW_ID + " = " + decoder_id, null);
                            Log.d("CSVTransfer", "The decoder with id " + decoder_id + " updated");
                        } else {
                            decoder_id = db.insert(DB.DECODERS_TABLE, null, cv);
                            Log.d("CSVTransfer", "The decoder with id "+decoder_id+" inserted to table");
                        }
                    }
                    else if (lineNum>=3) {
                        if (columns.length < CV_columns.length) {
                            Log.d("CSVTransfer", "Line " + lineNum + ": The Columns length read = " + columns.length + " instead of " + CV_columns.length);
                            continue;
                        }
                        ContentValues cv;
                        boolean cvExists = false;
                        int CV_address = 0;
                        long line_id = -1;
                        //Find the cv address in the file
                        for (int i = 0; i < CV_columns.length; i++) {
                            if (CV_columns[i][0].equals(DB.CV_ADDRESS)) {
                                CV_address = Integer.valueOf(columns[i]);
                                break;
                            }
                        }
                        //Ask database for this cv
                        Cursor cursor = db.query(DB.CV_TABLE, null, DB.CV_DECODER_ID + " = " + decoder_id + " AND " + DB.CV_ADDRESS + " = " + CV_address, null, null, null, null);
                        if (cursor.moveToFirst()) {
                            cv = DB.cursorRowToContentValues(cursor);
                            cvExists = true;
                            line_id = (int) cv.get(DB.ROW_ID);
                        }
                        else cv = new ContentValues(CV_columns.length);
                        //Update values in row
                        for (int i = 0; i < CV_columns.length; i++) {
                            if (CV_columns[i][1].length() > 0) { //this is an integer value
                                if (columns[i].length() != 0)
                                    cv.put(CV_columns[i][0], Integer.valueOf(columns[i]));
                                else cv.put(CV_columns[i][0], 0);
                            }else cv.put(CV_columns[i][0], columns[i]);
                        }
                        //Update the database
                        if (cvExists) {
                            db.update(DB.CV_TABLE, cv, DB.ROW_ID + " = " + line_id, null);
                            Log.d("CSVTransfer", "The CV with decoder id "+decoder_id+" and address = "+CV_address+" updated");
                        } else {
                            //Find the default value and apply to current
                            int dfl_val = (int)cv.get(DB.CV_DFL_VALUE);
                            cv.put(DB.CV_CUR_VALUE, dfl_val);
                            //Setup the decoder_id
                            cv.put(DB.CV_DECODER_ID,decoder_id);
                            //Finally add data to the DB
                            db.insert(DB.CV_TABLE, null, cv);
                            Log.d("CSVTransfer", "The CV with decoder id "+decoder_id+" and address = "+CV_address+" inserted to table");
                        }
                    }
                }
                Log.d("CSVTransfer", "The "+filePath+" file was loaded successfully with decoderID="+decoder_id);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    static void LoadAll(SQLiteDatabase db, Context context){
        AssetManager manager = context.getAssets();
        try {
            String[] list = manager.list("");
            boolean EmptyCSVList = true;
            if (list.length > 0) {
                for (String file : list) {
                    if (file.contains(".csv")){
                        EmptyCSVList = false;
                        CSVTransfer.LoadCVs(file,db,context);
                    }
                }
            }
            if (EmptyCSVList) Log.w("CSV","Empty assets");
        }catch (Exception e){
            Log.e("CSV","LoadAll error:", e);
        }
    }
}
