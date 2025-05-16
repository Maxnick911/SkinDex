package com.example.skindex.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.skindex.databinding.ItemPatientBinding
import com.example.skindex.data.api.model.Patient

class PatientsAdapter(private val onPatientClick: (Patient) -> Unit)
    : ListAdapter<Patient, PatientsAdapter.PatientViewHolder>(PatientDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PatientViewHolder(ItemPatientBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val p = getItem(position)
        holder.bind(p)
        holder.itemView.setOnClickListener { onPatientClick(p) }
    }

    inner class PatientViewHolder(val binding: ItemPatientBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(p: Patient) {
            binding.patientNameTextView.text = p.name
            binding.patientEmailTextView.text = p.email
        }
    }

    class PatientDiffCallback : DiffUtil.ItemCallback<Patient>() {
        override fun areItemsTheSame(oldItem: Patient, newItem: Patient): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Patient, newItem: Patient): Boolean {
            return oldItem == newItem
        }
    }
}