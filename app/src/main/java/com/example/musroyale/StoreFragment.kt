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
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class StoreFragment : Fragment() {

    // Inicializamos el adapter y el catálogo igual que antes
    private val storeAdapter by lazy { StoreProductAdapter(StoreProductDiffCallback) { showPurchaseFeedback(it) } }
    private val catalog by lazy { createCatalog() }

    // 1. Inflamos la vista (Layout)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Asegúrate de que el XML se llame 'fragment_store' (o como se llamara en tu activity)
        return inflater.inflate(R.layout.fragment_store, container, false)
    }

    // 2. Aquí va la lógica que antes tenías en onCreate
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvStoreProducts)

        // CAMBIO CLAVE: Usamos GridLayoutManager con 2 columnas
        val gridLayoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
        recyclerView.layoutManager = gridLayoutManager

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = storeAdapter
        storeAdapter.submitList(catalog)

        // Configuración de la búsqueda
        view.findViewById<EditText>(R.id.etStoreSearch)?.doAfterTextChanged { text ->
            val query = text?.toString()?.trim()?.lowercase().orEmpty()
            val filtered = if (query.isEmpty()) catalog else catalog.filter {
                it.name.lowercase().contains(query) || it.description.lowercase().contains(query)
            }
            storeAdapter.submitList(filtered)
        }
    }

    private fun showPurchaseFeedback(product: StoreProduct) {
        // Usamos requireContext() para el Toast
        Toast.makeText(requireContext(), "Has comprado ${product.name}", Toast.LENGTH_SHORT).show()
    }

    // --- DATOS Y ADAPTERS (Se mantienen casi igual) ---

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
        // Si quitaste la descripción del XML, comenta la siguiente línea:
        // private val description: TextView = itemView.findViewById(R.id.tvProductDescription)
        private val price: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val buyButton: MaterialButton = itemView.findViewById(R.id.btnBuy)

        fun bind(product: StoreProduct) {
            name.text = product.name
            // description.text = product.description // Solo si el ID existe en el XML
            price.text = product.priceLabel
            image.setImageResource(product.imageRes)
            buyButton.setOnClickListener { onBuyClick(product) }
        }
    }

    // He quitado 'private' para evitar problemas de visibilidad con el DiffCallback si fuera necesario,
    // pero dentro del archivo está bien.
    data class StoreProduct(
        val id: String,
        val name: String,
        val description: String,
        val priceLabel: String,
        @DrawableRes val imageRes: Int
    )

    private object StoreProductDiffCallback : DiffUtil.ItemCallback<StoreProduct>() {
        override fun areItemsTheSame(oldItem: StoreProduct, newItem: StoreProduct) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: StoreProduct, newItem: StoreProduct) = oldItem == newItem
    }
}