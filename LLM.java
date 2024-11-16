import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

class LLM {
    public static final String API_KEY = System.getenv("AZURE_API_KEY");
    private static final String COURSE_NUM = "31315";  // Your course ID

    public static void main(String[] args) {
        try {
            // Fetch the latest discussion post
            String latestDiscussion = CanvasAPI.getLatestDiscussion(COURSE_NUM);
            System.out.println("Latest Discussion Post: " + latestDiscussion);

            // Fetch and extract content from the latest PDF if available
            String pdfContent = CanvasAPI.extractPdfText();
            System.out.println("Raw PDF Content: " + pdfContent);

            // Combine discussion post and cleaned PDF content if a PDF exists
            String prompt = latestDiscussion + "\n\nRelevant PDF Content:\n";

            // Ensure the total length of the prompt doesn't exceed limits
            if (prompt.length() > 3000) {  // Adjust this based on token limits
                prompt = prompt.substring(0, 3000) + "...";
            }

            // Generate response using Azure OpenAI
            String response = generateResponse(prompt);
            System.out.println("Response to Discussion Post: " + response);

            // Post the response back to the Canvas discussion
            postResponseToCanvas(response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String generateResponse(String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // JSON payload in chat format
        JSONObject payload = new JSONObject();

        // Remove max_tokens for now (as per your requirement)
        // payload.put("max_tokens", 100);

        // Construct the messages array
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.put(message);

        payload.put("messages", messages);

        // Create the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ai-poncema49411ai671128431104.openai.azure.com/openai/deployments/gpt-35-turbo-16k/chat/completions?api-version=2024-08-01-preview"))
                .header("Content-Type", "application/json")
                .header("api-key", API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        // Send the request and get the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String errorMessage = response.body();
            // Look for the specific error code indicating content policy violation
            if (errorMessage.contains("ResponsibleAIPolicyViolation")) {
                System.out.println("Error: Content filtered due to policy violation.");
            }
            throw new RuntimeException("Failed request: " + response.statusCode() + " - " + errorMessage);
        }

        // Parse response
        JSONObject jsonResponse = new JSONObject(response.body());
        return jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }

    // Post the generated response to the Canvas discussion
    public static void postResponseToCanvas(String response) throws Exception {
        String url = "https://setonhall.instructure.com/api/v1/courses/" + COURSE_NUM + "/discussion_topics/";  // Add discussion topic ID here

        HttpClient client = HttpClient.newHttpClient();
        JSONObject postPayload = new JSONObject();
        postPayload.put("message", response);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + System.getenv("CANVAS_API_KEY"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(postPayload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> postResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (postResponse.statusCode() != 200) {
            throw new RuntimeException("Failed to post response: " + postResponse.statusCode() + " - " + postResponse.body());
        }

        System.out.println("Response successfully posted to Canvas.");
    }
}
