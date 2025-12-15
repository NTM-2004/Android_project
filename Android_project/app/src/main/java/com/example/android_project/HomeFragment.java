package com.example.android_project;

import static com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG;
import static com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF;
import static com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

public class HomeFragment extends Fragment {

    private ImageView imgSelect;
    private ImageView imgImport;
    private ImageView pdfExport;
    private ImageView imgTranslate;
    private Uri imageUri;
    private Uri pdfUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        imgSelect = view.findViewById(R.id.fab_camera);
        imgImport = view.findViewById(R.id.import_image);
        imgTranslate = view.findViewById(R.id.translate_button);
        pdfExport = view.findViewById(R.id.export_pdf);

        setupClickListeners();

        return view;
    }

    private void setupClickListeners() {
        imgSelect.setOnClickListener(v -> {
            // Tạo Document Scanner
            GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                    .setScannerMode(SCANNER_MODE_FULL)
                    .setGalleryImportAllowed(true)
                    .setPageLimit(1)
                    .setResultFormats(RESULT_FORMAT_JPEG)
                    .build();

            GmsDocumentScanner scanner = GmsDocumentScanning.getClient(options);

            scanner.getStartScanIntent(requireActivity())
                    .addOnSuccessListener(intentSender -> {
                        try {
                            // Dùng IntentSender(PendingIntent)
                            startIntentSenderForResult(
                                    intentSender,
                                    1,
                                    null, 0, 0, 0, null
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .addOnFailureListener(Throwable::printStackTrace);
        });

        imgImport.setOnClickListener(v -> {
            // Tạo intent yêu cầu lấy ảnh trong thư mục
            Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            //Bắt đầu thư viện, chờ kết quả, IMAGE_PICK_CODE (100) để nhận dạng kết quả.
            startActivityForResult(gallery, 2);
        });

        pdfExport.setOnClickListener(v -> {
            // Tạo Document Scanner
            GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                    .setScannerMode(SCANNER_MODE_FULL)
                    .setGalleryImportAllowed(true)
                    .setPageLimit(1)
                    .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
                    .build();

            GmsDocumentScanner scanner = GmsDocumentScanning.getClient(options);

            scanner.getStartScanIntent(requireActivity())
                    .addOnSuccessListener(intentSender -> {
                        try {
                            // Dùng IntentSender(PendingIntent)
                            startIntentSenderForResult(
                                    intentSender,
                                    3,
                                    null, 0, 0, 0, null
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .addOnFailureListener(Throwable::printStackTrace);
        });

        imgTranslate.setOnClickListener(v -> {
            GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                    .setScannerMode(SCANNER_MODE_FULL)
                    .setGalleryImportAllowed(true)
                    .setPageLimit(1)
                    .setResultFormats(RESULT_FORMAT_JPEG)
                    .build();

            GmsDocumentScanner scanner = GmsDocumentScanning.getClient(options);

            scanner.getStartScanIntent(requireActivity())
                    .addOnSuccessListener(intentSender -> {
                        try {
                            startIntentSenderForResult(
                                    intentSender,
                                    4,
                                    null, 0, 0, 0, null
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .addOnFailureListener(Throwable::printStackTrace);
        });
    }

    // Method gọi sau khi có kết quả của activity
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Kiểm tra xem có thành công ko
        if (resultCode == getActivity().RESULT_OK && data != null) {
            if (requestCode == 2) {
                imageUri = data.getData();
            } else if (requestCode == 1) {
                // Lấy dữ liệu từ Document Scanner
                GmsDocumentScanningResult result =
                        GmsDocumentScanningResult.fromActivityResultIntent(data);

                imageUri = result.getPages().get(0).getImageUri();

            } else if (requestCode == 4) {
                GmsDocumentScanningResult result =
                        GmsDocumentScanningResult.fromActivityResultIntent(data);
                imageUri = result.getPages().get(0).getImageUri();
            } else if (requestCode == 3) {

                GmsDocumentScanningResult result =
                        GmsDocumentScanningResult.fromActivityResultIntent(data);

                // Lấy file từ Document Scanner
                imageUri = result.getPages().get(0).getImageUri();
                pdfUri = result.getPdf().getUri();

            }

            if (imageUri != null && requestCode != 3 && requestCode != 4) {
                Intent intent = new Intent(getActivity(), SelectFormatActivity.class);
                // Nếu cần mở rộng, thay = ArrayList<>
                intent.putExtra("imageUri", imageUri.toString());
                startActivity(intent);
            } else if (imageUri != null && requestCode == 3) {
                Intent intent = new Intent(getActivity(), PdfExport.class);
                intent.putExtra("imageUri", imageUri.toString());
                intent.putExtra("pdfUri", pdfUri.toString());
                startActivity(intent);
            } else if (imageUri != null && requestCode == 4) {
                Intent intent = new Intent(getActivity(), TranslateActivity.class);
                intent.putExtra("IMAGE_URI", imageUri.toString());
                startActivity(intent);
            }
        }
    }
}
