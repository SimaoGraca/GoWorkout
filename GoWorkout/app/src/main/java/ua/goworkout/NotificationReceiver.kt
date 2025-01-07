package ua.goworkout

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationReceiver : BroadcastReceiver() {

    // Definir a URL base onde as imagens estão hospedadas
    private val baseUrl = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/uploads/imagem_categoria/"

    override fun onReceive(context: Context, intent: Intent) {
        // Obter os dados da aula da intent
        val nomeAula = intent.getStringExtra("nome_aula")
        val horarioAula = intent.getStringExtra("horario_aula")
        val imagemCategoria = intent.getStringExtra("imagem_categoria") // Nome da imagem ou caminho

        // Formatar o horário para exibir apenas a hora (HH:mm)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) // Formato de entrada
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault()) // Formato de saída

        var formattedTime = ""
        try {
            val date = dateFormat.parse(horarioAula) // Converte a string de hora para objeto Date
            formattedTime = timeFormat.format(date) // Formata a hora
        } catch (e: Exception) {
            e.printStackTrace() // Caso ocorra erro na formatação
        }

        // Obter o NotificationManager para exibir a notificação
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Criar o canal de notificação para Android 8 e acima
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "AULA_NOTIFICATION_CHANNEL", // ID do canal
                "Aulas Marcadas", // Nome do canal
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações para lembrar aulas marcadas"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // URL da imagem completa (baseUrl + nome da imagem)
        val imageUrl = "$baseUrl$imagemCategoria"

        // Carregar a imagem com Glide
        Glide.with(context)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    // Criar o ícone redondo da imagem
                    val roundedBitmap = createRoundedBitmap(resource)

                    // Texto formatado para a notificação
                    val formattedText = "Tem uma aula de $nomeAula marcada para hoje às $formattedTime horas"

                    // Configuração da notificação
                    val notification = NotificationCompat.Builder(context, "AULA_NOTIFICATION_CHANNEL")
                        .setContentTitle("Lembrete de Aula")
                        .setContentText("Tem uma aula marcada") // Texto curto para a notificação
                        .setStyle(NotificationCompat.BigTextStyle().bigText(formattedText)) // Usando BigTextStyle para suporte a quebras de linha
                        .setSmallIcon(R.drawable.logofeedback1) // Ícone pequeno da notificação
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true) // Remove a notificação após o usuário interagir
                        .setLargeIcon(roundedBitmap) // Define a imagem redonda como a Large Icon
                        .build()

                    // Exibir a notificação
                    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
                }

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                    // Handle image load failure or placeholder
                }
            })
    }

    // Função para criar uma versão arredondada do Bitmap
    private fun createRoundedBitmap(bitmap: Bitmap): Bitmap {
        val size = Math.min(bitmap.width, bitmap.height)
        val rounded = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(rounded)
        val paint = android.graphics.Paint()
        val shader = android.graphics.BitmapShader(bitmap, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
        paint.setShader(shader)
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2).toFloat(), paint)
        return rounded
    }
}
