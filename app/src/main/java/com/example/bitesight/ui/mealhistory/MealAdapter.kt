package com.example.bitesight.ui.mealhistory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hackathon.R
import com.example.bitesight.data.local.entity.Meal

class MealAdapter(
    private var meals: List<Meal>
) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    class MealViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mealImage: ImageView = view.findViewById(R.id.mealImage)
        val mealName: TextView = view.findViewById(R.id.mealName)
        val mealCalories: TextView = view.findViewById(R.id.mealCalories)
        val btnLogMeal: Button = view.findViewById(R.id.btnLogMeal)
        val btnViewStats: Button = view.findViewById(R.id.btnViewStats)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.meal_item, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = meals[position]

        holder.mealName.text = meal.foodName
        holder.mealCalories.text = "${meal.calories} kcal"

        meal.imagePath?.let { imageName ->
            val resId = holder.itemView.context.resources.getIdentifier(
                imageName, "drawable", holder.itemView.context.packageName
            )
            if (resId != 0) holder.mealImage.setImageResource(resId)
        }
    }

    override fun getItemCount(): Int = meals.size

    fun updateMeals(newMeals: List<Meal>) {
        meals = newMeals
        notifyDataSetChanged()
    }
}
