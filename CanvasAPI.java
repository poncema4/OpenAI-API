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
    private static final String TOKEN = System.getenv("YOUR_CANVAS_API_KEY"); // Your API Key from Canvas
    private static final String BASE_URL = "https://setonhall.instructure.com/api/v1";

    // Fetch the files in the "Documents" folder of the course
    public static JSONArray getFilesInDocumentsFolder(String courseNum) throws Exception {
        if (TOKEN == null || TOKEN.isEmpty()) {
            throw new IllegalStateException("Canvas API token (CANVAS_API_KEY) is not set.");
        }

        String url = BASE_URL + "/courses/" + courseNum + "/files";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + TOKEN)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch files: " + response.body());
        }

        // Parse the JSON response and return files
        JSONArray files = new JSONArray(response.body());
        return files;
    }

    public static String downloadAndExtractPdfText(String downloadUrl) throws Exception {
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

            // Limit the PDF content to the first 500 characters for summary
            String pdfText = handler.toString().trim();
            return pdfText.length() > 500 ? pdfText.substring(0, 500) + "..." : pdfText;
        } finally {
            // Delete the temporary file after processing
            Files.deleteIfExists(tempFile);
        }
    }

    // Fetch the latest PDF file summary from the Documents folder
    public static String getLatestPdfSummary(String courseNum) throws Exception {
        if (TOKEN == null || TOKEN.isEmpty()) {
            throw new IllegalStateException("Canvas API token (CANVAS_API_KEY) is not set.");
        }

        // Fetch the files in the Documents folder
        JSONArray filesInDocuments = getFilesInDocumentsFolder(courseNum);
        String pdfText = null;

        // Look for a PDF file in the "Documents" folder and extract its content
        for (int i = 0; i < filesInDocuments.length(); i++) {
            JSONObject file = filesInDocuments.getJSONObject(i);

            // Debug print the file content to inspect it
            System.out.println("File data: " + file.toString());

            // Check if the file contains the "content_type" key and is a PDF
            if (file.has("content_type") && file.getString("content_type").equals("application/pdf")) {
                String downloadUrl = file.getString("url");
                pdfText = downloadAndExtractPdfText(downloadUrl);  // Get the PDF summary
                break;  // Only need the first PDF found
            }
        }

        return pdfText;  // Return the PDF summary (or null if no PDF was found)
    }

    // Fetch the latest discussion and extract PDF from Documents if no attachment
    public static String[] getLatestDiscussion(String courseNum) throws Exception {
        if (TOKEN == null || TOKEN.isEmpty()) {
            throw new IllegalStateException("Canvas API token (CANVAS_API_KEY) is not set.");
        }

        // Fetch the latest discussion topics
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

        // Step 1: Fetch files in the Documents folder
        JSONArray filesInDocuments = getFilesInDocumentsFolder(courseNum);
        String pdfText = null;

        // Step 2: Look for a PDF file in the "Documents" folder and extract its content
        for (int i = 0; i < filesInDocuments.length(); i++) {
            JSONObject file = filesInDocuments.getJSONObject(i);

            // Debug print the file content to inspect it
            System.out.println("File data: " + file.toString());

            // Check if the file contains the "content_type" key
            if (file.has("content_type") && file.getString("content_type").equals("application/pdf")) {
                String downloadUrl = file.getString("url");
                pdfText = downloadAndExtractPdfText(downloadUrl);
                break; // Only need the first found PDF
            }
        }

        // Return the message, topic ID, and extracted PDF text (if any)
        return new String[]{message, String.valueOf(discussionTopicId), pdfText};
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
