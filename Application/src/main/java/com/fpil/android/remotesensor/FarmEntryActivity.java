package com.fpil.android.remotesensor;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.Date;

/**
 * Created by bob on 23/11/16.
 */

public class FarmEntryActivity extends Activity {
    ImageView mImageView;
    String base64photo = "";

    static final String farmPhoto = "farm photo";
    static final String farmName = "farm name";
    static final String farmNotes = "farm notes";
    static final String observeKey = "observe key";

    long key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.newentry);

        TextView dateTime = (TextView) findViewById(R.id.textDateTime);

        final Date date = new Date();
        dateTime.setText(date.toString());

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setImageResource(R.drawable.blank_farm_web);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });
        ImageButton newImage;
        newImage = (ImageButton) findViewById(R.id.captureImageButton);
        newImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });

        Button save = (Button) findViewById(R.id.savebutton);
        final EditText fname = (EditText) findViewById(R.id.editName);
        final EditText fnotes = (EditText) findViewById(R.id.editNotes);
        key = getIntent().getLongExtra(observeKey,0);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent result = new Intent();
                result.putExtra(farmPhoto, base64photo);
                result.putExtra(farmName, fname.getText().toString());
                result.putExtra(farmNotes, fnotes.getText().toString());
                result.putExtra(observeKey, key);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });
    }

    static final int REQUEST_IMAGE_CAPTURE = 101;
    static final int REQUEST_NEW_ENTRY = 102;

    private void takePhoto() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(getApplicationContext(), "Device does not have camera", Toast.LENGTH_LONG).show();
        } else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            base64photo = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);
        }
    }

}
