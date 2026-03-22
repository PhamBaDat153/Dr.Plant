package com.nckh.drplant.AiApi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.nckh.drplant.Models.DiagnosisResponse;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class DiagnosisApiClient {

    private static final MediaType DEFAULT_IMAGE_MEDIA_TYPE = Objects.requireNonNull(MediaType.parse("image/jpeg"));
    private static final Gson GSON = new Gson();
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    // Ngăn việc khởi tạo trực tiếp vì đây là lớp tiện ích chỉ chứa hàm tĩnh.
    private DiagnosisApiClient() {
        // Utility class
    }

    // Callback dùng để trả kết quả chẩn đoán thành công hoặc thất bại về nơi gọi.
    public interface DiagnosisCallback {
        void onSuccess(@NonNull DiagnosisResponse diagnosisResponse, @NonNull String rawJson);

        void onFailure(@NonNull String message, @Nullable Throwable throwable);
    }

    // Gửi file ảnh lên API dưới dạng multipart/form-data và chuyển phản hồi về callback.
    public static void diagnoseImage(@Nullable File imageFile, @NonNull DiagnosisCallback callback) {
        if (imageFile == null || !imageFile.exists() || !imageFile.isFile()) {
            callback.onFailure("Image file is missing or invalid.", null);
            return;
        }

        RequestBody fileBody = RequestBody.create(resolveMediaType(imageFile), imageFile);
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(ApiCall.DIAGNOSE_FILE_PART, imageFile.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(ApiCall.DIAGNOSE_URL)
                .post(requestBody)
                .build();

        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            // Xử lý trường hợp không thể kết nối tới máy chủ chẩn đoán.
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure("Unable to connect to diagnosis server.", e);
            }

            @Override
            // Đọc nội dung phản hồi, kiểm tra lỗi HTTP và parse JSON thành DiagnosisResponse.
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                String responseBody = null;
                try (Response closableResponse = response) {
                    if (closableResponse.body() != null) {
                        responseBody = closableResponse.body().string();
                    }

                    if (!closableResponse.isSuccessful()) {
                        callback.onFailure(
                                responseBody != null && !responseBody.trim().isEmpty()
                                        ? responseBody
                                        : "Diagnosis request failed with code " + closableResponse.code(),
                                null
                        );
                        return;
                    }

                    if (responseBody == null || responseBody.trim().isEmpty()) {
                        callback.onFailure("Diagnosis server returned an empty response.", null);
                        return;
                    }

                    DiagnosisResponse diagnosisResponse = GSON.fromJson(responseBody, DiagnosisResponse.class);
                    if (diagnosisResponse == null) {
                        callback.onFailure("Unable to parse diagnosis response.", null);
                        return;
                    }

                    callback.onSuccess(diagnosisResponse, responseBody);
                } catch (Exception exception) {
                    callback.onFailure("Unable to process diagnosis response.", exception);
                }
            }
        });
    }

    // Xác định MIME type phù hợp cho file ảnh, mặc định dùng image/jpeg nếu không suy ra được.
    @NonNull
    private static MediaType resolveMediaType(@NonNull File imageFile) {
        String guessedMimeType = URLConnection.guessContentTypeFromName(imageFile.getName());
        if (guessedMimeType == null || guessedMimeType.trim().isEmpty()) {
            return DEFAULT_IMAGE_MEDIA_TYPE;
        }

        MediaType resolvedMediaType = MediaType.parse(guessedMimeType);
        return resolvedMediaType != null ? resolvedMediaType : DEFAULT_IMAGE_MEDIA_TYPE;
    }
}

