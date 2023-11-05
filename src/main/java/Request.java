import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class Request {
   private String method;
   private String path;
   private List<String> headers;
   private String body;
   private List<NameValuePair> params;

   public Request(String method, String path, List<String> headers, List<NameValuePair> params) {
      this.method = method;
      this.path = path;
      this.headers = headers;
      this.params = params;
   }

   public Request(String method, String path) {
      this.method = method;
      this.path = path;
   }

   //GnS ===========================
   public String getMethod() {
      return method;
   }
   public void setMethod(String method) {
      this.method = method;
   }
   public List<String> getHeaders() {
      return headers;
   }
   public void setHeaders(List<String> headers) {
      this.headers = headers;
   }
   public List<NameValuePair> getParams() {
      return params;
   }
   public void setParams(List<NameValuePair> params) {
      this.params = params;
   }
   public String getBody() {
      return body;
   }
   public void setBody(String body) {
      this.body = body;
   }
   public String getPath() {
      return path;
   }
   public void setPath(String path) {
      this.path = path;
   }

   static Request parseRequest(BufferedInputStream in) throws IOException, URISyntaxException {
      final List<String> methodsList = List.of("GET", "POST");

      final var limit = 4096;
      in.mark(limit);
      final var buffer = new byte[limit];
      final var read = in.read(buffer);

      // seeking request line
      final var requestLineDelimiter = new byte[]{'\r', '\n'};
      final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
      if (requestLineEnd == -1) {
         return null;
      }

      // reading request line
      final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
      if (requestLine.length != 3) {
         return null;
      }

      // check for method
      final var method = requestLine[0];
      if (!methodsList.contains(method)) {
         return null;
      }
      System.out.println(method);

      final var path = requestLine[1];
      if (!path.startsWith("/")) {
         return null;
      }
      System.out.println(path);

      // seeking headers
      final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
      final var headersStart = requestLineEnd + requestLineDelimiter.length;
      final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
      if (headersEnd == -1) {
         return null;
      }

      in.reset();
      in.skip(headersStart);

      final var headerBytes = in.readNBytes(headersEnd - headersStart);
      List<String> headers = Arrays.asList(new String(headerBytes).split("\r\n"));
      List<NameValuePair> params = URLEncodedUtils.parse(new URI(path), StandardCharsets.UTF_8);
      return new Request(method, path, headers, params);
   }

   private static int indexOf(byte[] array, byte[] target, int start, int max) {
      outer:
      for (int i = start; i < max - target.length + 1; i++) {
         for (int j = 0; j < target.length; j++) {
            if (array[i + j] != target[j]) {
               continue outer;
            }
         }
         return i;
      }
      return -1;
   }

   public NameValuePair getQueryParam(String name) {
      return getParams().stream()
              .filter(param -> param.getName().equalsIgnoreCase(name))
              .findFirst().orElse(new NameValuePair() {
                 @Override
                 public String getName() {
                    return name;
                 }
                 @Override
                 public String getValue() {
                    return "";
                 }
              });
   }
}