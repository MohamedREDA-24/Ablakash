package com.xperiencelabs.arapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Html
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private val activityScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var visualize3DButton: Button
    private lateinit var uploadImageButton: Button
    private lateinit var instructionsButton: ImageButton  // Top-left button for instructions
    private var latestImageUrl: String? = null
    private var sessionId: String? = null

    val imageUrlList = mutableListOf<String>()
    val imageUrlList2d = mutableListOf<String>()

    private companion object {
        const val IMAGE_PICK_REQUEST_CODE = 1001
        const val RECOMMEND_URL = "http://13.92.86.232/recommend/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Initialize views from the layout
        chatRecyclerView = findViewById(R.id.chat_recycler_view)
        messageInput = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.send_button)
        visualize3DButton = findViewById(R.id.btn_visualize_3d)
        uploadImageButton = findViewById(R.id.btn_upload_image)
        instructionsButton = findViewById(R.id.btn_left_icon) // This is our info/instructions button

        // Setup initial states
        visualize3DButton.isEnabled = false
        sendButton.isEnabled = false

        // Configure RecyclerView
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(messages)
        chatRecyclerView.adapter = chatAdapter

        // Initialize session
        activityScope.launch {
            try {
                val startResponse = makeStartRequest()
                handleStartResponse(startResponse)
                sendButton.isEnabled = true
            } catch (e: Exception) {
                handleError(e)
                finish()
            }
        }

        // Set up button click listeners
        sendButton.setOnClickListener { handleSendMessage() }
        visualize3DButton.setOnClickListener { handle3DVisualization() }
        uploadImageButton.setOnClickListener { openImagePicker() }
        instructionsButton.setOnClickListener { showInstructionsDialog() } // Show pop-up on tap
    }

    // Show a dialog with instructions for the user
    private fun showInstructionsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Instructions")
        builder.setMessage(
            "1-Start a chat to share your furniture preferences and features.\n\n" +
                    "2. When you finish describing your furniture features, type “done” to receive your furniture recommendation from our store.\n\n" +
                    "3. Use the AR mode to try the selected furniture in your room and screenshot  an image that includes both your room and the chosen piece.\n\n" +
                    "4. Exit AR mode and click the external button (camera button) to either edit your room style or receive a new furniture suggestion.\n\n" +
                    "5. If prompted with “Recommend one? (yes/no)” and you respond with “no,” the system will fetch an alternative recommendation from trusted sources (e.g., Amazon).\n\n"+
                    "6. You can repeat this cycle by confirming “New recommendation? (yes/no)” to view additional options.\n\n"
        )
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun handleSendMessage() {
        val userMessage = messageInput.text.toString().trim()
        if (userMessage.isEmpty()) return

        if (sessionId == null) {
            Toast.makeText(this, "Session not initialized", Toast.LENGTH_SHORT).show()
            return
        }

        addUserMessage(userMessage)
        activityScope.launch {
            try {
                val serverResponse = makeChatRequest(userMessage, sessionId!!)
                handleServerResponse(serverResponse)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun handle3DVisualization() {
        val intent = Intent(this, MainActivity::class.java).apply {
            if (imageUrlList.isNotEmpty()) {
                putStringArrayListExtra("IMAGE_URL_LIST", ArrayList(imageUrlList))
                putStringArrayListExtra("IMAGE_URL_LIST_2D", ArrayList(imageUrlList2d))
            } else {
                latestImageUrl?.let {
                    putExtra("IMAGE_URL", it.replace(".jpg", ".glb"))
                }
            }
        }
        startActivity(intent)
    }

    private fun addUserMessage(text: String) {
        messages.add(ChatMessage(message = text, isSent = true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        messageInput.text.clear()
        chatRecyclerView.smoothScrollToPosition(messages.size - 1)
    }

    // Region: Network Operations
    private suspend fun makeStartRequest(): String = withContext(Dispatchers.IO) {
        val url = URL("http://13.92.86.232/start")
        var result = ""

        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Accept", "application/json")

            try {
                result = if (responseCode in 200..299) {
                    inputStream.bufferedReader().use(BufferedReader::readText)
                } else {
                    "HTTP Error: $responseCode"
                }
                Log.d("ChatActivity", "Start Response: $result")
            } finally {
                disconnect()
            }
        }
        return@withContext result
    }

    private suspend fun makeChatRequest(userInput: String, sessionId: String): String =
        withContext(Dispatchers.IO) {
            val url = URL("http://13.92.86.232/my-chat/")
            var result = ""

            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                val jsonInput = JSONObject().apply {
//                    put("session_id", sessionId)
                    put("text", userInput)
                }.toString()
//                Log.d("ChatActivity", "Chat Response: $jsonInput")

                outputStream.use { os ->
                    OutputStreamWriter(os, "UTF-8").use { writer ->
                        writer.write(jsonInput)
                        writer.flush()
                    }
                }

                try {
                    result = if (responseCode in 200..299) {
                        inputStream.bufferedReader().use(BufferedReader::readText)
                    } else {
                        errorStream?.bufferedReader()?.use(BufferedReader::readText)
                            ?: "HTTP Error: $responseCode"
                    }
                    Log.d("ChatActivity", "Chat Response: $result")
                } finally {
                    disconnect()
                }
            }
            return@withContext result
        }

    // Region: Image Upload Handling
    private fun openImagePicker() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(this, IMAGE_PICK_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                // Display the selected image in the chat immediately
                addUserImageMessage(uri)

                // Continue with uploading the image
                activityScope.launch {
                    try {
                        val response = uploadImage(uri)
                        handleRecommendationResponse(response)
                    } catch (e: Exception) {
                        handleError(e)
                    }
                }
            }
        }
    }

    private fun addUserImageMessage(uri: Uri) {
        // Create a chat message using the image URI.
        // Make sure your ChatAdapter and ChatMessage model can handle image messages.
        messages.add(ChatMessage(imageUrl = uri.toString(), isSent = true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        chatRecyclerView.smoothScrollToPosition(messages.size - 1)
    }

    private suspend fun uploadImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val url = URL(RECOMMEND_URL)
        val connection = url.openConnection() as HttpURLConnection
        val boundary = "Boundary-${System.currentTimeMillis()}"

        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setRequestProperty("accept", "application/json")

            val outputStream = connection.outputStream
            val writer = OutputStreamWriter(outputStream)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileName = getFileNameFromUri(uri) ?: "uploaded_image.jpg"
                writer.append("--$boundary\r\n")
                    .append("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
                    .append("Content-Type: image/jpeg\r\n\r\n")
                    .flush()

                inputStream.copyTo(outputStream)
                outputStream.flush()
            }

            writer.append("\r\n--$boundary--\r\n").flush()

            return@withContext if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: "Error: ${connection.responseCode}"
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                ) else null
            }
            "file" -> uri.lastPathSegment
            else -> null
        }
    }

    // Region: Response Handling
    private fun handleStartResponse(response: String) {
        try {
            val jsonObject = JSONObject(response)
            sessionId = jsonObject.optString("session_id", "").also {
                if (it.isEmpty()) throw IllegalStateException("Empty session_id")
            }
            Log.d("ChatActivity", "New session ID: $sessionId")
        } catch (e: Exception) {
            Log.e("ChatActivity", "Session initialization failed", e)
            throw e
        }
    }

    private fun handleServerResponse(response: String) {
        try {
            Log.d("ChatActivity", "Raw response: $response")

            // Parse the outer JSON response
            val jsonResponse = JSONObject(response)

            // First check if it's a furniture recommendation response
            if (jsonResponse.has("results")) {
                handleFurnitureResults(jsonResponse)
                return
            }

            // Existing handling for other response types
            val responseContent = jsonResponse.optString("response")
            when (val parsed = tryParseJson(responseContent)) {
                is JSONArray -> handleRagResponse(parsed)
                is JSONObject -> handleJsonObject(parsed)
                else -> handlePotentialMalformedResponse(responseContent)
            }
        } catch (e: Exception) {
            Log.e("ChatActivity", "Response handling failed", e)
            handleTextResponse("Sorry, I had trouble processing that request. Please try again.")
        }
    }

    private fun handleFurnitureResults(jsonResponse: JSONObject) {
        try {
            val resultsArray = jsonResponse.getJSONArray("results")
            val formattedResponse = StringBuilder().apply {
                append("Here are the furniture items I found:\n\n")
            }

            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.getJSONObject(i)
                val title = item.getString("title")
                val url = item.getString("url")

                formattedResponse.append("• ${title.trim()}\n")
                formattedResponse.append("   ${createClickableLink(url)}\n\n")
            }

            handleFormattedResults(formattedResponse.toString())

        } catch (e: Exception) {
            Log.e("ChatActivity", "Error processing furniture results", e)
            handleTextResponse("Found some items but had trouble displaying them properly")
        }
    }

    private fun createClickableLink(url: String): String {
        return "<a href=\"$url\">View Product</a>"
    }

    private fun handleFormattedResults(formattedText: String) {
        runOnUiThread {
            val spannedText = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)
            messages.add(
                ChatMessage(
                    message = spannedText.toString(),
                    isSent = false,
                    isReply = true
                )
            )
            chatAdapter.notifyItemInserted(messages.size - 1)
            chatRecyclerView.smoothScrollToPosition(messages.size - 1)
        }
    }


    private fun tryParseJson(response: String): Any? {
        return try {
            JSONTokener(response).nextValue()
        } catch (e: JSONException) {
            null
        }
    }

    private fun handleRagResponse(jsonArray: JSONArray) {
        Log.d("ChatActivity", "Handling RAG response: ${jsonArray.toString()}")

        val imageItems = mutableListOf<String>()
        var textResponse = ""

        for (i in 0 until jsonArray.length()) {
            try {
                when (val item = jsonArray[i]) {
                    is JSONObject -> {
                        item.optString("image_path").takeIf { it.isNotEmpty() }?.let {
                            val cleanPath = it.replace(" ", "_")
                            if (!imageItems.contains(cleanPath)) {
                                imageItems.add(cleanPath)
                            }
                        }
                    }
                    is String -> {
                        // Append text responses, maintaining formatting
                        if (textResponse.isNotEmpty()) {
                            textResponse += "\n"
                        }
                        textResponse += item.replace("\\n", "\n")
                            .replace(Regex("""[\[\]{}"]"""), "")
                            .trim()
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error processing array item: ${jsonArray[i]}", e)
            }
        }

        // Process images first
        imageItems.forEach { imagePath ->
            val imageUrl = buildImageUrl(imagePath)
            handleImageUrl(imageUrl)
        }

        // Then handle text response
        when {
            textResponse.isNotEmpty() -> handleTextResponse(textResponse)
            imageItems.isNotEmpty() -> handleTextResponse("Here are the items I found:")
            else -> handleTextResponse("I found some items but couldn't display them properly.")
        }
    }

    private fun handlePotentialMalformedResponse(response: String) {
        // Handle potential scraper response first
        if (response.contains("## Final Answer:")) {
            handleTextResponse(extractFinalAnswer(response))
            return
        }

        // Check for furniture-related response patterns
        val imagePattern = Regex("""image_path["']?\s*[:=]\s*["']?([\w\s_\-/]+\.(?:jpg|png|jpeg))""", RegexOption.IGNORE_CASE)
        val textPattern = Regex("""(?:Here are the furniture items I found for you:[\s\S]*?)(?=\\n}|$|\{)""")

        val images = imagePattern.findAll(response)
            .map { it.groupValues[1].replace(" ", "_") }
            .distinct()
            .toList()

        val textMatch = textPattern.find(response)?.value
            ?.replace("\\n", "\n")
            ?.replace(Regex("""[\[\]{}"]"""), "")
            ?.trim()

        if (images.isNotEmpty() || textMatch != null) {
            // Process found images
            images.forEach { imagePath ->
                handleImageUrl(buildImageUrl(imagePath))
            }

            // Handle any text content
            textMatch?.let { handleTextResponse(it) }
                ?: if (images.isNotEmpty()) handleTextResponse("Here are the items I found:") else TODO()
        } else {
            // Fallback to treating as plain text response
            handleTextResponse(response.replace("\\n", "\n").trim())
        }
    }

    private fun handleJsonObject(jsonObject: JSONObject) {
        Log.d("ChatActivity", "Handling JSON object response")

        when {
            jsonObject.has("image_path") -> {
                val imagePath = jsonObject.getString("image_path")
                handleImageUrl(buildImageUrl(imagePath))

                // Check for additional text content
                jsonObject.optString("caption")?.takeIf { it.isNotEmpty() }?.let {
                    handleTextResponse(it)
                }
            }
            else -> handleTextResponse(jsonObject.toString())
        }
    }

    private fun buildImageUrl(imagePath: String): String {
        val baseUrl = "http://13.92.86.232/static/" // Replace with your actual base URL
        return "$baseUrl/${imagePath.replace(" ", "_")}"
    }


    private fun extractFinalAnswer(text: String): String {
        val pattern = Regex("## Final Answer:(.*?)(?:\$|#)", RegexOption.DOT_MATCHES_ALL)
        return pattern.find(text)?.groupValues?.get(1)?.trim() ?: text
    }

    // Existing UI update methods
    private fun handleImageUrl(imageUrl: String) {
        Log.d("ChatActivity", "Processing image URL: $imageUrl")

        latestImageUrl = imageUrl
        imageUrlList2d.add(imageUrl)
        imageUrlList.add(imageUrl.replace(".jpg", ".glb"))

        runOnUiThread {
            visualize3DButton.isEnabled = true
            messages.add(
                ChatMessage(
                    imageUrl = imageUrl,
                    isSent = false,
                    isReply = true
                )
            )
            chatAdapter.notifyItemInserted(messages.size - 1)
            chatRecyclerView.smoothScrollToPosition(messages.size - 1)
        }
    }

    private fun handleTextResponse(text: String) {
        Log.d("ChatActivity", "Processing text response: $text")

        runOnUiThread {
            messages.add(
                ChatMessage(
                    message = text,
                    isSent = false,
                    isReply = true
                )
            )
            chatAdapter.notifyItemInserted(messages.size - 1)
            chatRecyclerView.smoothScrollToPosition(messages.size - 1)
        }
    }
    private fun handleRecommendationResponse(response: String) {
        try {
            Log.d("ChatActivity", "Recommendation response: $response")
            val json = JSONObject(response)
            val recommendation = json.optString("recommendation", "")

            if (recommendation.isNotEmpty()) {
                runOnUiThread {
                    messages.add(ChatMessage(
                        message = recommendation,
                        isSent = false,
                        isReply = true
                    ))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    chatRecyclerView.smoothScrollToPosition(messages.size - 1)
                }
            }
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error handling recommendation", e)
            handleError(e)
        }
    }

    private fun handleError(error: Exception) {
        runOnUiThread {
            Toast.makeText(this, "Error: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }
}
