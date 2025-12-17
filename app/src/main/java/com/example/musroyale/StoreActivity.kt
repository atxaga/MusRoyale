// Kotlin
package com.example.musroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class StoreActivity : AppCompatActivity() {

    private val storeAdapter by lazy { StoreProductAdapter(StoreProductDiffCallback) { showPurchaseFeedback(it) } }
    private val catalog by lazy { createCatalog() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_store)

        val recyclerView = findViewById<RecyclerView>(R.id.rvStoreProducts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = storeAdapter
        storeAdapter.submitList(catalog)

        findViewById<EditText>(R.id.etStoreSearch)?.doAfterTextChanged { text ->
            val query = text?.toString()?.trim()?.lowercase().orEmpty()
            val filtered = if (query.isEmpty()) catalog else catalog.filter {
                it.name.lowercase().contains(query) || it.description.lowercase().contains(query)
            }
            storeAdapter.submitList(filtered)
        }
    }

    private fun showPurchaseFeedback(product: StoreProduct) {
        Toast.makeText(this, "Has comprado ${product.name}", Toast.LENGTH_SHORT).show()
    }

    private fun createCatalog(): List<StoreProduct> = listOf(
        StoreProduct("coins_basic", "Bolsa de monedas", "Suma 500 monedas para retar rivales", "€2.99", android.R.drawable.ic_menu_compass),
        StoreProduct("coins_premium", "Cofre maestro", "Incluye 2000 monedas y booster", "€9.99", android.R.drawable.ic_menu_gallery),
        StoreProduct("ticket_daily", "Ticket diario", "Duplica tus recompensas por 24 h", "€1.49", android.R.drawable.ic_menu_day),
        StoreProduct("skin_special", "Baraja dorada", "Aspecto exclusivo edición Maestro", "€4.49", android.R.drawable.ic_menu_crop),
        StoreProduct("bundle_full", "Pack Gran Maestro", "Monedas, booster y ticket semanal", "€14.99", android.R.drawable.ic_menu_agenda)
    )

    private inner class StoreProductAdapter(
        diffCallback: DiffUtil.ItemCallback<StoreProduct>,
        private val onBuyClick: (StoreProduct) -> Unit
    ) : ListAdapter<StoreProduct, StoreProductViewHolder>(diffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreProductViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_store_product, parent, false)
            return StoreProductViewHolder(view, onBuyClick)
        }

        override fun onBindViewHolder(holder: StoreProductViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private class StoreProductViewHolder(
        itemView: View,
        private val onBuyClick: (StoreProduct) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val image: ImageView = itemView.findViewById(R.id.ivProductImage)
        private val name: TextView = itemView.findViewById(R.id.tvProductName)
        private val description: TextView = itemView.findViewById(R.id.tvProductDescription)
        private val price: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val buyButton: MaterialButton = itemView.findViewById(R.id.btnBuy)

        fun bind(product: StoreProduct) {
            name.text = product.name
            description.text = product.description
            price.text = product.priceLabel
            image.setImageResource(product.imageRes)
            buyButton.setOnClickListener { onBuyClick(product) }
        }
    }

    private data class StoreProduct(
        val id: String,
        val name: String,
        val description: String,
        val priceLabel: String,
        @DrawableRes val imageRes: Int
    )

    private object StoreProductDiffCallback : DiffUtil.ItemCallback<StoreActivity.StoreProduct>() {
        override fun areItemsTheSame(oldItem: StoreActivity.StoreProduct, newItem: StoreActivity.StoreProduct) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: StoreActivity.StoreProduct, newItem: StoreActivity.StoreProduct) = oldItem == newItem
    }
}

