package com.example.android_project;

import static com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG;
import static com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

public class MainActivity extends AppCompatActivity {

    private ImageView imgSelect;
    private ImageView imgImport;
    private Uri imageUri;

    // Phương thức khởi tạo
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgSelect = findViewById(R.id.fab_camera);
        imgImport = findViewById(R.id.import_image);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navbar);

        imgSelect.setOnClickListener(v -> {
            // Tạo Document Scanner
            GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                    .setScannerMode(SCANNER_MODE_FULL)
                    .setGalleryImportAllowed(true)
                    .setPageLimit(1)
                    .setResultFormats(RESULT_FORMAT_JPEG)
                    .build();

            GmsDocumentScanner scanner = GmsDocumentScanning.getClient(options);

            scanner.getStartScanIntent(MainActivity.this)
                    .addOnSuccessListener(intentSender -> {
                        try {
                            // Dùng IntentSender(PendingIntent)
                            startIntentSenderForResult(
                                    intentSender,
                                    1,
                                    null, 0, 0, 0
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .addOnFailureListener(Throwable::printStackTrace);

            // Method cũ
            //Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Bắt đầu Camera, chờ kết quả, PICK_CAMERA (101) để nhận dạng kết quả.
            //startActivityForResult(cameraIntent, 1);
        });

        imgImport.setOnClickListener(v -> {
            // Tạo intent yêu cầu lấy ảnh trong thư mục
            Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            //Bắt đầu thư viện, chờ kết quả, IMAGE_PICK_CODE (100) để nhận dạng kết quả.
            startActivityForResult(gallery, 2);
        });

        bottomNavigation.setOnItemSelectedListener(menuItem -> {
            int item= menuItem.getItemId();
            if(item == R.id.homePage){
                return true;
            }else if(item == R.id.filePage){
                Intent intent = new Intent(MainActivity.this, FileManageActivity.class);
                startActivity(intent);
            }
            return false;
        });
    }

    // Method gọi sau khi có kết quả của activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Kiểm tra xem có thành công ko
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == 2) {
                imageUri = data.getData();
            } else if (requestCode == 1) {
                // Lấy dữ liệu từ Document Scanner
                GmsDocumentScanningResult result =
                        GmsDocumentScanningResult.fromActivityResultIntent(data);

                imageUri = result.getPages().get(0).getImageUri();

            }
            if (imageUri != null) {
                Intent intent = new Intent(MainActivity.this, SelectFormatActivity.class);
                // Nếu cần mở rộng, thay = ArrayList<>
                intent.putExtra("imageUri", imageUri.toString());
                startActivity(intent);
            }
        }
    }
}
