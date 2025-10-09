package com.example.android_project;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class ShareFileActivity extends AppCompatActivity{
    private static final int REQUEST_CODE_PICK_FILE = 1;
    private Uri selectedFileUri = null;
    private TextView fileNameTextView;
    private TextView serverStatusTextView;
    private FileHttpServer httpServer;

    //Lưu data từ FileManageActivity
    private File selectedFile;
    private String filePath;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_file);

        Button btnShareFile = findViewById(R.id.btnShare);
        fileNameTextView = findViewById(R.id.fileNameTextView);
        serverStatusTextView = findViewById(R.id.serverStatusTextView);

        // Nhận dữ liệu file từ FileManageActivity
        receiveFileFromIntent();

        // Nút chia sẻ
        btnShareFile.setOnClickListener(v -> {
            if (selectedFile != null && selectedFile.exists()) {
                startHttpServer();
            } else {
                Toast.makeText(this, "Không có file để chia sẻ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void receiveFileFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            filePath = intent.getStringExtra("file_path");
            fileName = intent.getStringExtra("file_name");

            if (filePath != null && fileName != null) {
                selectedFile = new File(filePath);

                // Convert file thành URI
                selectedFileUri = Uri.fromFile(selectedFile);

                fileNameTextView.setText(fileName);

                // Check file
                if (selectedFile.exists()) {
                    serverStatusTextView.setText("Trạng thái: File sẵn sàng để chia sẻ");
                } else {
                    serverStatusTextView.setText("Trạng thái: Lỗi, File lỗi");
                }
            } else {
                // Lỗi nếu file không lấy đc từ FileManageActivity
                fileNameTextView.setText("Chưa chọn file");
                serverStatusTextView.setText("Trạng thái: Chưa có file để chia sẻ");
            }
        }
    }

//    private void shareFile() {
//        if (selectedFile != null && selectedFile.exists()) {
//            Intent shareIntent = new Intent(Intent.ACTION_SEND);
//            shareIntent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
//
//            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", selectedFile);
//            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
//            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//            startActivity(Intent.createChooser(shareIntent, "Chia sẻ file qua"));
//        }
//    }

    private void startHttpServer() {
        try {
            int port = 8080;
            if (httpServer != null) {
                httpServer.stop();
            }

            // Đẩy URI vào server
            httpServer = new FileHttpServer(port, this, selectedFileUri, fileName);
            httpServer.start();

            // Lấy IP cho hiển thị URL
            String ipAddress = getIPAddress();
            String serverUrl = "http://" + ipAddress + ":" + port;

            serverStatusTextView.setText("Server đang chạy: " + serverUrl);
            //Toast.makeText(this, "Server khởi động thành công!\nURL: " + serverUrl, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            serverStatusTextView.setText("Server lỗi: " + e.getMessage());
           //Toast.makeText(this, "Không thể khởi động server: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    //  lấy IP
    private String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<java.net.InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (java.net.InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && sAddr.contains(".")) { // IPv4
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ShareFileActivity", "Error getting IP", e);
        }
        return "127.0.0.1";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpServer != null) {
            httpServer.stop();
        }
    }


}
