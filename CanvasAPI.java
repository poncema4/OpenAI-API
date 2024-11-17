import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.BodyContentHandler;

public class CanvasAPI {
    private static final String TOKEN = System.getenv("YOUR_CANVAS_API_KEY"); // <-- Here is where you place your Canvas_API_Key in your environment for good practice 
    private static final String BASE_URL = "https://setonhall.instructure.com/api/v1";

    // Fetch the latest discussion post's message, attachment link, and topic ID
    public static String[] getLatestDiscussion(String courseNum) throws Exception {
        if (TOKEN == null || TOKEN.isEmpty()) {
            throw new IllegalStateException("Canvas API token (CANVAS_API_KEY) is not set.");
        }

        String url = BASE_URL + "/courses/" + courseNum + "/discussion_topics";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + TOKEN)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch discussion topics: " + response.body());
        }

        JSONArray discussions = new JSONArray(response.body());
        if (discussions.isEmpty()) {
            throw new RuntimeException("No discussion topics found.");
        }

        JSONObject latestDiscussion = discussions.getJSONObject(0);
        String message = latestDiscussion.getString("message");
        int discussionTopicId = latestDiscussion.getInt("id");

        JSONArray attachments = latestDiscussion.optJSONArray("attachments");
        String pdfText = null;

        // Check for PDF attachments
        if (attachments != null) {
            for (int i = 0; i < attachments.length(); i++) {
                JSONObject attachment = attachments.getJSONObject(i);
                if (attachment.getString("content_type").equals("application/pdf")) {
                    String downloadUrl = attachment.getString("url");
                    pdfText = downloadAndExtractPdfText(downloadUrl);
                    break;
                }
            }
        }

        // Return the message, topic ID, and extracted PDF text (if any)
        return new String[]{message, String.valueOf(discussionTopicId), pdfText};
    }

    // Helper method to download and extract text from a PDF file
    private static String downloadAndExtractPdfText(String downloadUrl) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("Authorization", "Bearer " + TOKEN)
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to download PDF. Status code: " + response.statusCode());
        }

        // Save the PDF to a temporary file
        Path tempFile = Files.createTempFile("downloaded", ".pdf");
        try {
            Files.copy(response.body(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            // Extract text from the PDF using Tika
            PDFParser pdfParser = new PDFParser();
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            pdfParser.parse(Files.newInputStream(tempFile), handler, metadata);

            return handler.toString().trim();
        } finally {
            // Delete the temporary file after processing
            Files.deleteIfExists(tempFile);
        }
    }

    // Post the response to the Canvas discussion
    public static void postResponseToCanvas(String response, String discussionTopicId, String courseNum) throws Exception {
        String url = BASE_URL + "/courses/" + courseNum + "/discussion_topics/" + discussionTopicId + "/entries";

        HttpClient client = HttpClient.newHttpClient();
        JSONObject postPayload = new JSONObject();
        postPayload.put("message", response);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(postPayload.toString()))
                .build();

        HttpResponse<String> postResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (postResponse.statusCode() != 201) {
            throw new RuntimeException("Failed to post response: " + postResponse.statusCode() + " - " + postResponse.body());
        }

        System.out.println("Response has been uploaded to Canvas!");
    }
}
