package com.nckh.drplant;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.nckh.drplant.Models.DiagnosisHistoryItem;
import com.nckh.drplant.Utils.DiagnosisHelper;
import com.nckh.drplant.Utils.DiagnosisHistoryManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private final List<DiagnosisHistoryItem> allHistoryItems = new ArrayList<>();
    private final List<DiagnosisHistoryItem> visibleHistoryItems = new ArrayList<>();

    private RadioGroup filterGroup;
    private ListView historyListView;
    private TextView emptyHistoryText;
    private HistoryAdapter historyAdapter;

    // Khởi tạo màn hình lịch sử, ánh xạ view và gán sự kiện cho bộ lọc cùng danh sách.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        filterGroup = findViewById(R.id.filterGroup);
        historyListView = findViewById(R.id.historyListView);
        emptyHistoryText = findViewById(R.id.emptyHistoryText);
        historyListView.setEmptyView(emptyHistoryText);

        historyAdapter = new HistoryAdapter();
        historyListView.setAdapter(historyAdapter);
        historyListView.setOnItemClickListener((parent, view, position, id) -> openHistoryItem(position));

        filterGroup.setOnCheckedChangeListener((group, checkedId) -> applySelectedFilter());
    }

    // Tải lại dữ liệu lịch sử mỗi khi quay lại màn hình để đảm bảo danh sách luôn mới nhất.
    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    // Lấy toàn bộ lịch sử chẩn đoán đã lưu và áp dụng bộ lọc hiện tại.
    private void loadHistory() {
        allHistoryItems.clear();
        allHistoryItems.addAll(DiagnosisHistoryManager.getHistoryItems(this));
        applySelectedFilter();
    }

    // Lọc danh sách theo lựa chọn hiện tại: tất cả, sầu riêng hoặc cà phê.
    private void applySelectedFilter() {
        String filter = DiagnosisHistoryManager.FILTER_ALL;
        int checkedId = filterGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.filterDurianButton) {
            filter = DiagnosisHistoryManager.FILTER_DURIAN;
            emptyHistoryText.setText(R.string.history_empty_durian);
        } else if (checkedId == R.id.filterCoffeeButton) {
            filter = DiagnosisHistoryManager.FILTER_COFFEE;
            emptyHistoryText.setText(R.string.history_empty_coffee);
        } else {
            emptyHistoryText.setText(R.string.history_empty_all);
        }

        visibleHistoryItems.clear();
        visibleHistoryItems.addAll(DiagnosisHistoryManager.filterHistory(allHistoryItems, filter));
        historyAdapter.notifyDataSetChanged();
    }

    // Mở lại màn hình chi tiết chẩn đoán khi người dùng chọn một phần tử trong lịch sử.
    private void openHistoryItem(int position) {
        if (position < 0 || position >= visibleHistoryItems.size()) {
            return;
        }

        DiagnosisHistoryItem historyItem = visibleHistoryItems.get(position);
        if (historyItem == null || historyItem.diagnosisResponse == null) {
            return;
        }

        DiagnosisHelper.launchDiagnosisActivity(
                this,
                historyItem.diagnosisResponse,
                historyItem.imagePath,
                false
        );
    }

    // Adapter dùng để hiển thị từng mục lịch sử chẩn đoán trong ListView.
    private class HistoryAdapter extends BaseAdapter {

        private final LayoutInflater layoutInflater = LayoutInflater.from(HistoryActivity.this);
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        // Trả về số lượng phần tử lịch sử đang được hiển thị sau khi lọc.
        @Override
        public int getCount() {
            return visibleHistoryItems.size();
        }

        // Trả về phần tử lịch sử tương ứng với vị trí trong danh sách.
        @Override
        public Object getItem(int position) {
            return visibleHistoryItems.get(position);
        }

        // Trả về id tạm thời theo vị trí phần tử trong danh sách.
        @Override
        public long getItemId(int position) {
            return position;
        }

        // Tạo hoặc tái sử dụng view item để hiển thị dữ liệu của một chẩn đoán trong lịch sử.
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.item_diagnosis_history, parent, false);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            DiagnosisHistoryItem historyItem = visibleHistoryItems.get(position);
            bindViewHolder(viewHolder, historyItem);
            return convertView;
        }

        // Gán dữ liệu từ một mục lịch sử vào các thành phần hiển thị trên item.
        private void bindViewHolder(@NonNull ViewHolder viewHolder, @NonNull DiagnosisHistoryItem historyItem) {
            String displayName = !TextUtils.isEmpty(historyItem.diseaseName)
                    ? historyItem.diseaseName
                    : formatDisplayName(historyItem.predictionName);
            viewHolder.titleText.setText(displayName);

            String plantType = formatPlantType(historyItem.plantTypeNormalized, historyItem.plantTypeRaw);
            String status = getString(historyItem.healthy ? R.string.diagnosis_status_healthy : R.string.diagnosis_status_unhealthy);
            viewHolder.subtitleText.setText(getString(R.string.history_item_subtitle_format, plantType, status));

            viewHolder.confidenceText.setText(
                    String.format(
                            Locale.getDefault(),
                            getString(R.string.history_item_confidence_format),
                            historyItem.confidence
                    )
            );
            viewHolder.timeText.setText(dateFormat.format(new Date(historyItem.viewedAt)));
        }
    }

    // ViewHolder giúp giữ tham chiếu các view con để tái sử dụng item hiệu quả hơn.
    private static class ViewHolder {
        final TextView titleText;
        final TextView subtitleText;
        final TextView confidenceText;
        final TextView timeText;

        // Ánh xạ các thành phần hiển thị bên trong một item lịch sử.
        ViewHolder(@NonNull View rootView) {
            titleText = rootView.findViewById(R.id.titleText);
            subtitleText = rootView.findViewById(R.id.subtitleText);
            confidenceText = rootView.findViewById(R.id.confidenceText);
            timeText = rootView.findViewById(R.id.timeText);
        }
    }

    // Chuyển plant type đã chuẩn hóa thành nhãn hiển thị thân thiện cho người dùng.
    private String formatPlantType(String normalizedType, String rawType) {
        if (DiagnosisHistoryManager.FILTER_DURIAN.equals(normalizedType)) {
            return getString(R.string.history_filter_durian);
        }
        if (DiagnosisHistoryManager.FILTER_COFFEE.equals(normalizedType)) {
            return getString(R.string.history_filter_coffee);
        }
        if (!TextUtils.isEmpty(rawType)) {
            return formatDisplayName(rawType);
        }
        return getString(R.string.diagnosis_not_available);
    }

    // Chuẩn hóa chuỗi hiển thị bằng cách thay dấu gạch dưới thành khoảng trắng.
    private String formatDisplayName(String rawValue) {
        if (TextUtils.isEmpty(rawValue)) {
            return getString(R.string.diagnosis_not_available);
        }
        return rawValue.replace("_", " ");
    }
}

