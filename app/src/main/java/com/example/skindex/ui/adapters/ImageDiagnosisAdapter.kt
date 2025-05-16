package com.example.skindex.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.skindex.R
import com.example.skindex.viewmodel.PatientDetailViewModel
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.example.skindex.core.constants.AppConstants
import com.example.skindex.data.api.model.DiagnosisResponse

class ImageDiagnosisAdapter(private val items: List<PatientDetailViewModel.ImageWithDiagnosis>,
                            private val onDiagnosisClick: (DiagnosisResponse) -> Unit)
    : RecyclerView.Adapter<ImageDiagnosisAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val diagnosisTextView: TextView = itemView.findViewById(R.id.textViewDiagnosis)
        val confidenceTextView: TextView = itemView.findViewById(R.id.textViewConfidence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_diagnosis, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val imageUrl = "${AppConstants.BASE_URL}/${item.image.filePath}"
        Log.d("ImageDiagnosisAdapter", "Loading image from URL: $imageUrl")
        Picasso.get()
            .load(imageUrl)
            .error(R.drawable.ic_error_placeholder)
            .into(holder.imageView, object : Callback {
                override fun onSuccess() {
                    Log.d("ImageDiagnosisAdapter", "Image loaded successfully: $imageUrl")
                }

                override fun onError(e: Exception) {
                    Log.e("ImageDiagnosisAdapter", "Error loading image: $imageUrl, Error: ${e.message}")
                }
            })
        if (item.diagnosis != null) {
            holder.diagnosisTextView.text = "Diagnosis: ${item.diagnosis.diseaseName}"
            holder.confidenceTextView.text = "Probability: ${String.format("%.2f%%", item.diagnosis.confidence * 100)}"

            holder.itemView.setOnClickListener {
                onDiagnosisClick(item.diagnosis)
            }
        } else {
            holder.diagnosisTextView.text = "Diagnosis: —"
            holder.confidenceTextView.text = "Probability: —"
        }
    }

    override fun getItemCount(): Int = items.size
}