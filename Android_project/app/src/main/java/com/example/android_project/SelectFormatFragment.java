package com.example.android_project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SelectFormatFragment extends Fragment {

    private ImageView imagePreview;
    private TextRecognizer recognizer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_format, container, false);
        imagePreview = view.findViewById(R.id.preview_image);
        Button export = view.findViewById(R.id.export_button);

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        String uriString = getArguments().getString("imageUri");
        if (uriString != null) {
            Uri imageUri = Uri.parse(uriString);
            imagePreview.setImageURI(imageUri);
            export.setOnClickListener(v -> {
                try{
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
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
                                    Text.Line maxLine = Collections.max(lines, Comparator.comparingInt(line -> line.getBoundingBox().right));
                                    double minLeft = minLine.getBoundingBox().left;
                                    double maxRight = maxLine.getBoundingBox().right;
                                    for(Text.Line line : lines){
                                        String text = line.getText().trim();
                                        Rect box = line.getBoundingBox();
                                        // cỡ chữ = chiều rộng bouding box / số chữ
                                        double letterSize = ((double)box.right - (double)box.left) / text.length();
                                        // A4 rộng 8.3'' , 1pt = 1/72 inch
                                        fontSize = (int) Math.round((6.3 * 72 * letterSize) / (maxRight - minLeft) + 1);
                                        int spaceNumber = 0;
                                        // điều kiên dòng đầu tiên
                                        if(lastBox == null){
                                            run = paragraph.createRun();
                                            run.setFontSize(fontSize);
                                            run.setFontFamily("Courier New");

                                            spaceNumber = (int) Math.round((box.left - minLeft) / letterSize);
                                        }else{
                                            // điều kiện xuống dòng (bouding box .top không cao hơn trung tâm của dòng trước)
                                            if(box.top > (lastBox.bottom + lastBox.top)/2){
                                                run.setText(s);
                                                s = "";
                                                run.addBreak();

                                                run = paragraph.createRun();
                                                run.setFontSize(fontSize);
                                                run.setFontFamily("Courier New");

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
                                        Log.d("MLkit.Log", "Text: " + text + ": " + text.length());
                                        Log.d("MLkit.Log", "Image size: " + image.getWidth());
                                        Log.d("MLkit.Log", "Left min: " + minLeft);
                                        Log.d("MLkit.Log", "Right max: " + maxRight);
                                        Log.d("MLkit.Log", "Left: " + box.left + ", right: " + box.right);
                                        Log.d("MLkit.Log", "Letter size: " + letterSize);
                                        Log.d("MLkit.Log", "Front size: " + fontSize);
                                        Log.d("MLkit.Log", "Space number: " + spaceNumber);
                                        Log.d("MLkit.Log", "*******************");
                                    }
                                    // nhập dòng cuối
//                                    run = paragraph.createRun();
//                                    run.setFontSize(fontSize);
//                                    run.setFontFamily("Courier New");
                                    run.setText(s);

                                    File file = new File(requireContext().getExternalFilesDir(null), "OCR_Result.docx");
                                    FileOutputStream out = new FileOutputStream(file);
                                    document.write(out);
                                    out.close();
                                    document.close();

                                    Toast.makeText(requireContext(), "Xuất file thành công: " + file.getAbsolutePath(),
                                            Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .addOnFailureListener(e -> Toast.makeText(requireContext(), "Lỗi OCR: "
                                    + e.getMessage(), Toast.LENGTH_SHORT).show());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }else{
            Toast.makeText(requireContext(), "không nhận được ảnh", Toast.LENGTH_LONG).show();
        }

        return view;
    }
}