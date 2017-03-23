package com.fpil.android.remotesensor;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.widget.Toast;

import com.fpil.android.common.logger.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Created by bob on 23/11/16.
 */

public class FeedReaderDbHelper extends SQLiteOpenHelper {
    static final String LogData = "FReader";
    public static final String Table_DB_Raw_Data = "DB_RAWDATA";
    public static final String Col_DB_Raw_Data_DateTime = "DateTime";
    public static final String Col_DB_Raw_Data_Red = "Red";
    public static final String Col_DB_Raw_Data_Green = "Green";
    public static final String Col_DB_Raw_Data_Blue = "Blue";
    public static final String Col_DB_Raw_Data_C = "C";
    public static final String Col_DB_Raw_Data_Key = "Key";
    public static final String Col_DB_Raw_Data_LeafIndex = "Leaf";

    public static final String Table_DB_View = "DB_View";
    public static final String Col_DB_View_Latitude = "Latitude";
    public static final String Col_DB_View_Longitude = "Longitude";
    public static final String Col_DB_View_Image_Farm = "Image_Farm";
    public static final String Col_DB_View_Name = "Name";
    public static final String Col_DB_View_Notes = "Notes";
    public static final String Col_DB_View_Treatment = "Treatment";
    public static final String Col_DB_View_Key = "Key";

    public static final String TEXT_TYPE = " TEXT";
    public static final String DATETIME_TYPE = " INTEGER";
    public static final String INT_TYPE = " INTEGER";
    public static final String FLOAT_TYPE = " REAL";
    public static final String IMAGE_TYPE = " BLOB";

    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_ENTRIES_DB_Raw_Data =
            "CREATE TABLE " + Table_DB_Raw_Data + " (" +
                    Col_DB_Raw_Data_DateTime + DATETIME_TYPE + COMMA_SEP +
                    Col_DB_Raw_Data_Red + INT_TYPE + COMMA_SEP +
                    Col_DB_Raw_Data_Green + INT_TYPE + COMMA_SEP +
                    Col_DB_Raw_Data_Blue + INT_TYPE + COMMA_SEP +
                    Col_DB_Raw_Data_C + INT_TYPE + COMMA_SEP +
                    Col_DB_Raw_Data_Key + DATETIME_TYPE + COMMA_SEP +
                    Col_DB_Raw_Data_LeafIndex + INT_TYPE + " )";

    private static final String SQL_CREATE_ENTRIES_DB_View =
            "CREATE TABLE " + Table_DB_View + " (" +
                    Col_DB_View_Latitude + FLOAT_TYPE + COMMA_SEP +
                    Col_DB_View_Longitude + FLOAT_TYPE + COMMA_SEP +
                    Col_DB_View_Image_Farm + IMAGE_TYPE + COMMA_SEP +
                    Col_DB_View_Name + TEXT_TYPE + COMMA_SEP +
                    Col_DB_View_Notes + TEXT_TYPE + COMMA_SEP +
                    Col_DB_View_Treatment + TEXT_TYPE + COMMA_SEP +
                    Col_DB_View_Key + DATETIME_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES_Raw_Data =
            "DROP TABLE IF EXISTS " + Table_DB_Raw_Data;

    private static final String SQL_DELETE_ENTRIES_View =
            "DROP TABLE IF EXISTS " + Table_DB_View;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = String.format("FarmDB_v%d.db", DATABASE_VERSION);

    Context con;

    public FeedReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        con = context;
//        Log.d(LogData, new File(Environment.getExternalStorageDirectory().getAbsolutePath(),DATABASE_NAME).getAbsolutePath());
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES_DB_Raw_Data);
        db.execSQL(SQL_CREATE_ENTRIES_DB_View);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES_Raw_Data);
        db.execSQL(SQL_DELETE_ENTRIES_View);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    void saveExternally() {
        this.close();

        FileChannel inc = null, outc = null;
        try {
            String in_name = this.getReadableDatabase().getPath();
            Log.d(LogData, in_name);
            File fdata = new File(in_name);
            inc = new FileInputStream(fdata).getChannel();
        } catch (FileNotFoundException e) {
            Toast.makeText(con, "File IN Not Found!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            f.mkdir();
            String out_name = f.getAbsolutePath() + File.separator + DATABASE_NAME;
            Log.d(LogData, out_name);
            outc = new FileOutputStream(out_name,false).getChannel();
        } catch (FileNotFoundException e) {
            Toast.makeText(con, "File OUT Not Found!", Toast.LENGTH_SHORT).show();
            return;
        } catch (SecurityException e) {
            Toast.makeText(con, "No write permit!", Toast.LENGTH_SHORT).show();
            return;
        }
        long t = 0;
        try {
            t = inc.transferTo(0, inc.size(), outc);
        } catch (IOException e) {
            Toast.makeText(con, "IO Exception!", Toast.LENGTH_SHORT).show();
        } catch (NullPointerException e) {
            Toast.makeText(con, "Null pointer Exception!", Toast.LENGTH_SHORT).show();
        } finally {
            try {
                inc.close();
                outc.close();
            } catch (IOException e) {
                Toast.makeText(con, "Finally IO Exception!", Toast.LENGTH_SHORT).show();
            }
        }
        Toast.makeText(con, String.format("%d bytes written", t), Toast.LENGTH_SHORT).show();
    }

    void loadFromExternal() {
        this.close();

        FileChannel inc = null, outc = null;
        try {
            String in_name = this.getReadableDatabase().getPath();
            Log.d(LogData, in_name);
            File fdata = new File(in_name);
            outc = new FileOutputStream(fdata, false).getChannel();
        } catch (FileNotFoundException e) {
            Toast.makeText(con, "File OUT Not Found!", Toast.LENGTH_SHORT).show();
            return;
        } catch (SecurityException e) {
            Toast.makeText(con, "No write permit!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            f.mkdir();
            String out_name = f.getAbsolutePath() + File.separator + DATABASE_NAME;
            Log.d(LogData, out_name);
            inc = new FileInputStream(out_name).getChannel();
        } catch (FileNotFoundException e) {
            Toast.makeText(con, "File IN Not Found!", Toast.LENGTH_SHORT).show();
            return;
        }

        long t = 0;
        try {
            t = inc.transferTo(0, inc.size(), outc);
        } catch (IOException e) {
            Toast.makeText(con, "IO Exception!", Toast.LENGTH_SHORT).show();
        } catch (NullPointerException e) {
            Toast.makeText(con, "Null pointer Exception!", Toast.LENGTH_SHORT).show();
        } finally {
            try {
                inc.close();
                outc.close();
            } catch (IOException e) {
                Toast.makeText(con, "Finally IO Exception!", Toast.LENGTH_SHORT).show();
            }
        }
        Toast.makeText(con, String.format("%d bytes written", t), Toast.LENGTH_SHORT).show();
    }
}