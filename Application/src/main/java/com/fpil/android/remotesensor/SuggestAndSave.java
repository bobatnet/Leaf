package com.fpil.android.remotesensor;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Base64;

import com.fpil.android.common.logger.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Created by bob on 24/11/16.
 */

public class SuggestAndSave {
    final String SuggestTag = "Suggest";
    static public final String red = "Red";
    static public final String green = "Green";
    static public final String blue = "Blue";
    static public final String C = "C";
    static public final String IMEI = "IMEI";
    static public final String latitude = FeedReaderDbHelper.Col_DB_View_Latitude;
    static public final String longitude = FeedReaderDbHelper.Col_DB_View_Longitude;
    static public final String accuracy = "Accuracy";
    static public final String Location = "Location";

    Context context;

    SuggestAndSave(Context c) {
        context = c;
    }

    String suggest(Intent data) {
        ArrayList<Integer> reds, greens;
        Bundle ext = data.getExtras();
        reds = ext.getIntegerArrayList(red);
        greens = ext.getIntegerArrayList(green);

        Double avred, avgreen;
        Long sum = 0L;
        try {
            if (!reds.isEmpty()) {
                for (Integer r : reds) sum += r;
                avred = sum.doubleValue() / reds.size();

                sum = 0L;
                for (Integer g : greens) sum += g;
                avgreen = sum.doubleValue() / greens.size();

                double decGreen = 0.7 * avred + 1900.0;

                if (avgreen < 3000)
                    return "Apply urea.";
                else
                    return "Urea is sufficient.";

                //if (avgreen < decGreen)
                //    return "Apply urea.";
                //else
                //    return "Urea is sufficient.";
            }
        } catch (NullPointerException e) {}
        return "Retry.";
    }

    void storeRemote (Intent data){
        // TODO: store data remotely
    }

    void updateLocation(long key, double latitude, double longitude) {
        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(context);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues row = new ContentValues();

        row.put(FeedReaderDbHelper.Col_DB_View_Latitude, latitude);
        row.put(FeedReaderDbHelper.Col_DB_View_Longitude, longitude);

        db.update(FeedReaderDbHelper.Table_DB_View, row, FeedReaderDbHelper.Col_DB_View_Key + "=" + String.format("%l",key),null);
    }

    long saveInfo(Intent data) {
        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(context);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues row = new ContentValues();

        row.put(FeedReaderDbHelper.Col_DB_View_Latitude, data.getDoubleExtra(FeedReaderDbHelper.Col_DB_View_Latitude, 0));
        row.put(FeedReaderDbHelper.Col_DB_View_Longitude, data.getDoubleExtra(FeedReaderDbHelper.Col_DB_View_Longitude, 0));

//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        BitmapFactory.decodeResource(context.getResources(), R.drawable.blank_farm_web).compress(Bitmap.CompressFormat.JPEG, 90, stream);
//        String base64photo = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);

        row.put(FeedReaderDbHelper.Col_DB_View_Image_Farm, data.getStringExtra(FarmEntryActivity.farmPhoto));
        row.put(FeedReaderDbHelper.Col_DB_View_Name, data.getStringExtra(FarmEntryActivity.farmName));
        row.put(FeedReaderDbHelper.Col_DB_View_Notes, data.getStringExtra(FarmEntryActivity.farmNotes));
        row.put(FeedReaderDbHelper.Col_DB_View_Treatment, suggest(data));
        row.put(FeedReaderDbHelper.Col_DB_View_Key, data.getLongExtra(FarmEntryActivity.observeKey, 0));

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(FeedReaderDbHelper.Table_DB_View, null, row);

        Log.d(SuggestTag, String.format("Saved view data %d.", newRowId));
        return data.getLongExtra(FarmEntryActivity.observeKey, 0);
    }

    long saveInfo_Demo() {
        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(context);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues row = new ContentValues();

        row.put(FeedReaderDbHelper.Col_DB_View_Latitude, 0);
        row.put(FeedReaderDbHelper.Col_DB_View_Longitude, 0);

//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        BitmapFactory.decodeResource(context.getResources(), R.drawable.blank_farm_web).compress(Bitmap.CompressFormat.JPEG, 90, stream);
//        String base64photo = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);

        row.put(FeedReaderDbHelper.Col_DB_View_Image_Farm, "");
        row.put(FeedReaderDbHelper.Col_DB_View_Name, "Wheat Farm");
        row.put(FeedReaderDbHelper.Col_DB_View_Notes, "Notes");
        row.put(FeedReaderDbHelper.Col_DB_View_Treatment, "Apply urea");
        row.put(FeedReaderDbHelper.Col_DB_View_Key, 0);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(FeedReaderDbHelper.Table_DB_View, null, row);

        Log.d(SuggestTag, String.format("Saved view data %d.", newRowId));
        return newRowId;
    }
}
