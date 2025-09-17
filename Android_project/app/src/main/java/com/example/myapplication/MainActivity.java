package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 100;
    private Uri imageUri;
    private ImageView imgPreview;
    private TextView txtResult;
    private TextRecognizer recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnPick = findViewById(R.id.btnPick);
        Button btnOcr = findViewById(R.id.btnOcr);
        imgPreview = findViewById(R.id.imgPreview);
        txtResult = findViewById(R.id.txtResult);

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Chọn ảnh từ gallery
        btnPick.setOnClickListener(v -> {
            Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(gallery, PICK_IMAGE);
        });

        // Quét OCR
        btnOcr.setOnClickListener(v -> {
            if (imageUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    InputImage image = InputImage.fromBitmap(bitmap, 0);

                    recognizer.process(image)
                            .addOnSuccessListener(result -> {
                                List<Text.Line> allLines = new ArrayList<>();
                                for (Text.TextBlock block : result.getTextBlocks()) {
                                    allLines.addAll(block.getLines());
                                }

                                allLines.sort(Comparator.comparingInt(l -> l.getBoundingBox().top));

                                try {
                                    XWPFDocument document = new XWPFDocument();
                                    int imageWidth = image.getWidth();
                                    int centerImageX = imageWidth / 2; // tâm ảnh

                                    for (Text.Line line : allLines) {
                                        XWPFParagraph paragraph = document.createParagraph();

                                        // Ghép text từ các element
                                        StringBuilder lineText = new StringBuilder();
                                        List<Text.Element> elements = new ArrayList<>(line.getElements());
                                        elements.sort(Comparator.comparingInt(e -> e.getBoundingBox().left));
                                        for (Text.Element e : elements) {
                                            lineText.append(e.getText()).append(" ");
                                        }
                                        String text = lineText.toString().trim();

                                        XWPFRun run = paragraph.createRun();

                                        // --- Rule formatting ---
                                        if (text.toUpperCase().contains("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM")
                                                || text.toUpperCase().contains("ĐỘC LẬP - TỰ DO - HẠNH PHÚC")) {
                                            paragraph.setAlignment(ParagraphAlignment.CENTER);
                                            run.setBold(true);
                                            run.setFontSize(14);
                                            run.setText(text);
                                            continue;
                                        }

                                        if (text.matches(".*ngày \\d{1,2} tháng \\d{1,2} năm \\d{4}.*")) {
                                            paragraph.setAlignment(ParagraphAlignment.RIGHT);
                                            run.setItalic(true);
                                            run.setText(text);
                                            continue;
                                        }

                                        if (text.matches("^[-•]\\s.*") || text.matches("^\\d+\\..*")) {
                                            paragraph.setAlignment(ParagraphAlignment.LEFT);
                                            run.setText("• " + text.replaceFirst("^[-•]\\s*", "").trim());
                                            continue;
                                        }

                                        // --- Căn chỉnh dựa theo centerX ---
                                        int centerX = line.getBoundingBox().centerX();
                                        int offset = Math.abs(centerX - centerImageX);

                                        if (offset < imageWidth * 0.1) {
                                            paragraph.setAlignment(ParagraphAlignment.CENTER);
                                        } else if (centerX < centerImageX) {
                                            paragraph.setAlignment(ParagraphAlignment.LEFT);
                                        } else {
                                            paragraph.setAlignment(ParagraphAlignment.RIGHT);
                                        }

                                        run.setText(text);
                                    }

                                    File file = new File(getExternalFilesDir(null), "OCR_Result.docx");
                                    FileOutputStream out = new FileOutputStream(file);
                                    document.write(out);
                                    out.close();
                                    document.close();

                                    Toast.makeText(this, "Xuất file thành công: " + file.getAbsolutePath(),
                                            Toast.LENGTH_LONG).show();

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    Toast.makeText(this, "Lỗi xuất file: " + ex.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }

                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Lỗi OCR: " + e.getMessage(), Toast.LENGTH_SHORT).show());


//                    recognizer.process(image)
//                            .addOnSuccessListener(result -> {
//
//                                String resultText = result.getText();
//                                txtResult.setText(resultText.isEmpty() ? "Không nhận diện được chữ" : resultText);
//                            })
//                            .addOnFailureListener(e ->
//                                    Toast.makeText(this, "Lỗi OCR: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Vui lòng chọn ảnh trước", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imgPreview.setImageURI(imageUri);
        }
    }
}

