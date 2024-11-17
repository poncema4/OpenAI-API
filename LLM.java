import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class LLM {
    public static final String API_KEY = System.getenv("YOUR_OPENAI_API_KEY"); // Your OpenAI API Key
    private static final String COURSE_NUM = "12345";  // Your course ID

    public static void main(String[] args) {
        try {
            if (API_KEY == null || API_KEY.isEmpty()) {
                throw new IllegalStateException("OpenAI API key (OPENAI_API_KEY) is not set.");
            }

            // Fetch the latest discussion post
            String[] latestDiscussionData = CanvasAPI.getLatestDiscussion(COURSE_NUM);
            String latestDiscussion = latestDiscussionData[0];
            String discussionTopicId = latestDiscussionData[1];
            String latestPdf = latestDiscussionData[2];

            // Print the latest discussion post
            System.out.println("Latest Discussion Post:\n" + latestDiscussion);

            // Print the latest PDF content (if available)
            if (latestPdf != null && !latestPdf.isEmpty()) {
                System.out.println("Latest PDF:\n" + latestPdf);
            } else {
                System.out.println("No PDF attachment in the latest discussion post.");
            }

            // Generate response using OpenAI
            String prompt = latestDiscussion + (latestPdf != null && !latestPdf.isEmpty() ? "\n\nExtracted from PDF:\n" + latestPdf : "");
            String response = generateResponse(prompt);
            System.out.println("Generated Response:\n" + response);

            // Post the response back to Canvas
            CanvasAPI.postResponseToCanvas(response, discussionTopicId, COURSE_NUM);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Generate a response using OpenAI API
    public static String generateResponse(String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // JSON payload for OpenAI API
        JSONObject payload = new JSONObject();
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.put(message);

        payload.put("model", "gpt-3.5-turbo");  // You can change this to "gpt-4" if needed
        payload.put("messages", messages);

        // Create the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed request: " + response.statusCode() + " - " + response.body());
        }

        // Parse the OpenAI response
        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }
}
