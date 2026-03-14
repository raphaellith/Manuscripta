package com.manuscripta.student.ui.quiz;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * ItemDecoration that adds vertical spacing between quiz option items.
 */
public class OptionSpacingDecoration extends RecyclerView.ItemDecoration {

    /** The spacing in pixels to add between items. */
    private final int spacing;

    /**
     * Creates a new OptionSpacingDecoration.
     *
     * @param spacing The vertical spacing in pixels between items.
     */
    public OptionSpacingDecoration(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect,
                               @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position > 0) {
            outRect.top = spacing;
        }
    }
}
