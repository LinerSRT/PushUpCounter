package st.pushupcounter.view.fragment.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import java.util.Objects;
import st.pushupcounter.CounterApplication;
import st.pushupcounter.R;
import st.pushupcounter.domain.repository.CounterRepository;
import st.pushupcounter.view.activity.MainActivity;
import st.pushupcounter.data.model.IntegerCounter;
import st.pushupcounter.data.exception.CounterException;
import st.pushupcounter.data.util.BroadcastHelper;
import st.pushupcounter.view.fragment.CounterFragment;

public class EditDialog extends DialogFragment {

  public static final String TAG = EditDialog.class.getSimpleName();
  private static final String BUNDLE_ARGUMENT_NAME = "name";
  private static final String BUNDLE_ARGUMENT_VALUE = "value";
  private static final String BUNDLE_ARGUMENT_TIME = "time";

  public static EditDialog newInstance(final @NonNull String counterName, int counterValue, final String stopwatchValue) {
    final EditDialog dialog = new EditDialog();

    final Bundle arguments = new Bundle();
    arguments.putString(BUNDLE_ARGUMENT_NAME, counterName);
    arguments.putInt(BUNDLE_ARGUMENT_VALUE, counterValue);
    arguments.putString(BUNDLE_ARGUMENT_TIME, stopwatchValue);
    dialog.setArguments(arguments);

    return dialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    final String oldName = requireArguments().getString(BUNDLE_ARGUMENT_NAME);
    final int oldValue = requireArguments().getInt(BUNDLE_ARGUMENT_VALUE);
    String oldTime = requireArguments().getString(BUNDLE_ARGUMENT_TIME);

    final MainActivity activity = (MainActivity) getActivity();

    final View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit, null);
    final EditText nameInput = dialogView.findViewById(R.id.edit_name);
    nameInput.setText(oldName);
    final EditText valueInput = dialogView.findViewById(R.id.edit_value);
    valueInput.setText(String.valueOf(oldValue));
    //valueInput.setInputType(EditorInfo.TYPE_CLASS_NUMBER); // ???????????????????? ???????? ???????????? ???????????????????????? ??????????????
    final EditText timeInput = dialogView.findViewById(R.id.edit_stopwatch);
    // ???????????????????? ???????????? ???? ?????????????? "00:00.00" ?? "00,00,00" ?????? ???????????????????? ???????????????? ?? ??????????????????????
    oldTime = oldTime.replace(":", ",");
    oldTime = oldTime.replace(".", ",");
    // ?????????????????????????????? ???????????? ?????????????? ?? ???????? ??????????
    timeInput.setText(oldTime);

    final InputFilter[] valueFilter = new InputFilter[1];
    valueFilter[0] = new InputFilter.LengthFilter(IntegerCounter.getValueCharLimit());
    valueInput.setFilters(valueFilter);

    final AlertDialog dialog =
        new AlertDialog.Builder(getActivity())
            .setView(dialogView)
            .setTitle(getString(R.string.dialog_edit_title))
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(getResources().getText(R.string.dialog_button_cancel), null)
            .create();

    // ???? ?????????????????? ?????????? ?????????????? ???? ?????????? ???????????? ???????????????????? ???????? ??????????????????????. ?????????? ?????????? ???? ??????????????????????, ????????
    // ???????? ???????????? ?????? ????????????????????, ?? ?????????????????????????? ?????????????????? ????????????. ?????? ???????? ???? ????????????????????????.
    dialog.setOnShowListener(dialogInterface -> {

      Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
      button.setOnClickListener(view -> {
        final String newName = nameInput.getText().toString();
        if (newName.isEmpty()) {
          Toast.makeText(
                  activity,
                  getResources().getText(R.string.toast_no_name_message),
                  Toast.LENGTH_SHORT)
                  .show();
          return;
        } else {
          int newValue;
          String valueInputContents = valueInput.getText().toString();
          if (!valueInputContents.equals("")) {
            try {
              newValue = Integer.parseInt(valueInputContents);
            } catch (NumberFormatException e) {
              Log.w(TAG, "Unable to parse new value", e);
              Toast.makeText(
                      activity,
                      getResources().getText(R.string.toast_unable_to_modify),
                      Toast.LENGTH_SHORT)
                      .show();
              newValue = CounterFragment.DEFAULT_VALUE;
              return;
            }
          } else {
            newValue = CounterFragment.DEFAULT_VALUE;
          }

          long newTime = 0L;
          final String timeInputContents = timeInput.getText().toString().trim();
          if (!timeInputContents.isEmpty()) {
            try {
              newTime = IntegerCounter.isStopwatchValueCorrect(timeInputContents);
            } catch (NumberFormatException e) {
              Log.w(TAG, "Unable to parse time", e);
              Toast.makeText(
                      activity,
                      getResources().getText(R.string.toast_unable_to_parse_time),
                      Toast.LENGTH_SHORT)
                      .show();
              return;
            }
          } else {
            newTime = CounterFragment.DEFAULT_TIME;
          }
          if (newTime < 0) {
            Log.w(TAG, "Unable to parse time");
            Toast.makeText(
                    activity,
                    getResources().getText(R.string.toast_unable_to_parse_time),
                    Toast.LENGTH_SHORT)
                    .show();
            return;
          }

          final CounterRepository<IntegerCounter> storage =
                  CounterApplication.getComponent().localStorage();

          storage.delete(oldName);
          try {
            storage.write(new IntegerCounter(newName, newValue, newTime));
          } catch (CounterException e) {
            Log.getStackTraceString(e);
            Toast.makeText(
                    getContext(), R.string.toast_unable_to_modify, Toast.LENGTH_SHORT)
                    .show();
          }

          new BroadcastHelper(requireContext()).sendSelectCounterBroadcast(newName);
        }

        //Dismiss once everything is OK.
        dialog.dismiss();
      });
    });

    dialog.setCanceledOnTouchOutside(true);
    Objects.requireNonNull(dialog.getWindow())
        .setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

    return dialog;
  }
}
