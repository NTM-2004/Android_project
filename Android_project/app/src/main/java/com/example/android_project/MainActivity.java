package com.example.android_project;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.Rect;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Button btnSelect;
    private TextView tvResult;
    private static final int IMAGE_PICK_CODE = 1000;

    // Keep JSON in memory for later use
    private JsonObject lastJsonResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnSelect = findViewById(R.id.btn_select_image);
        tvResult = findViewById(R.id.tv_result);

        btnSelect.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, IMAGE_PICK_CODE);

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets ;

            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null){
            Uri imageUri = data.getData();
            if(imageUri != null){
                try {
                    InputImage image = InputImage.fromFilePath(this, imageUri);
                    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                    recognizer.process(image)
                            .addOnSuccessListener(visionText -> {
                                // Build JSON
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
                                        Rect lineRect = line.getBoundingBox();
                                        if (lineRect != null) {
                                            lineJson.addProperty("left", lineRect.left);
                                            lineJson.addProperty("top", lineRect.top);
                                            lineJson.addProperty("right", lineRect.right);
                                            lineJson.addProperty("bottom", lineRect.bottom);
                                            lineJson.addProperty("width", lineRect.width());
                                            lineJson.addProperty("height", lineRect.height());
                                        }
                                        linesArray.add(lineJson);
                                    }
                                    blockJson.add("lines", linesArray);
                                    blocksArray.add(blockJson);
                                }

                                lastJsonResult = new JsonObject(); // save in memory
                                lastJsonResult.add("blocks", blocksArray);

                                String jsonString = new Gson().toJson(lastJsonResult);
                                tvResult.setText(jsonString); // show JSON on screen

                            })
                            .addOnFailureListener(e -> tvResult.setText("Lỗi: " + e.getMessage()));

                } catch (IOException e) {
                    e.printStackTrace();
                    tvResult.setText("Lỗi đọc ảnh: " + e.getMessage());
                }
            }
        }
    }

    // Optional: getter to use JSON elsewhere
    public JsonObject getLastJsonResult() {
        return lastJsonResult;
    }
}
