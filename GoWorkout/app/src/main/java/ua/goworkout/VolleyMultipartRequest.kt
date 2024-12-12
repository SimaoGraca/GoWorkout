import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset

class VolleyMultipartRequest(
    method: Int,
    url: String,
    private val responseListener: Response.Listener<JSONObject>,
    private val errorListener: Response.ErrorListener,
    private val params: Map<String, String>,
    private val imageUri: Uri?,
    private val context: Context
) : Request<JSONObject>(method, url, errorListener) {

    private val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    private val byteArrayOutputStream = ByteArrayOutputStream()

    init {
        addTextParams()
        addImageParam()
        finalizeMultipart()
    }

    private fun addTextParams() {
        for ((key, value) in params) {
            byteArrayOutputStream.write(("--$boundary\r\n").toByteArray(Charset.forName("UTF-8")))
            byteArrayOutputStream.write("Content-Disposition: form-data; name=\"$key\"\r\n".toByteArray(Charset.forName("UTF-8")))
            byteArrayOutputStream.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n".toByteArray(Charset.forName("UTF-8")))
            byteArrayOutputStream.write(value.toByteArray(Charset.forName("UTF-8")))
            byteArrayOutputStream.write("\r\n".toByteArray(Charset.forName("UTF-8")))
        }
    }

    private fun addImageParam() {
        imageUri?.let {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(it)
            val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"

            val inputStream: InputStream = contentResolver.openInputStream(it)!!
            val buffer = ByteArray(1024)
            var length: Int
            byteArrayOutputStream.write(("--$boundary\r\n").toByteArray(Charset.forName("UTF-8")))
            byteArrayOutputStream.write("Content-Disposition: form-data; name=\"imagem\"; filename=\"profile.$fileExtension\"\r\n".toByteArray(Charset.forName("UTF-8")))
            byteArrayOutputStream.write("Content-Type: $mimeType\r\n\r\n".toByteArray(Charset.forName("UTF-8")))

            while (inputStream.read(buffer).also { length = it } != -1) {
                byteArrayOutputStream.write(buffer, 0, length)
            }
            byteArrayOutputStream.write("\r\n".toByteArray(Charset.forName("UTF-8")))
        }
    }


    private fun finalizeMultipart() {
        byteArrayOutputStream.write(("--$boundary--\r\n").toByteArray(Charset.forName("UTF-8")))
    }

    override fun getBody(): ByteArray {
        return byteArrayOutputStream.toByteArray()
    }

    override fun getHeaders(): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        headers["Content-Type"] = "multipart/form-data; boundary=$boundary"
        return headers
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> {
        val jsonResponse = String(response.data)
        return Response.success(JSONObject(jsonResponse), HttpHeaderParser.parseCacheHeaders(response))
    }

    override fun deliverResponse(response: JSONObject) {
        responseListener.onResponse(response)
    }
}
