package st.pushupcounter.view.fragment.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import st.pushupcounter.R;
import st.pushupcounter.databinding.DialogDonateBinding;

public class DonateDialog extends DialogFragment {

    public static final String TAG = DonateDialog.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        st.pushupcounter.databinding.DialogDonateBinding binding = DialogDonateBinding.inflate(getLayoutInflater());
        View dialogView = binding.getRoot();

        binding.donateCardTv.setOnClickListener(this::copyTextToClipboard);
        binding.donatePhoneTv.setOnClickListener(this::copyTextToClipboard);
        binding.donateEmailTv.setOnClickListener(this::copyTextToClipboard);

        // Создаю диалоговое окно
        return new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .setTitle(R.string.menu_donate)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
    private void copyTextToClipboard(View view) {
        ClipboardManager clipboard = (ClipboardManager)requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("",((TextView)view).getText().toString());
        clipboard.setPrimaryClip(clip);
        Toast toast = Toast.makeText(requireActivity().getApplicationContext(),R.string.text_copied, Toast.LENGTH_SHORT);
        toast.show();
    }
}
