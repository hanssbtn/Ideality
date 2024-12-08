package com.example.ideality.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ideality.databinding.ItemFaqBinding
import com.example.ideality.databinding.ItemFaqSectionBinding
import com.example.ideality.models.FAQItem
import com.example.ideality.models.FAQSection

class FAQAdapter(
    private val onItemClick: (FAQSection, FAQItem, Boolean) -> Unit
) : ListAdapter<FAQSection, FAQAdapter.ViewHolder>(FAQDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFaqSectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemFaqSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(section: FAQSection) {
            binding.apply {
                sectionTitle.text = section.title
                faqContainer.removeAllViews()

                section.items.forEach { item ->
                    val itemView = ItemFaqBinding.inflate(
                        LayoutInflater.from(root.context),
                        faqContainer,
                        false
                    )

                    itemView.apply {
                        question.text = item.question
                        answer.text = item.answer
                        answer.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
                        expandIcon.rotation = if (item.isExpanded) 180f else 0f

                        root.setOnClickListener {
                            // First notify the activity
                            onItemClick(section, item, item.isExpanded)

                            // Then handle the local view changes
                            val shouldExpand = !item.isExpanded
                            answer.visibility = if (shouldExpand) View.VISIBLE else View.GONE

                            // Animate the arrow
                            expandIcon.animate()
                                .rotation(if (shouldExpand) 180f else 0f)
                                .setDuration(200)
                                .start()

                            // Animate the answer
                            if (shouldExpand) {
                                answer.alpha = 0f
                                answer.animate()
                                    .alpha(1f)
                                    .setDuration(200)
                                    .start()
                            }
                        }
                    }

                    faqContainer.addView(itemView.root)
                }
            }
        }
    }

    class FAQDiffCallback : DiffUtil.ItemCallback<FAQSection>() {
        override fun areItemsTheSame(oldItem: FAQSection, newItem: FAQSection): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: FAQSection, newItem: FAQSection): Boolean {
            return oldItem == newItem
        }
    }
}