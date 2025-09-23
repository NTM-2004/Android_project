package com.example.android_project;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Rect;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SelectFormatActivity extends AppCompatActivity {

    private ImageView imagePreview;
    private TextRecognizer recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_format);

        imagePreview = findViewById(R.id.preview_image);
        Button export = findViewById(R.id.export_button);

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        String uriString = getIntent().getExtras().getString("imageUri");
        if (uriString != null) {
            Uri imageUri = Uri.parse(uriString);
            imagePreview.setImageURI(imageUri);
            export.setOnClickListener(v -> {
                try{
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    InputImage image = InputImage.fromBitmap(bitmap, 0);

                    recognizer.process(image)
                            .addOnSuccessListener(result -> {
                                XWPFDocument document = new XWPFDocument();

                                CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
                                CTPageMar pageMar = sectPr.addNewPgMar();
                                // Căn lề khổ A4 (4 cạnh căn lề 1'')
                                pageMar.setLeft(BigInteger.valueOf(1440));
                                pageMar.setRight(BigInteger.valueOf(1440));
                                pageMar.setTop(BigInteger.valueOf(1440));
                                pageMar.setBottom(BigInteger.valueOf(1440));

                                XWPFParagraph paragraph = document.createParagraph();
                                XWPFRun run = paragraph.createRun();

                                List<Text.Line> lines = new ArrayList<>();
                                for(Text.TextBlock block : result.getTextBlocks()){
                                    lines.addAll(block.getLines());
                                }
                                // Sắp xếp các line theo thứ tự .top
                                lines.sort(Comparator.comparingInt(line -> line.getBoundingBox().top));
//
                                try{
                                    Rect lastBox = null;
                                    String s = "";
                                    int fontSize = 10;
                                    int imageWidth = image.getWidth();

                                    //Sắp xếp line theo thứ tự .left
                                    Text.Line minLine = Collections.min(lines, Comparator.comparingInt(line -> line.getBoundingBox().left));
                                    double minLeft = minLine.getBoundingBox().left;
                                    for(Text.Line line : lines){
                                        String text = line.getText().trim();
                                        Rect box = line.getBoundingBox();
                                        // cỡ chữ = chiều rộng bouding box / số chữ
                                        double letterSize = (box.right - box.left) / text.length();
                                        // A4 rộng 8.3'' , 1pt = 1/72 inch
                                        fontSize = (int) Math.round((8.3 * 72 * letterSize) / imageWidth + 1);
                                        run.setFontSize(fontSize);
                                        run.setFontFamily("Courier New");

                                        int spaceNumber = 0;
                                        // điều kiên dòng đầu tiên
                                        if(lastBox == null){
                                            spaceNumber = (int) Math.round((box.left - minLeft) / letterSize);
                                        }else{
                                            // điều kiện xuống dòng (bouding box .top không cao hơn trung tâm của dòng trước)
                                            if(box.top > (lastBox.bottom + lastBox.top)/2){
                                                run.setText(s);
                                                s = "";
                                                run.addBreak();

                                                spaceNumber = (int) Math.round((box.left - minLeft) / letterSize);
                                            }else{
                                                spaceNumber = (int) Math.round((box.left - lastBox.right) / letterSize);
                                            }
                                        }
                                        for (int i = 0; i < spaceNumber; i++) {
                                            s += " ";
                                        }
                                        s += text;
                                        lastBox = box;
                                    }
                                    // nhập dòng cuối
                                    run.setFontSize(fontSize);
                                    run.setFontFamily("Courier New");
                                    run.setText(s);

                                    File file = new File(getExternalFilesDir(null), "OCR_Result.docx");
                                    FileOutputStream out = new FileOutputStream(file);
                                    document.write(out);
                                    out.close();
                                    document.close();

                                    Toast.makeText(this, "Xuất file thành công: " + file.getAbsolutePath(),
                                            Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi OCR: "
                                    + e.getMessage(), Toast.LENGTH_SHORT).show());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }else{
            Toast.makeText(this, "không nhận được ảnh", Toast.LENGTH_LONG).show();
        }

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navbar);
        bottomNavigation.setOnItemSelectedListener(menuItem -> {
            int item= menuItem.getItemId();
            if(item == R.id.homePage){
                startActivity(new Intent(SelectFormatActivity.this, MainActivity.class));
                return true;
            }else if(item == R.id.filePage){

            }
            return false;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }
}