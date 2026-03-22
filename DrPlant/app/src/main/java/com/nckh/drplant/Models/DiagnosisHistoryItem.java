package com.nckh.drplant.Models;

import java.io.Serializable;

public class DiagnosisHistoryItem implements Serializable {
    public String id;
    public long viewedAt;
    public String imagePath;
    public String message;
    public String diseaseName;
    public String predictionName;
    public String plantTypeRaw;
    public String plantTypeNormalized;
    public double confidence;
    public boolean healthy;
    public DiagnosisResponse diagnosisResponse;
}

