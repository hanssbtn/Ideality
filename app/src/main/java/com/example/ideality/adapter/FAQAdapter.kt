package com.example.ideality.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ideality.R
import com.example.ideality.databinding.ItemFaqBinding
import com.example.ideality.databinding.ItemFaqSectionBinding
import com.example.ideality.models.FAQItem
import com.example.ideality.models.FAQSection

class FAQAdapter(
    private val onItemClick: (FAQSection, FAQItem, Boolean) -> Unit
) : ListAdapter<FAQSection, FAQAdapter.ViewHolder>(FAQDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemFaqSectionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val section = getItem(position)
        holder.bind(section)
    }

    inner class ViewHolder(private val binding: ItemFaqSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(section: FAQSection) {
            binding.apply {
                sectionTitle.text = section.title
                faqContainer.removeAllViews()

                // Only add items that match the search criteria
                section.items.forEach { item ->
                    val itemBinding = ItemFaqBinding.inflate(
                        LayoutInflater.from(itemView.context),
                        faqContainer,
                        true
                    )

                    itemBinding.apply {
                        question.text = item.question
                        answer.text = item.answer
                        answer.isVisible = item.isExpanded

                        root.setOnClickListener {
                            onItemClick(section, item, item.isExpanded)
                        }

                        expandIcon.rotation = if (item.isExpanded) 180f else 0f
                    }
                }
            }
        }
    }

        private fun animateExpansion(itemView: View, isExpanding: Boolean) {
            val answerView = itemView.findViewById<TextView>(R.id.answer)
            val arrowIcon = itemView.findViewById<ImageView>(R.id.expandIcon)

            if (isExpanding) {
                // Expand animation
                answerView.visibility = View.VISIBLE
                answerView.startAnimation(
                    AnimationUtils.loadAnimation(itemView.context, R.anim.slide_down)
                )
                arrowIcon.animate()
                    .rotation(180f)
                    .setDuration(300)
                    .start()
            } else {
                // Collapse animation
                val slideUp = AnimationUtils.loadAnimation(itemView.context, R.anim.slide_up)
                slideUp.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationRepeat(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        answerView.visibility = View.GONE
                    }
                })
                answerView.startAnimation(slideUp)
                arrowIcon.animate()
                    .rotation(0f)
                    .setDuration(300)
                    .start()
            }
        }
    }


class FAQDiffCallback : DiffUtil.ItemCallback<FAQSection>() {
    override fun areItemsTheSame(oldItem: FAQSection, newItem: FAQSection) =
        oldItem.title == newItem.title

    override fun areContentsTheSame(oldItem: FAQSection, newItem: FAQSection) =
        oldItem == newItem
}