package com.example.android_project;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileManageActivity extends AppCompatActivity {
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manage);

        container = findViewById(R.id.container_file);

        // Xin quyền cấp phép 
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            loadWordFiles();
        }

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navbar);
        bottomNavigation.setOnItemSelectedListener(menuItem -> {
            int item= menuItem.getItemId();
            if(item == R.id.homePage){
                Intent intent = new Intent(FileManageActivity.this, MainActivity.class);
                startActivity(intent);
            }else if(item == R.id.filePage){
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Auto reload danh sách file mỗi khi quay về FileManageActivity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            loadWordFiles();
        }
    }

    //Nếu có quyền truy cập
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadWordFiles();
        } else {
            Toast.makeText(this, "Không có quyền đọc file!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadWordFiles() {
        //xóa hết hiển thị đã có
        container.removeAllViews();

        //Lấy đường dẫn ngoài
        File dir = getExternalFilesDir(null);
        if (dir == null || !dir.exists()) {
            TextView tv = new TextView(this);
            tv.setText("Thư mục lưu file không tồn tại");
            tv.setTextColor(Color.WHITE);
            tv.setPadding(20, 20, 20, 20);
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
            container.addView(tv);
            return;
        }

        File[] allFiles = dir.listFiles();
        if (allFiles == null || allFiles.length == 0) {
            TextView tv = new TextView(this);
            tv.setText("Không có file nào trong thư mục app");
            tv.setPadding(20, 20, 20, 20);
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
            container.addView(tv);
            return;
        }

        // Lọc chỉ lấy file .docx + .pdf và sắp xếp theo thời gian tạo (mới nhất trước)
        List<File> docxFiles = new ArrayList<>();
        for (File file : allFiles) {
            if (file.isFile() && (file.getName().toLowerCase().endsWith(".docx") || file.getName().toLowerCase().endsWith(".pdf"))) {
                docxFiles.add(file);
            }
        }

        if (docxFiles.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Không có file Word (.docx) nào trong thư mục");
            tv.setPadding(20, 20, 20, 20);
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
            container.addView(tv);
            return;
        }

        // Sắp xếp theo thời gian sửa đổi cuối cùng (file mới nhất lên đầu)
        Collections.sort(docxFiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });

        for (File file : docxFiles) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(20, 20, 20, 20);
            itemLayout.setGravity(Gravity.CENTER_VERTICAL);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ContextCompat.getColor(this, R.color.primary_color));
            itemLayout.setBackground(bg);

            ImageView icon = new ImageView(this);

            if(file.getName().toLowerCase().endsWith(".docx")){
                icon.setImageResource(R.drawable.word_icon);
            }else{
                icon.setImageResource(R.drawable.pdf_icon);
            }

            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(80, 80);
            iconParams.setMargins(0, 0, 30, 0);
            icon.setLayoutParams(iconParams);
            itemLayout.addView(icon);

            TextView tv = new TextView(this);
            tv.setText(file.getName());
            tv.setTextSize(20);
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
            LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            tvParams.gravity = Gravity.CENTER_VERTICAL;
            tv.setLayoutParams(tvParams);
            itemLayout.addView(tv);

            ImageButton menu = new ImageButton(this);
            menu.setImageResource(R.drawable.file_menu);
            menu.setBackgroundColor(Color.TRANSPARENT);
            LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            menuParams.gravity = Gravity.CENTER_VERTICAL;
            menu.setLayoutParams(menuParams);
            itemLayout.addView(menu);

            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            itemParams.setMargins(0, 0, 0, 20);
            container.addView(itemLayout, itemParams);

            itemLayout.setOnClickListener(v -> openWordFile(file));
            menu.setOnClickListener(v ->{
                PopupMenu popup = new PopupMenu(this, v);
                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.delete_file) {
                        new AlertDialog.Builder(this)
                                .setTitle("Xóa file")
                                .setMessage("Bạn có chắc muốn xóa không ?")
                                .setPositiveButton("Có", (dialog, which) ->{
                                    boolean deleted = file.delete();
                                    if (deleted) {
                                        Toast.makeText(this, "Đã xóa thành công: " + file.getName(), Toast.LENGTH_SHORT).show();
                                        loadWordFiles();
                                    } else {
                                        Toast.makeText(this, "Lỗi: Không thể xóa file " + file.getName(), Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("Không", null)
                                .show();
                        return true;
                    }

                    // xử lý nút chia sẻ file
                    if (itemId == R.id.share_file)
                    {
                        Intent intent = new Intent(FileManageActivity.this, ShareFileActivity.class);
                        intent.putExtra("file_path", file.getAbsolutePath());
                        intent.putExtra("file_name", file.getName());
                        startActivity(intent);
                        return true;
                    }
                    return false;
                });
                popup.getMenuInflater().inflate(R.menu.file_menu, popup.getMenu());
                popup.show();
            });
        }
    }

    private void openWordFile(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Không có ứng dụng mở file Word", Toast.LENGTH_SHORT).show();
        }
    }
}