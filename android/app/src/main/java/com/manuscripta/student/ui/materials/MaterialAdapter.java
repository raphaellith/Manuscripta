package com.manuscripta.student.ui.materials;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.manuscripta.student.R;
import com.manuscripta.student.domain.model.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying a list of materials.
 */
public class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.MaterialViewHolder> {

    private final List<Material> materials;
    private OnMaterialClickListener listener;

    /**
     * Interface for material click events.
     */
    public interface OnMaterialClickListener {
        /**
         * Called when a material is clicked.
         *
         * @param material The clicked material
         */
        void onMaterialClick(Material material);
    }

    /**
     * Creates a new MaterialAdapter.
     */
    public MaterialAdapter() {
        this.materials = new ArrayList<>();
    }

    /**
     * Sets the click listener.
     *
     * @param listener The listener
     */
    public void setOnMaterialClickListener(OnMaterialClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the materials list.
     *
     * @param newMaterials The new list of materials
     */
    public void setMaterials(@NonNull List<Material> newMaterials) {
        this.materials.clear();
        this.materials.addAll(newMaterials);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MaterialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new MaterialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MaterialViewHolder holder, int position) {
        Material material = materials.get(position);
        holder.bind(material, listener);
    }

    @Override
    public int getItemCount() {
        return materials.size();
    }

    /**
     * ViewHolder for material items.
     */
    static class MaterialViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView typeText;

        MaterialViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(android.R.id.text1);
            typeText = itemView.findViewById(android.R.id.text2);
        }

        void bind(@NonNull Material material, OnMaterialClickListener listener) {
            titleText.setText(material.getTitle());
            typeText.setText(material.getType().toString());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMaterialClick(material);
                }
            });
        }
    }
}
