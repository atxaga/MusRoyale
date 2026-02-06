package com.example.musroyale

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musroyale.databinding.ItemChatMessageBinding
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import android.os.Environment
class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val currentMessage = messages[position]
        val previousMessage = if (position > 0) messages[position - 1] else null
        holder.bind(currentMessage, previousMessage)
    }

    inner class ChatViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage, previousMessage: ChatMessage?) {
            val dateText = getFormattedDateSeparator(message.timestamp, previousMessage?.timestamp)
            binding.textDateSeparator.visibility = if (dateText != null) View.VISIBLE else View.GONE
            binding.textDateSeparator.text = dateText
            message.imageUrl?.let { url ->
                binding.imgSent.setOnClickListener { mostrarImagenAmpliada(url, binding.root.context) }
                binding.imgReceived.setOnClickListener { mostrarImagenAmpliada(url, binding.root.context) }
            }
            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeString = sdfTime.format(Date(message.timestamp))

            if (message.isMine) {
                binding.layoutSent.visibility = View.VISIBLE
                binding.layoutReceived.visibility = View.GONE
                binding.timeSent.text = timeString

                if (!message.imageUrl.isNullOrBlank()) {
                    binding.textSent.visibility = View.GONE
                    binding.imgSent.visibility = View.VISIBLE
                    cargarImagenBase64(message.imageUrl, binding.imgSent)
                } else {
                    binding.imgSent.visibility = View.GONE
                    binding.textSent.visibility = View.VISIBLE
                    binding.textSent.text = message.message
                }
                binding.imgStatus.setImageResource(if (message.leido) R.drawable.leido else R.drawable.noleido)

            } else {
                binding.layoutReceived.visibility = View.VISIBLE
                binding.layoutSent.visibility = View.GONE
                binding.timeReceived.text = timeString

                if (!message.imageUrl.isNullOrBlank()) {
                    binding.textReceived.visibility = View.GONE
                    binding.imgReceived.visibility = View.VISIBLE
                    cargarImagenBase64(message.imageUrl, binding.imgReceived)
                } else {
                    binding.imgReceived.visibility = View.GONE
                    binding.textReceived.visibility = View.VISIBLE
                    binding.textReceived.text = message.message
                }
            }
        }
        private fun mostrarImagenAmpliada(base64String: String, context: android.content.Context) {
            val dialog = android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.setContentView(R.layout.dialog_ver_imagen) // Crearemos este layout ahora

            val imageView = dialog.findViewById<android.widget.ImageView>(R.id.imgFull)
            val btnDescargar = dialog.findViewById<android.widget.ImageButton>(R.id.btnDownload)
            val btnCerrar = dialog.findViewById<android.widget.ImageButton>(R.id.btnClose)

            val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.NO_WRAP)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            imageView.setImageBitmap(bitmap)

            btnCerrar.setOnClickListener { dialog.dismiss() }

            btnDescargar.setOnClickListener {
                guardarImagenEnGaleria(bitmap, context)
                android.widget.Toast.makeText(context, "Irudia galerian gordeta", android.widget.Toast.LENGTH_SHORT).show()
            }

            dialog.show()
        }
        private fun guardarImagenEnGaleria(bitmap: android.graphics.Bitmap, context: android.content.Context) {
            val filename = "MusRoyale_${System.currentTimeMillis()}.jpg"
            val outputStream: OutputStream?

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    outputStream = imageUri?.let { resolver.openOutputStream(it) }
                } else {
                    val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val image = File(imagesDir, filename)
                    outputStream = FileOutputStream(image)
                }

                outputStream?.use {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, it)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatAdapter", "Error al guardar: ${e.message}")
            }
        }
        private fun cargarImagenBase64(base64String: String, imageView: android.widget.ImageView) {
            try {
                val cleanBase64 = base64String.replace("\\s".toRegex(), "")
                val imageBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.NO_WRAP)

                Glide.with(imageView.context)
                    .asBitmap()
                    .load(imageBytes)
                    .override(800, 800)
                    .fitCenter()
                    .into(imageView)
            } catch (e: Exception) {
                android.util.Log.e("ChatAdapter", "Error: ${e.message}")
            }
        }
    }

    private fun getFormattedDateSeparator(currentTs: Long, prevTs: Long?): String? {
        if (prevTs == null || !isSameDay(currentTs, prevTs)) {
            val now = Calendar.getInstance()
            return when {
                isSameDay(currentTs, now.timeInMillis) -> "Hoy"
                isSameDay(currentTs, now.timeInMillis - 86400000) -> "Ayer"
                else -> SimpleDateFormat("d 'de' MMMM", Locale("es", "ES")).format(Date(currentTs))
            }
        }
        return null
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}