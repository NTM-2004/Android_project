package com.example.android_project;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.graphics.Rect;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfExport extends AppCompatActivity {

    private ImageView imagePreview;
    private EditText fileName;
    String fname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_format);

        imagePreview = findViewById(R.id.preview_image);
        Button export = findViewById(R.id.export_button);

        String imageUriString = getIntent().getStringExtra("imageUri");
        String pdfUriString = getIntent().getStringExtra("pdfUri");

        if (imageUriString != null && pdfUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            Uri pdfUri = Uri.parse(pdfUriString);

            imagePreview.setImageURI(imageUri);

            export.setOnClickListener(v -> {
                fileName = findViewById(R.id.file_name);
                fname = fileName.getText().toString();
                if (fname.isEmpty()) fname = "OCR";

                try (InputStream in = getContentResolver().openInputStream(pdfUri)) {
                    File dir = getExternalFilesDir(null);
                    File outFile = new File(dir, fname + ".pdf");
                    try (OutputStream out = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
                    }
                    Toast.makeText(this, "Xuất file thành công: " + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Lưu không thành công: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Không nhận dữ liệu PDF hoặc ảnh", Toast.LENGTH_LONG).show();
            finish();
        }

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navbar);
        bottomNavigation.setOnItemSelectedListener(menuItem -> {
            int item= menuItem.getItemId();
            if(item == R.id.homePage){
                startActivity(new Intent(this, MainActivity.class));
                return true;
            }else if(item == R.id.filePage){
                Intent intent = new Intent(this, FileManageActivity.class);
                startActivity(intent);
            }
            return false;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }
}