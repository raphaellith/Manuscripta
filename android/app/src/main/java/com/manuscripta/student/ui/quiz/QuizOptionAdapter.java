package com.manuscripta.student.ui.quiz;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.manuscripta.student.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying quiz answer options.
 * Supports single-selection highlighting matching the prototype's behaviour.
 */
public class QuizOptionAdapter
        extends RecyclerView.Adapter<QuizOptionAdapter.OptionViewHolder> {

    /** The list of option text values. */
    private final List<String> options = new ArrayList<>();

    /** The currently selected option index, or -1 if none selected. */
    private int selectedIndex = -1;

    /** Listener invoked when an option is tapped. */
    private OnOptionSelectedListener listener;

    /**
     * Callback interface for option selection events.
     */
    public interface OnOptionSelectedListener {
        /**
         * Called when an option is selected.
         *
         * @param index The index of the selected option.
         */
        void onOptionSelected(int index);
    }

    /**
     * Sets the listener for option selection events.
     *
     * @param listener The listener to set.
     */
    public void setOnOptionSelectedListener(@NonNull OnOptionSelectedListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the current options with the given list and resets selection.
     *
     * @param newOptions The new list of option strings.
     */
    public void setOptions(@NonNull List<String> newOptions) {
        options.clear();
        options.addAll(newOptions);
        selectedIndex = -1;
        notifyDataSetChanged();
    }

    /**
     * Sets the selected option index and updates the display.
     *
     * @param index The index to select, or -1 to clear selection.
     */
    public void setSelectedIndex(int index) {
        int previous = selectedIndex;
        selectedIndex = index;
        if (previous >= 0 && previous < options.size()) {
            notifyItemChanged(previous);
        }
        if (index >= 0 && index < options.size()) {
            notifyItemChanged(index);
        }
    }

    /**
     * Returns the currently selected option index.
     *
     * @return The selected index, or -1 if none selected.
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quiz_option, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        String label = String.valueOf((char) ('A' + position));
        String text = label + ") " + options.get(position);
        holder.getTextOption().setText(text);

        if (position == selectedIndex) {
            holder.getTextOption().setBackgroundResource(R.drawable.bg_option_selected);
        } else {
            holder.getTextOption().setBackgroundResource(R.drawable.bg_option_default);
        }

        holder.itemView.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION) {
                setSelectedIndex(adapterPos);
                if (listener != null) {
                    listener.onOptionSelected(adapterPos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    /**
     * ViewHolder for a single quiz option item.
     */
    static class OptionViewHolder extends RecyclerView.ViewHolder {

        /** The text view displaying the option label and text. */
        private final TextView textOption;

        /**
         * Creates a new OptionViewHolder.
         *
         * @param itemView The root view of the option item layout.
         */
        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            textOption = itemView.findViewById(R.id.textOption);
        }

        /**
         * Returns the text view for the option.
         *
         * @return The option TextView.
         */
        @NonNull
        TextView getTextOption() {
            return textOption;
        }
    }
}
