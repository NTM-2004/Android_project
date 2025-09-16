package com.example.android_project;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.Rect;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SelectFormatActivity extends AppCompatActivity {

    private Button btnSelectImage, btnSaveWord;
    private TextView tvResult;
    private static final int IMAGE_PICK_CODE = 1000;

    private String jsonString; // keep recognized text

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_format);

        btnSelectImage = findViewById(R.id.btn_select_image);
        btnSaveWord = findViewById(R.id.btn_save_word);
        tvResult = findViewById(R.id.tv_result);

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, IMAGE_PICK_CODE);
        });

        btnSaveWord.setOnClickListener(v -> {
            if (jsonString == null) {
                Toast.makeText(this, "Hãy chọn ảnh trước", Toast.LENGTH_SHORT).show();
            } else {
                createWordFile(jsonString);
            }
        });

    }

    private void createWordFile(String jsonData) {
        try {
            JsonObject root = new Gson().fromJson(jsonData, JsonObject.class);
            JsonArray blocks = root.getAsJsonArray("blocks");

            // 1. Create Word document
            XWPFDocument document = new XWPFDocument();

            for (int i = 0; i < blocks.size(); i++) {
                JsonObject block = blocks.get(i).getAsJsonObject();
                String blockText = block.get("text").getAsString();

                // Create paragraph for block
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(blockText);
                run.addCarriageReturn();
            }

            // 2. Save file
            File wordFile = new File(getFilesDir(), "recognized_text.docx");
            FileOutputStream out = new FileOutputStream(wordFile);
            document.write(out);
            out.close();
            document.close();

            Toast.makeText(this, "Đã lưu file Word", Toast.LENGTH_SHORT).show();

            // 3. Preview file
            previewWordFile(wordFile);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi tạo file Word: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void previewWordFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this, "Không có ứng dụng xem Word. File được lưu ở: " + file.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    InputImage image = InputImage.fromFilePath(this, imageUri);
                    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                    recognizer.process(image)
                            .addOnSuccessListener(visionText -> {
                                JsonArray blocksArray = new JsonArray();

                                for (Text.TextBlock block : visionText.getTextBlocks()) {
                                    JsonObject blockJson = new JsonObject();
                                    blockJson.addProperty("text", block.getText());

                                    Rect rect = block.getBoundingBox();
                                    if (rect != null) {
                                        blockJson.addProperty("left", rect.left);
                                        blockJson.addProperty("top", rect.top);
                                        blockJson.addProperty("right", rect.right);
                                        blockJson.addProperty("bottom", rect.bottom);
                                        blockJson.addProperty("width", rect.width());
                                        blockJson.addProperty("height", rect.height());
                                    }

                                    JsonArray linesArray = new JsonArray();
                                    for (Text.Line line : block.getLines()) {
                                        JsonObject lineJson = new JsonObject();
                                        lineJson.addProperty("text", line.getText());
                                        linesArray.add(lineJson);
                                    }
                                    blockJson.add("lines", linesArray);
                                    blocksArray.add(blockJson);
                                }

                                JsonObject lastJsonResult = new JsonObject();
                                lastJsonResult.add("blocks", blocksArray);
                                jsonString = new Gson().toJson(lastJsonResult);

                                tvResult.setText(jsonString);

                            })
                            .addOnFailureListener(e -> tvResult.setText("Lỗi: " + e.getMessage()));

                } catch (IOException e) {
                    e.printStackTrace();
                    tvResult.setText("Lỗi đọc ảnh: " + e.getMessage());
                }
            }
        }
    }
}
