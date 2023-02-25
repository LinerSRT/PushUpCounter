package st.pushupcounter.view.component;

import android.content.Context;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;

public class UnderlineTextView extends androidx.appcompat.widget.AppCompatTextView {
    public UnderlineTextView(Context context) {
        this(context, null);
    }

    public UnderlineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        SpannableString content = new SpannableString(text);
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        super.setText(content, type);
    }
}