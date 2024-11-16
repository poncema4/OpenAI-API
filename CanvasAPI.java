import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.json.JSONArray;
import org.json.JSONObject;

public class CanvasAPI {
    private static final String TOKEN = System.getenv("CANVAS_API_KEY");
    private static final String BASE_URL = "https://setonhall.instructure.com/api/v1";

    // Fetch the most recent discussion post's message and attachment link
    public static String getLatestDiscussion(String courseNum) throws Exception {
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
        JSONObject latestDiscussion = discussions.getJSONObject(0);

        String message = latestDiscussion.getString("message");
        JSONArray attachments = latestDiscussion.getJSONArray("attachments");

        // Only download the PDF if an attachment is available
        if (!attachments.isEmpty()) {
            JSONObject attachment = attachments.getJSONObject(0);
            String fileType = attachment.getString("content_type");
            if (fileType.equals("application/pdf")) {
                String downloadUrl = attachment.getString("url");
                downloadFile(downloadUrl);
            }
        }

        return message;
    }

    // Download the file to the system's temporary directory
    public static void downloadFile(String url) throws Exception {
        try (InputStream in = URI.create(url).toURL().openStream()) {
            Files.copy(in, Paths.get(System.getenv("TEMP") + "/latest.pdf"));
        }
    }

    // Extract text from the downloaded PDF
    public static String extractPdfText() throws Exception {
        File file = new File(System.getenv("TEMP") + "/latest.pdf");
        if (!file.exists()) {
            return "";
        }

        try (FileInputStream fstream = new FileInputStream(file)) {
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            PDFParser parser = new PDFParser();

            parser.parse(fstream, handler, metadata, context);
            return handler.toString();
        }
    }
}
