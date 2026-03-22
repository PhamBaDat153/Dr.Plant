package com.nckh.drplant.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nckh.drplant.Models.DiagnosisHistoryItem;
import com.nckh.drplant.Models.DiagnosisResponse;

import java.lang.reflect.Type;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DiagnosisHistoryManager {

    public static final String FILTER_ALL = "all";
    public static final String FILTER_DURIAN = "durian";
    public static final String FILTER_COFFEE = "coffee";

    private static final String PREFS_NAME = "diagnosis_history_prefs";
    private static final String KEY_HISTORY = "history_items";
    private static final int MAX_HISTORY_ITEMS = 100;
    private static final Gson GSON = new Gson();
    private static final Type HISTORY_LIST_TYPE = new TypeToken<List<DiagnosisHistoryItem>>() { }.getType();

    private DiagnosisHistoryManager() {
        // Utility class
    }

    public static void saveHistoryItem(@NonNull Context context,
                                       @NonNull DiagnosisResponse diagnosisResponse,
                                       @Nullable String imagePath) {
        List<DiagnosisHistoryItem> historyItems = getHistoryItems(context);

        DiagnosisHistoryItem historyItem = new DiagnosisHistoryItem();
        historyItem.id = String.valueOf(System.currentTimeMillis());
        historyItem.viewedAt = System.currentTimeMillis();
        historyItem.imagePath = imagePath;
        historyItem.message = diagnosisResponse.message;
        historyItem.diagnosisResponse = diagnosisResponse;

        if (diagnosisResponse.prediction != null) {
            historyItem.predictionName = diagnosisResponse.prediction.className;
            historyItem.plantTypeRaw = diagnosisResponse.prediction.plantType;
            historyItem.plantTypeNormalized = normalizePlantType(diagnosisResponse.prediction.plantType);
            historyItem.confidence = diagnosisResponse.prediction.confidence;
            historyItem.healthy = diagnosisResponse.prediction.isHealthy;
        } else {
            historyItem.plantTypeNormalized = FILTER_ALL;
        }

        if (diagnosisResponse.disease != null) {
            historyItem.diseaseName = diagnosisResponse.disease.name;
        }

        historyItems.add(0, historyItem);
        if (historyItems.size() > MAX_HISTORY_ITEMS) {
            historyItems = new ArrayList<>(historyItems.subList(0, MAX_HISTORY_ITEMS));
        }

        saveHistoryItems(context, historyItems);
    }

    @NonNull
    public static List<DiagnosisHistoryItem> getHistoryItems(@NonNull Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String historyJson = sharedPreferences.getString(KEY_HISTORY, "");
        if (TextUtils.isEmpty(historyJson)) {
            return new ArrayList<>();
        }

        List<DiagnosisHistoryItem> historyItems = GSON.fromJson(historyJson, HISTORY_LIST_TYPE);
        return historyItems != null ? new ArrayList<>(historyItems) : new ArrayList<>();
    }

    @NonNull
    public static List<DiagnosisHistoryItem> filterHistory(@NonNull List<DiagnosisHistoryItem> sourceItems,
                                                           @NonNull String filter) {
        if (FILTER_ALL.equals(filter)) {
            return new ArrayList<>(sourceItems);
        }

        List<DiagnosisHistoryItem> filteredItems = new ArrayList<>();
        for (DiagnosisHistoryItem item : sourceItems) {
            if (item == null) {
                continue;
            }
            String normalizedType = item.plantTypeNormalized;
            if (TextUtils.isEmpty(normalizedType)) {
                normalizedType = normalizePlantType(item.plantTypeRaw);
            }
            if (filter.equals(normalizedType)) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

    @NonNull
    public static String normalizePlantType(@Nullable String rawPlantType) {
        if (TextUtils.isEmpty(rawPlantType)) {
            return FILTER_ALL;
        }

        String normalized = Normalizer.normalize(rawPlantType, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z]", "");

        if (normalized.contains("saurieng") || normalized.contains("durian")) {
            return FILTER_DURIAN;
        }
        if (normalized.contains("coffee") || normalized.contains("cafe") || normalized.contains("caphe")) {
            return FILTER_COFFEE;
        }
        return FILTER_ALL;
    }

    private static void saveHistoryItems(@NonNull Context context, @NonNull List<DiagnosisHistoryItem> historyItems) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_HISTORY, GSON.toJson(historyItems)).apply();
    }
}

