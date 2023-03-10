package st.pushupcounter.view.fragment.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import java.util.Objects;
import st.pushupcounter.CounterApplication;
import st.pushupcounter.R;
import st.pushupcounter.data.model.IntegerCounter;
import st.pushupcounter.domain.repository.CounterRepository;
import st.pushupcounter.data.util.BroadcastHelper;

public class DeleteDialog extends DialogFragment {

  public static final String TAG = "DeleteDialog";
  private static final String BUNDLE_ARGUMENT_NAME = "selected_position";

  public static DeleteDialog newInstance(final @NonNull String counterName) {
    final DeleteDialog dialog = new DeleteDialog();

    final Bundle arguments = new Bundle();
    arguments.putString(BUNDLE_ARGUMENT_NAME, counterName);
    dialog.setArguments(arguments);

    return dialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    final String name = requireArguments().getString(BUNDLE_ARGUMENT_NAME);

    final Dialog deleteDialog =
        new AlertDialog.Builder(getActivity())
            .setMessage(getResources().getText(R.string.dialog_delete_title))
            .setCancelable(false)
            .setPositiveButton(
                getResources().getText(R.string.dialog_button_delete),
                (dialog, id) -> {
                  final CounterRepository<IntegerCounter> storage =
                      CounterApplication.getComponent().localStorage();
                  storage.delete(name);

                  Toast.makeText(
                          getContext(),
                          String.format(
                              (String) getResources().getText(R.string.toast_delete_success), name),
                          Toast.LENGTH_SHORT)
                      .show();

                  // Switch to a different counter
                  new BroadcastHelper(requireContext())
                      .sendSelectCounterBroadcast(storage.getFirst().getName());
                })
            .setNegativeButton(getResources().getText(R.string.dialog_button_cancel), null)
            .create();
    Objects.requireNonNull(deleteDialog.getWindow())
        .setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

    return deleteDialog;
  }
}
