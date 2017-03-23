package com.fpil.android.remotesensor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteBindOrColumnIndexOutOfRangeException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;

/**
 * Created by bob on 24/11/16.
 */

public class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.FarmViewHolder> {
    final String LogView = "CardviewLog";

    public static class FarmViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView name, treatment, notes;
        double map_latitude, map_longitude;
        Date datetime;
        ImageView img;
        Button map;
        LeafColorView lcview;

        FarmViewHolder(View itemview){
            super(itemview);
            cv = (CardView) itemview.findViewById(R.id.cview);
            img = (ImageView) itemview.findViewById(R.id.farm_image);
            name = (TextView) itemview.findViewById(R.id.farm_name);
            notes = (TextView) itemview.findViewById(R.id.farm_note);
            treatment = (TextView) itemview.findViewById(R.id.farm_treat);
            map = (Button) itemview.findViewById(R.id.button_map);
            datetime = new Date();
            lcview = (LeafColorView) itemview.findViewById(R.id.leaf_color);
        }
    }

    private Cursor cursor;
    private SQLiteDatabase db, db2;

    ViewAdapter(Context context) {

        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(context);

        db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                FeedReaderDbHelper.Col_DB_View_Latitude,
                FeedReaderDbHelper.Col_DB_View_Longitude,
                FeedReaderDbHelper.Col_DB_View_Image_Farm,
                FeedReaderDbHelper.Col_DB_View_Name,
                FeedReaderDbHelper.Col_DB_View_Treatment,
                FeedReaderDbHelper.Col_DB_View_Key,
                FeedReaderDbHelper.Col_DB_View_Notes
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = FeedReaderDbHelper.Col_DB_View_Key + " DESC";

        cursor = db.query(
                FeedReaderDbHelper.Table_DB_View,           // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder,                                // The sort order
                null                                      // no limit
        );

    }

    @Override
    public FarmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.leafview,parent,false);
        FarmViewHolder vh = new FarmViewHolder(v);
        return vh;
    }

    private final String[] project_RG = {
            FeedReaderDbHelper.Col_DB_Raw_Data_Red,
            FeedReaderDbHelper.Col_DB_Raw_Data_Green
    };

    private Cursor cursor_RG;
    private String[] key_search = {""};
    final String key_query = FeedReaderDbHelper.Col_DB_Raw_Data_Key + "=?";

    @Override
    public void onBindViewHolder(FarmViewHolder holder, int position) {
        cursor.moveToPosition(position);
        holder.name.setText(cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderDbHelper.Col_DB_View_Name)));
        holder.notes.setText(cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderDbHelper.Col_DB_View_Notes)));
        holder.treatment.setText(cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderDbHelper.Col_DB_View_Treatment)));
        holder.map_latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(FeedReaderDbHelper.Col_DB_View_Latitude));
        holder.map_longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(FeedReaderDbHelper.Col_DB_View_Longitude));

        long key;
        try {
            key = cursor.getLong(cursor.getColumnIndexOrThrow(FeedReaderDbHelper.Col_DB_View_Key));
            holder.datetime.setTime(key);
            key_search[0] = String.format("%d", key);
            cursor_RG = db.query(
                    FeedReaderDbHelper.Table_DB_Raw_Data,
                    project_RG, key_query, key_search,null,null,null,null);

            // only take first data point
            cursor_RG.moveToFirst();
            int red = cursor_RG.getInt(0);
            int green = cursor_RG.getInt(1);
            float v = Math.max(5 * (1.0F - ((float) red)/green), 0.F);
            Log.d(LogView, String.format("Ratio : %f", v));
            holder.lcview.setFractionalValue(v);

        } catch (android.database.CursorIndexOutOfBoundsException e) {
            holder.lcview.setVisibility(View.INVISIBLE);
        }

        String imstring = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderDbHelper.Col_DB_View_Image_Farm));
        holder.img.setImageResource(R.mipmap.ic_launcher);
        BitmapFactory.Options bopt = new BitmapFactory.Options();
        byte[] decodedString = Base64.decode(imstring, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//        Bitmap bmp = BitmapFactory.decodeByteArray(imstring.getBytes(), 0, imstring.getBytes().length);
//        holder.img.setImageBitmap(decodedByte);

        holder.img.invalidate();
        Log.d(LogView, String.format("BVH %d", position));
        Log.d(LogView, String.format("IM: %s", decodedString));
    }

    @Override
    public int getItemCount() {
//        Log.d(LogView,String.format("ItemCount: %d", cursor.getCount()));
        return cursor.getCount();
    }

    @Override
    public long getItemId(int position){
        cursor.moveToPosition(position);
        return cursor.getLong(cursor.getColumnIndexOrThrow(FeedReaderDbHelper.Col_DB_View_Key));
    }
}
