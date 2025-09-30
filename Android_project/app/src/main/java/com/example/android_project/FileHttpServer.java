package com.example.android_project;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import fi.iki.elonen.NanoHTTPD;

public class FileHttpServer extends NanoHTTPD {
    private static final String TAG = "FileHttpServer";
    private final Context context;
    private final Uri fileUri;
    private final String fileName;
    private final String originalFileName;

    public FileHttpServer(int port, Context context, Uri fileUri, String fileName) {
        super(port);
        this.context = context;
        this.fileUri = fileUri;
        this.fileName = fileName;
        this.originalFileName = getOriginalFileName(context, fileUri);
        Log.d(TAG, "Server created on port " + port + " for file: " + originalFileName);
    }

    private String getOriginalFileName(Context context, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting original filename", e);
            }
        }
        
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        
        // Fallback nếu vẫn không có tên file
        if (result == null || result.isEmpty()) {
            result = "downloaded_file";
        }
        
        Log.d(TAG, "Original filename determined: " + result);
        return result;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        String method = session.getMethod().toString();
        String remoteIp = session.getRemoteIpAddress();

        Log.i(TAG, "Incoming request: " + method + " " + uri + " from " + remoteIp);

        if ("/file".equals(uri)) {
            Log.d(TAG, "File download request for: " + originalFileName);
            try {
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(fileUri, "r");
                if (pfd == null) {
                    Log.e(TAG, "Could not open file descriptor for URI: " + fileUri);
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found");
                }

                InputStream is = new FileInputStream(pfd.getFileDescriptor());
                long fileSize = pfd.getStatSize();

                Log.i(TAG, "Serving file: " + originalFileName + " (" + fileSize + " bytes) to " + remoteIp);

                // Tự động detect MIME type dựa trên extension
                String mimeType = getMimeType(originalFileName);
                
                Response response = newFixedLengthResponse(Response.Status.OK, mimeType, is, fileSize);
                response.addHeader("Content-Disposition", "attachment; filename=\"" + originalFileName + "\"");
                response.addHeader("Content-Length", String.valueOf(fileSize));
                response.addHeader("Accept-Ranges", "bytes");

                Log.d(TAG, "File response created successfully with MIME type: " + mimeType);
                return response;

            } catch (Exception e) {
                Log.e(TAG, "Error serving file: " + originalFileName, e);
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "Landing page request from " + remoteIp);
            // Landing page with better UI
            String html = "<!DOCTYPE html><html><head>" +
                    "<title>File Sharing</title>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f5f5f5; }" +
                    ".container { max-width: 500px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                    ".download-btn { display: inline-block; padding: 15px 30px; background: #007acc; color: white; text-decoration: none; border-radius: 5px; font-size: 18px; margin: 20px 0; }" +
                    ".download-btn:hover { background: #005a9e; }" +
                    ".info { color: #666; font-size: 14px; margin-top: 20px; }" +
                    "</style></head><body>" +
                    "<div class='container'>" +
                    "<h2>📁 File Sharing Server</h2>" +
                    "<p>Server đang hoạt động thành công!</p>" +
                    "<a href='/file' class='download-btn'>⬇️ Tải xuống: " + originalFileName + "</a>" +
                    "<div class='info'><p>Tên file: <strong>" + originalFileName + "</strong></p></div>" +
                    "</div></body></html>";

            Log.d(TAG, "Returning landing page to " + remoteIp);
            return newFixedLengthResponse(Response.Status.OK, "text/html", html);
        }
    }
    
    private String getMimeType(String fileName) {
        String extension = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }
        
        switch (extension) {
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "pdf": return "application/pdf";
            case "txt": return "text/plain";
            case "mp4": return "video/mp4";
            case "mp3": return "audio/mpeg";
            case "zip": return "application/zip";
            case "doc": case "docx": return "application/msword";
            case "xls": case "xlsx": return "application/vnd.ms-excel";
            case "ppt": case "pptx": return "application/vnd.ms-powerpoint";
            default: return "application/octet-stream";
        }
    }

    @Override
    public void start() throws IOException {
        super.start();
        Log.i(TAG, "HTTP Server started on port " + getListeningPort());
    }

    @Override
    public void stop() {
        super.stop();
        Log.i(TAG, "HTTP Server stopped");
    }
}
