package st.pushupcounter.view.adapter;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Locale;

import st.pushupcounter.R;
import st.pushupcounter.data.model.IntegerCounter;

public class CountersListAdapter extends ArrayAdapter<IntegerCounter> {

    private static final String TAG = CountersListAdapter.class.getSimpleName();

    public CountersListAdapter(Context context) {
        super(context, 0);
    }

    @SuppressLint("InflateParams")
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.menu_row, null);
        }
        final TextView titleView = convertView.findViewById(R.id.row_title);
        final TextView countView = convertView.findViewById(R.id.row_count);
        final TextView stopwatchView = convertView.findViewById(R.id.stopwatch_value);

        final IntegerCounter counter = getItem(position);
        if (counter != null) {
            titleView.setText(counter.getName());
            countView.setText(String.format(Locale.getDefault(), "%d", counter.getValue()));
            stopwatchView.setText(counter.getTimerValue());
        } else {
            Log.v(TAG, String.format("Failed to get item in position %s", position));
            titleView.setText("???");
            countView.setText("???");
            stopwatchView.setText("???");
        }

        return convertView;
    }
}
