package com.example.bitesight.ui.mealhistory

import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.RecyclerView
import com.example.bitesight.R
import com.example.bitesight.data.local.entity.Meal
import java.io.File

class MealAdapter(
    var meals: List<Meal>, // Public access for ItemTouchHelper/Deletion
    private val listener: MealActionListener
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
        holder.mealCalories.text = "${meal.calories.toInt()} kcal"

        // Image Loading Logic (File or Drawable, with Rotation)
        meal.imagePath?.let { path ->
            val file = File(path)

            if (file.exists()) {
                var bitmap = BitmapFactory.decodeFile(path)
                bitmap = rotateBitmap(bitmap, path)
                holder.mealImage.setImageBitmap(bitmap)
            } else {
                val context = holder.itemView.context
                val resId = context.resources.getIdentifier(path, "drawable", context.packageName)
                if (resId != 0) {
                    holder.mealImage.setImageResource(resId)
                } else {
                    holder.mealImage.setImageResource(R.drawable.placeholder_food)
                }
            }
        } ?: run {
            holder.mealImage.setImageResource(R.drawable.placeholder_food)
        }

        // Log Meal Click Listener
        holder.btnLogMeal.setOnClickListener {
            listener.onLogMealClicked(meal)
        }

        holder.btnViewStats.setOnClickListener {
            listener.onViewStatsClicked(meal)
        }
    }

    override fun getItemCount(): Int = meals.size

    fun updateMeals(newMeals: List<Meal>) {
        meals = newMeals
        notifyDataSetChanged()
    }

    /**
     * Rotates the bitmap based on the orientation stored in the file's EXIF data.
     */
    private fun rotateBitmap(bitmap: android.graphics.Bitmap, imagePath: String): android.graphics.Bitmap {
        return try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }

            android.graphics.Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }
}