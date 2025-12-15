package com.example.android_project;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileManageFragment extends Fragment {
    private LinearLayout container;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_manage, container, false);

        this.container = view.findViewById(R.id.container_file);

        // Xin quyền cấp phép
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            loadWordFiles();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Auto reload danh sách file mỗi khi quay về Fragment
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            loadWordFiles();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadWordFiles();
        } else {
            Toast.makeText(requireContext(), "Không có quyền đọc file!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadWordFiles() {
        container.removeAllViews();

        File dir = requireActivity().getExternalFilesDir(null);
        if (dir == null || !dir.exists()) {
            TextView tv = new TextView(requireContext());
            tv.setText("Thư mục lưu file không tồn tại");
            tv.setTextColor(Color.WHITE);
            tv.setPadding(20, 20, 20, 20);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            container.addView(tv);
            return;
        }

        File[] allFiles = dir.listFiles();
        if (allFiles == null || allFiles.length == 0) {
            TextView tv = new TextView(requireContext());
            tv.setText("Không có file nào trong thư mục app");
            tv.setPadding(20, 20, 20, 20);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            container.addView(tv);
            return;
        }

        List<File> docxFiles = new ArrayList<>();
        for (File file : allFiles) {
            if (file.isFile() && (file.getName().toLowerCase().endsWith(".docx") || file.getName().toLowerCase().endsWith(".pdf"))) {
                docxFiles.add(file);
            }
        }

        if (docxFiles.isEmpty()) {
            TextView tv = new TextView(requireContext());
            tv.setText("Không có file Word (.docx) nào trong thư mục");
            tv.setPadding(20, 20, 20, 20);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            container.addView(tv);
            return;
        }

        Collections.sort(docxFiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });

        for (File file : docxFiles) {
            LinearLayout itemLayout = new LinearLayout(requireContext());
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(20, 20, 20, 20);
            itemLayout.setGravity(Gravity.CENTER_VERTICAL);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ContextCompat.getColor(requireContext(), R.color.primary_color));
            itemLayout.setBackground(bg);

            ImageView icon = new ImageView(requireContext());

            if (file.getName().toLowerCase().endsWith(".docx")) {
                icon.setImageResource(R.drawable.word_icon);
            } else {
                icon.setImageResource(R.drawable.pdf_icon);
            }

            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(80, 80);
            iconParams.setMargins(0, 0, 30, 0);
            icon.setLayoutParams(iconParams);
            itemLayout.addView(icon);

            TextView tv = new TextView(requireContext());
            tv.setText(file.getName());
            tv.setTextSize(20);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            tvParams.gravity = Gravity.CENTER_VERTICAL;
            tv.setLayoutParams(tvParams);
            itemLayout.addView(tv);

            ImageButton menu = new ImageButton(requireContext());
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
            menu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(requireContext(), v);
                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.delete_file) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Xóa file")
                                .setMessage("Bạn có chắc muốn xóa không ?")
                                .setPositiveButton("Có", (dialog, which) -> {
                                    boolean deleted = file.delete();
                                    if (deleted) {
                                        Toast.makeText(requireContext(), "Đã xóa thành công: " + file.getName(), Toast.LENGTH_SHORT).show();
                                        loadWordFiles();
                                    } else {
                                        Toast.makeText(requireContext(), "Lỗi: Không thể xóa file " + file.getName(), Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("Không", null)
                                .show();
                        return true;
                    }

                    if (itemId == R.id.share_file) {
                        Intent intent = new Intent(getActivity(), ShareFileActivity.class);
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
        Uri uri = FileProvider.getUriForFile(requireContext(), requireActivity().getPackageName() + ".provider", file);
        intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "Không có ứng dụng mở file Word", Toast.LENGTH_SHORT).show();
        }
    }
}
