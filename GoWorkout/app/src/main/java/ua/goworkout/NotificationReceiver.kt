package ua.goworkout
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.text.HtmlCompat
import ua.goworkout.R
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Obter os dados da aula da intent
        val nomeAula = intent.getStringExtra("nome_aula")
        val horarioAula = intent.getStringExtra("horario_aula")

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
            )
            notificationManager.createNotificationChannel(channel)
        }

        val formattedText = "Tem uma aula de $nomeAula marcada para hoje\nàs $formattedTime horas"

        val notification = NotificationCompat.Builder(context, "AULA_NOTIFICATION_CHANNEL")
            .setContentTitle("Lembrete de Aula")
            .setContentText("Tem uma aula marcada") // Texto curto para a notificação
            .setStyle(NotificationCompat.BigTextStyle().bigText(formattedText)) // Usando BigTextStyle para suporte a quebras de linha
            .setSmallIcon(R.drawable.logogo) // Substitua pelo ícone desejado
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Remove a notificação após o usuário interagir
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

    }
}