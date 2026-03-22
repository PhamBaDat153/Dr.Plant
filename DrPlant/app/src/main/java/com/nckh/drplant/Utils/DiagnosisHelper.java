package com.nckh.drplant.Utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.Gson;
import com.nckh.drplant.DiagnosisActivity;
import com.nckh.drplant.Models.DiagnosisResponse;

public class DiagnosisHelper {

    private static final String TAG = "DiagnosisHelper";

    private DiagnosisHelper() {
        // Utility class
    }

    /**
     * Launch the DiagnosisActivity with the diagnosis data
     * @param context The context from which to launch the activity
     * @param jsonResponse The JSON response string
     */
    public static void launchDiagnosisActivity(Context context, String jsonResponse) {
        launchDiagnosisActivity(context, jsonResponse, null, true);
    }

    public static void launchDiagnosisActivity(Context context, String jsonResponse, String imagePath) {
        launchDiagnosisActivity(context, jsonResponse, imagePath, true);
    }

    public static void launchDiagnosisActivity(Context context, String jsonResponse, String imagePath, boolean saveToHistory) {
        try {
            Gson gson = new Gson();
            DiagnosisResponse diagnosisData = gson.fromJson(jsonResponse, DiagnosisResponse.class);

            Intent intent = new Intent(context, DiagnosisActivity.class);
            intent.putExtra(DiagnosisActivity.EXTRA_DIAGNOSIS_JSON, jsonResponse);
            intent.putExtra(DiagnosisActivity.EXTRA_DIAGNOSIS_DATA, diagnosisData);
            intent.putExtra(DiagnosisActivity.EXTRA_CAPTURED_IMAGE_PATH, imagePath);
            intent.putExtra(DiagnosisActivity.EXTRA_SAVE_TO_HISTORY, saveToHistory);
            addNewTaskFlagIfNeeded(context, intent);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Unable to launch diagnosis activity from JSON", e);
        }
    }

    /**
     * Launch the DiagnosisActivity with a DiagnosisResponse object
     * @param context The context from which to launch the activity
     * @param diagnosisData The DiagnosisResponse object
     */
    public static void launchDiagnosisActivity(Context context, DiagnosisResponse diagnosisData) {
        launchDiagnosisActivity(context, diagnosisData, null, true);
    }

    public static void launchDiagnosisActivity(Context context, DiagnosisResponse diagnosisData, String imagePath) {
        launchDiagnosisActivity(context, diagnosisData, imagePath, true);
    }

    public static void launchDiagnosisActivity(Context context, DiagnosisResponse diagnosisData, String imagePath, boolean saveToHistory) {
        try {
            Intent intent = new Intent(context, DiagnosisActivity.class);
            intent.putExtra(DiagnosisActivity.EXTRA_DIAGNOSIS_DATA, diagnosisData);
            intent.putExtra(DiagnosisActivity.EXTRA_CAPTURED_IMAGE_PATH, imagePath);
            intent.putExtra(DiagnosisActivity.EXTRA_SAVE_TO_HISTORY, saveToHistory);
            addNewTaskFlagIfNeeded(context, intent);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Unable to launch diagnosis activity from object", e);
        }
    }

    private static void addNewTaskFlagIfNeeded(Context context, Intent intent) {
        if (!(context instanceof android.app.Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
    }
}

