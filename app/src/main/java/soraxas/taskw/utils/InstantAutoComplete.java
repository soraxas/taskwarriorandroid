package soraxas.taskw.utils;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


public class InstantAutoComplete extends android.support.v7.widget.AppCompatAutoCompleteTextView {
    public InstantAutoComplete(Context context) {
        super(context);
    }

    public InstantAutoComplete(Context arg0, AttributeSet arg1) {
        super(arg0, arg1);
    }

    public InstantAutoComplete(Context arg0, AttributeSet arg1, int arg2) {
        super(arg0, arg1, arg2);
    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        if (getWindowVisibility() == View.GONE) {
            Log.d("InstantAutoComplete", "Window not visible, will not show drop down");
            return;
        }
        if (focused && getAdapter() != null) {
            performFiltering(getText(), 0);
            showDropDown();
        }
    }
}