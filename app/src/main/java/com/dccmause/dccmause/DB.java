package com.dccmause.dccmause;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DB {

    private static final String DB_NAME = "mydb";
    private static final int DB_VERSION = 22;
    static final String ACC_TABLE = "accessory";
    static final String LOCO_TABLE = "loco";
    static final String CV_TABLE = "cv";
    static final String DECODERS_TABLE = "decoders";

    static final String ROW_ID = "_id";
    static final String ACC_IMAGE = "image";
    static final String ACC_TEXT = "text";
    static final String ACC_ADDR = "address";
    static final String ACC_STATE = "state";
    static final String ACC_S_IMAGE = "s_image";
    static final String ACC_TYPE = "type";
    static final String ACC_INVERTED = "inverted";

    static final String LOCO_IMAGE = "image";
    static final String LOCO_TEXT = "text";
    static final String LOCO_ADDR = "address";
    private static final String LOCO_TYPE = "type"; //TODO
    private static final String LOCO_INVERTED = "inverted"; //TODO
    static final String LOCO_SPEED_POS = "speed_pos";
    static final String LOCO_FUNC_EN = "func_en";
    static final String LOCO_FUNC_STS = "func_sts";
    static final String LOCO_FUNC_TYPE_ARR = "func_type_arr";

    static final String CV_DECODER_ID = "decoder_id";
    static final String CV_ADDRESS = "address";
    static final String CV_NAME = "name";
    static final String CV_DESCRIPTION = "description";
    static final String CV_READ_ONLY = "read_only";
    static final String CV_MIN_VALUE = "min_val";
    static final String CV_MIN_DESCRIPTION = "min_val_desc";
    static final String CV_MAX_VALUE = "max_val";
    static final String CV_MAX_DESCRIPTION = "max_val_desc";
    static final String CV_DFL_VALUE = "dfl_val";
    static final String CV_CUR_VALUE = "cur_val";
    static final String CV_BITS_EN = "bits_en";
    static final String CV_BIT0_DESCRIPTION   = "bit0_desc";
    static final String CV_BIT0_0_DESCRIPTION = "bit0_0_desc";
    static final String CV_BIT0_1_DESCRIPTION = "bit0_1_desc";
    static final String CV_BIT1_DESCRIPTION   = "bit1_desc";
    static final String CV_BIT1_0_DESCRIPTION = "bit1_0_desc";
    static final String CV_BIT1_1_DESCRIPTION = "bit1_1_desc";
    static final String CV_BIT2_DESCRIPTION   = "bit2_desc";
    static final String CV_BIT2_0_DESCRIPTION = "bit2_0_desc";
    static final String CV_BIT2_1_DESCRIPTION = "bit2_1_desc";
    static final String CV_BIT3_DESCRIPTION   = "bit3_desc";
    static final String CV_BIT3_0_DESCRIPTION = "bit3_0_desc";
    static final String CV_BIT3_1_DESCRIPTION = "bit3_1_desc";
    static final String CV_BIT4_DESCRIPTION   = "bit4_desc";
    static final String CV_BIT4_0_DESCRIPTION = "bit4_0_desc";
    static final String CV_BIT4_1_DESCRIPTION = "bit4_1_desc";
    static final String CV_BIT5_DESCRIPTION   = "bit5_desc";
    static final String CV_BIT5_0_DESCRIPTION = "bit5_0_desc";
    static final String CV_BIT5_1_DESCRIPTION = "bit5_1_desc";
    static final String CV_BIT6_DESCRIPTION   = "bit6_desc";
    static final String CV_BIT6_0_DESCRIPTION = "bit6_0_desc";
    static final String CV_BIT6_1_DESCRIPTION = "bit6_1_desc";
    static final String CV_BIT7_DESCRIPTION   = "bit7_desc";
    static final String CV_BIT7_0_DESCRIPTION = "bit7_0_desc";
    static final String CV_BIT7_1_DESCRIPTION = "bit7_1_desc";

    static final String DECODER_TYPE = "type";
    static final String DECODER_NAME = "name";
    static final String DECODER_MANUFACTURER = "manufacturer";
    static final String DECODER_DESC = "description";
    private static final String DECODER_IMAGE = "image"; //TODO
    private static final String DECODER_ID_CV_NUM = "id_cv_num"; //TODO
    private static final String DECODER_ID_CV_VAL = "id_cv_val"; //TODO

    //static final int ACC_TYPE_TURNOUT  = 0;
    //static final int ACC_TYPE_LAMP     = 1;
    //static final int ACC_TYPE_SIGNAL   = 2;
    //static final int ACC_TYPE_SOUND    = 3;
    private static final int[] AccTypeImages = {R.drawable.acc_turnout, R.drawable.acc_lamp_off, R.drawable.acc_signal_off, R.drawable.acc_sound_off};
    private static final int[] AccOnImages = {R.drawable.acc_turned, R.drawable.acc_lamp_on, R.drawable.acc_signal_on, R.drawable.acc_sound_on};
    private static final int[] AccOffImages = {R.drawable.acc_straight, R.drawable.acc_lamp_off, R.drawable.acc_signal_off, R.drawable.acc_sound_off};

    //static final int FUNC_TYPE_UNKNOWN  = 0;
    //static final int FUNC_TYPE_LAMP     = 1;
    //static final int FUNC_TYPE_ENGINE   = 2;
    //static final int FUNC_TYPE_SOUND    = 3;
    //static final int FUNC_TYPE_HORN     = 4;
    //static final int FUNC_TYPE_HOOK     = 5;
    private static final int[] FuncOnImages = {R.drawable.func_unknown_on, R.drawable.func_lamp_on, R.drawable.func_engine_on, R.drawable.func_sound_on, R.drawable.func_horn_on, R.drawable.func_hook_on};
    private static final int[] FuncOffImages = {R.drawable.func_unknown_off, R.drawable.func_lamp_off, R.drawable.func_engine_off, R.drawable.func_sound_off, R.drawable.func_horn_off, R.drawable.func_hook_off};
    static final int FUNC_FUNC_COUNT    = 29;
    static final int FUNC_TYPE_ISIZE    = 16;
    static final int FUNC_TYPE_ARR_SIZE = FUNC_FUNC_COUNT*FUNC_TYPE_ISIZE;

    private static final String ACC_CREATE =
            "create table " + ACC_TABLE + "(" +
                    ROW_ID + " integer primary key autoincrement, " +
                    ACC_IMAGE + " integer, " +
                    ACC_TEXT + " text," +
                    ACC_ADDR + " text," +
                    ACC_STATE + " integer," +
                    ACC_S_IMAGE + " integer," +
                    ACC_TYPE + " integer," +
                    ACC_INVERTED + " integer" +
                    ");";
    private static final String LOCO_CREATE =
            "create table " + LOCO_TABLE + "(" +
                    ROW_ID + " integer primary key autoincrement, " +
                    LOCO_IMAGE + " blob," +
                    LOCO_TEXT + " text," +
                    LOCO_ADDR + " text," +
                    LOCO_TYPE + " integer," +
                    LOCO_SPEED_POS + " integer," +
                    LOCO_FUNC_EN + " integer," +
                    LOCO_FUNC_STS + " integer," +
                    LOCO_FUNC_TYPE_ARR + " blob" +
                    LOCO_INVERTED + " integer" +
                    ");";
    private static final String CV_CREATE =
            "create table " + CV_TABLE + "(" +
                    ROW_ID + " integer primary key autoincrement, " +
                    CV_DECODER_ID + " integer," +
                    CV_ADDRESS + " integer," +
                    CV_NAME + " text," +
                    CV_DESCRIPTION + " text," +
                    CV_READ_ONLY + " integer," +
                    CV_MIN_VALUE + " integer," +
                    CV_MIN_DESCRIPTION + " text," +
                    CV_MAX_VALUE + " integer," +
                    CV_MAX_DESCRIPTION + " text," +
                    CV_DFL_VALUE + " integer," +
                    CV_CUR_VALUE + " integer," +
                    CV_BITS_EN + " integer," +
                    CV_BIT0_DESCRIPTION + " text," +
                    CV_BIT0_0_DESCRIPTION + " text," +
                    CV_BIT0_1_DESCRIPTION + " text," +
                    CV_BIT1_DESCRIPTION + " text," +
                    CV_BIT1_0_DESCRIPTION + " text," +
                    CV_BIT1_1_DESCRIPTION + " text," +
                    CV_BIT2_DESCRIPTION + " text," +
                    CV_BIT2_0_DESCRIPTION + " text," +
                    CV_BIT2_1_DESCRIPTION + " text," +
                    CV_BIT3_DESCRIPTION + " text," +
                    CV_BIT3_0_DESCRIPTION + " text," +
                    CV_BIT3_1_DESCRIPTION + " text," +
                    CV_BIT4_DESCRIPTION + " text," +
                    CV_BIT4_0_DESCRIPTION + " text," +
                    CV_BIT4_1_DESCRIPTION + " text," +
                    CV_BIT5_DESCRIPTION + " text," +
                    CV_BIT5_0_DESCRIPTION + " text," +
                    CV_BIT5_1_DESCRIPTION + " text," +
                    CV_BIT6_DESCRIPTION + " text," +
                    CV_BIT6_0_DESCRIPTION + " text," +
                    CV_BIT6_1_DESCRIPTION + " text," +
                    CV_BIT7_DESCRIPTION + " text," +
                    CV_BIT7_0_DESCRIPTION + " text," +
                    CV_BIT7_1_DESCRIPTION + " text" +
                    ");";
    private static final String DECODER_CREATE =
            "create table " + DECODERS_TABLE + "(" +
                    ROW_ID + " integer primary key autoincrement, " +
                    DECODER_TYPE + " integer," +
                    DECODER_NAME + " text," +
                    DECODER_MANUFACTURER + " text," +
                    DECODER_DESC + " text," +
                    DECODER_ID_CV_NUM + " integer," +
                    DECODER_ID_CV_VAL + " integer," +
                    DECODER_IMAGE + " integer" +
                    ");";

    private final Context mCtx;


    private DBHelper mDBHelper;
    private SQLiteDatabase mDatabase;

    DB(Context ctx) {
        mCtx = ctx;
    }

    void open() {
        mDBHelper = new DBHelper(mCtx, DB_NAME, null, DB_VERSION);
        mDatabase = mDBHelper.getWritableDatabase();
    }

    void close() {
        if (mDBHelper != null) mDBHelper.close();
    }

    Cursor getAllData(String table) {
        return mDatabase.query(table, null, null, null, null, null, null);
    }

    Cursor getAllDataWithQuery(String table, String query) {
        return mDatabase.query(table, null, query, null, null, null, null);
    }

    long addRec(String table, ContentValues cv) {
       return mDatabase.insert(table, null, cv);
    }

    void updateRec(String table, long id, ContentValues cv) {
        mDatabase.update(table, cv, ROW_ID + " = " + id, null);
    }

    ContentValues getRec(String table, long id){
        Cursor cursor = mDatabase.query(table, null, ROW_ID + " = " + id, null, null, null, null);
        if (cursor.moveToFirst())
            return cursorRowToContentValues(cursor);
        else Log.w("DB","cursor.moveToFirst() is null in getRec");
        return null;
    }

    /*ContentValues getRecWithQuery(String table, String query){
        Cursor cursor = mDatabase.query(table, null, query, null, null, null, null);
        if (cursor.moveToFirst())
            return cursorRowToContentValues(cursor);
        return null;
    }*/

    // удалить запись из ACC_TABLE
    void delRec(String table, long id) {
        mDatabase.delete(table, ROW_ID + " = " + id, null);
    }

    static ContentValues cursorRowToContentValues(Cursor cursor) {
        ContentValues values = new ContentValues();
        String[] columns = cursor.getColumnNames();
        int length = columns.length;
        for (int i = 0; i < length; i++) {
            switch (cursor.getType(i)) {
                case Cursor.FIELD_TYPE_NULL:
                    values.putNull(columns[i]);
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    values.put(columns[i], cursor.getInt(i));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    values.put(columns[i], cursor.getDouble(i));
                    break;
                case Cursor.FIELD_TYPE_STRING:
                    values.put(columns[i], cursor.getString(i));
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    values.put(columns[i], cursor.getBlob(i));
                    break;
            }
        }
        return values;
    }

    private class DBHelper extends SQLiteOpenHelper {

        final Context mContext;

        DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                        int version) {
            super(context, name, factory, version);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL(ACC_CREATE);
            db.execSQL(LOCO_CREATE);
            db.execSQL(CV_CREATE);
            db.execSQL(DECODER_CREATE);

            //load data from csv
            CSVTransfer.LoadAll(db, mContext);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 21)
            {
                db.execSQL("DROP TABLE " + CV_TABLE);
                db.execSQL("DROP TABLE " + DECODERS_TABLE);
                db.execSQL(CV_CREATE);
                db.execSQL(DECODER_CREATE);
            }

            if (oldVersion != newVersion){
                //update the DB from csv
                CSVTransfer.LoadAll(db, mContext);
            }

        }
    }

    static int FuncGetStateImg(int type, boolean state){
        if (type<FuncOffImages.length) {
            if (state) return FuncOnImages[type];
            else return FuncOffImages[type];
        }
        Log.e("DB", "FuncGetStateImg() incorrect type");
        return 0;
    }

    static int AccGetStateImg(int type, boolean state){
        if (type<AccTypeImages.length) {
            if (state) return AccOnImages[type];
            else return AccOffImages[type];
        }
        Log.e("DB", "AccGetStateImg() incorrect type");
        return 0;
    }

    static int AccGetTypeImg(int type){
        if (type<AccTypeImages.length)
            return AccTypeImages[type];
        Log.e("DB", "AccGetTypeImg() incorrect type");
        return 0;
    }
}
