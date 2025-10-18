package com.example.android_project;

import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TranslateActivity extends AppCompatActivity {

    private ImageView previewImage;
    private RelativeLayout overlayLayout;
    private Spinner sourceSpinner, targetSpinner;
    private Button translateButton;
    private Uri currentImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        previewImage = findViewById(R.id.preview_image);
        overlayLayout = findViewById(R.id.overlay_layout);
        sourceSpinner = findViewById(R.id.spinner_source_lang);
        targetSpinner = findViewById(R.id.spinner_target_lang);
        translateButton = findViewById(R.id.translate_button);

        String uriString = getIntent().getStringExtra("IMAGE_URI");
        if (uriString != null) {
            currentImageUri = Uri.parse(uriString);
            previewImage.setImageURI(currentImageUri);
        } else {
            Toast.makeText(this, "Không tìm thấy ảnh", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupLanguageSpinners();

        // 3. Xử lý nút Dịch
        translateButton.setOnClickListener(v -> {
            if (currentImageUri != null) {
                LanguageItem sourceLang = (LanguageItem) sourceSpinner.getSelectedItem();
                LanguageItem targetLang = (LanguageItem) targetSpinner.getSelectedItem();
                if (sourceLang != null && targetLang != null) {
                    processImageForTranslation(currentImageUri, sourceLang.code, targetLang.code);
                }
            }
        });
    }

    private void setupLanguageSpinners() {
        List<LanguageItem> languages = new ArrayList<>();
        languages.add(new LanguageItem("English", TranslateLanguage.ENGLISH));
        languages.add(new LanguageItem("Vietnamese", TranslateLanguage.VIETNAMESE));
        languages.add(new LanguageItem("French", TranslateLanguage.FRENCH));
        languages.add(new LanguageItem("Spanish", TranslateLanguage.SPANISH));

        ArrayAdapter<LanguageItem> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                languages
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceSpinner.setAdapter(adapter);
        targetSpinner.setAdapter(adapter);

        // Tìm vị trí của ngôn ngữ tiếng Anh
        int englishPos = -1;
        int vietnamesePos = -1;
        for (int i = 0; i < languages.size(); i++) {
            if (languages.get(i).code.equals(TranslateLanguage.ENGLISH)) {
                englishPos = i;
            }
            if (languages.get(i).code.equals(TranslateLanguage.VIETNAMESE)) {
                vietnamesePos = i;
            }
        }
        if (englishPos != -1) sourceSpinner.setSelection(englishPos);
        if (vietnamesePos != -1) targetSpinner.setSelection(vietnamesePos);
    }

    private void processImageForTranslation(Uri imageUri, String sourceLangCode, String targetLangCode) {
        // Xóa các TextView dịch cũ
        overlayLayout.removeAllViews();

        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        // Dịch từng khối văn bản
                        for (Text.TextBlock block : visionText.getTextBlocks()) {
                            String originalText = block.getText();
                            Rect boundingBox = block.getBoundingBox();
                            if (boundingBox != null) {
                                translateAndOverlayText(originalText, boundingBox, sourceLangCode, targetLangCode, image.getWidth(), image.getHeight());
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Nhận diện văn bản thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi đọc ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void translateAndOverlayText(String originalText, Rect originalBounds, String sourceLangCode, String targetLangCode, int originalWidth, int originalHeight) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLangCode)
                .setTargetLanguage(targetLangCode)
                .build();
        final Translator translator = Translation.getClient(options);

        // Tải model (yêu cầu internet lần đầu)
        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(v -> {
                    // Dịch văn bản
                    translator.translate(originalText)
                            .addOnSuccessListener(translatedText -> {
                                // 4. Hiển thị kết quả dịch đè lên ảnh
                                previewImage.post(() -> {
                                    overlayTranslatedText(translatedText, originalBounds, originalWidth, originalHeight);
                                });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(TranslateActivity.this, "Lỗi dịch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TranslateActivity.this, "Lỗi tải model dịch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void overlayTranslatedText(String translatedText, Rect originalBounds, int originalWidth, int originalHeight) {
        // Lấy kích thước hiện tại của ImageView
        int ivWidth = previewImage.getWidth();
        int ivHeight = previewImage.getHeight();

        // Tính toán tỷ lệ ảnh
        float imageRatio = (float) originalWidth / originalHeight;
        float viewRatio = (float) ivWidth / ivHeight;

        float scale;
        int offsetX, offsetY;

        if (imageRatio > viewRatio) { // Image rộng hơn View -> fit theo chiều ngang
            scale = (float) ivWidth / originalWidth;
            offsetX = 0;
            offsetY = (int) ((ivHeight - originalHeight * scale) / 2);
        } else { // Image cao hơn View -> fit theo chiều dọc
            scale = (float) ivHeight / originalHeight;
            offsetX = (int) ((ivWidth - originalWidth * scale) / 2);
            offsetY = 0;
        }

        // Ánh xạ tọa độ
        int left = (int) (originalBounds.left * scale) + offsetX;
        int top = (int) (originalBounds.top * scale) + offsetY;
        int right = (int) (originalBounds.right * scale) + offsetX;
        int bottom = (int) (originalBounds.bottom * scale) + offsetY;

        // Tạo TextView để hiển thị văn bản dịch
        TextView translatedTextView = new TextView(this);
        translatedTextView.setText(translatedText);
        translatedTextView.setTextColor(Color.WHITE);
        translatedTextView.setTextSize(14);
        translatedTextView.setBackgroundColor(Color.parseColor("#99000000")); // Nền đen bán trong suốt
        translatedTextView.setPadding(5, 5, 5, 5);

        // Đặt vị trí và kích thước cho TextView
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                right - left, // Chiều rộng bằng chiều rộng box dịch
                RelativeLayout.LayoutParams.WRAP_CONTENT // Chiều cao tự động
        );
        params.leftMargin = left;
        params.topMargin = top;

        overlayLayout.addView(translatedTextView, params);
    }
}

class LanguageItem {
    public String name;
    public String code;

    public LanguageItem(String name, String code) {
        this.name = name;
        this.code = code;
    }

    @Override
    public String toString() {
        return name;
    }
}