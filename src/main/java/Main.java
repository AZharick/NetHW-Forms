import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
   public static void main(String[] args) throws IOException {
      Server server = new Server();

      server.addHandler("GET", "/index.html", (request, responseStream) -> {
         final var filePath = Path.of(".", "public", "/index.html");
         final var mimeType = Files.probeContentType(filePath);
         final var length = Files.size(filePath);
         responseStream.write(("HTTP/1.1 200 OK\r\n" +
                 "Content-Type: " + mimeType + "\r\n" +
                 "Content-Length: " + length + "\r\n" +
                 "Connection: close\r\n" +
                 "\r\n"
         ).getBytes());
         Files.copy(filePath, responseStream);
         responseStream.flush();
      });

      server.addHandler("POST", "/index.html", (request, responseStream) -> {
         responseStream.write(("HTTP/1.1 200 OK\r\n" +
                 "Content-Type: text/html\r\n" +
                 "Content-Length: 0\r\n" +
                 "Connection: close\r\n" +
                 "\r\n").getBytes());
      });

      server.start();
   }
}