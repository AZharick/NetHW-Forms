import org.apache.http.NameValuePair;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
   private static final int PORT = 9999;
   private static final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
           "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
   ServerSocket serverSocket;
   Socket clientSocket;
   private ConcurrentHashMap<String, Handler> getHandlers;
   private ConcurrentHashMap<String, Handler> postHandlers;

   public Server() {
      getHandlers = new ConcurrentHashMap<>();
      postHandlers = new ConcurrentHashMap<>();
   }

   public void addHandler(String method, String path, Handler handler) {
      if (method.equals("GET") && !getHandlers.containsKey(path)) {
         getHandlers.put(path, handler);
      } else if (method.equals("POST") && !postHandlers.containsKey(path)) {
         postHandlers.put(path, handler);
      }
   }

   public void start() throws IOException {
      serverSocket = new ServerSocket(PORT);
      System.out.println(getDateAndTime() + "Server started at port " + PORT);
      ExecutorService threadPool = Executors.newFixedThreadPool(64);

      while (true) {
         clientSocket = serverSocket.accept();
         System.out.println(getDateAndTime() + "Client connected: " + clientSocket.getInetAddress().getHostAddress());
         threadPool.submit(() -> {
            try {
               handleRequest(clientSocket);
            } catch (IOException e) {
               throw new RuntimeException(e);
            }
         });
      }
   }

   private void handleRequest(Socket socket) throws IOException {
      try (
              final var in = new BufferedInputStream(socket.getInputStream());
              final var out = new BufferedOutputStream(socket.getOutputStream())
      ) {
         Request request = Request.parseRequest(in);
         displayRequest(request);

         if (request.getMethod().equals("GET") && getHandlers.containsKey(request.getShortPath())) {
            Handler handler = getHandlers.get(request.getShortPath());
            handler.handle(request, out);

         } else if (request.getMethod().equals("POST") && postHandlers.containsKey(request.getShortPath())) {
            Handler handler = postHandlers.get(request.getShortPath());
            handler.handle(request, out);

         } else {
            System.out.println(getDateAndTime() + "> 404");
            out.write(("HTTP/1.1 404 Not Found\r\n" +
                    "Content-Length: 0\r\n" +
                    "Connection: close\r\n" +
                    "\r\n").getBytes());
         }
         out.flush();
      }//try-with-res
      catch (URISyntaxException e) {
         throw new RuntimeException(e);
      }
   }//handleRequest

   private void displayRequest(Request request) {
      System.out.println("\nDisplaying request:");
      System.out.println("Method: " + request.getMethod());
      System.out.println("Path: " + request.getFullpath());
      System.out.println("Headers: " + request.getHeaders());
      System.out.println("Query: ");
      for (NameValuePair param : request.getParams()) {
         System.out.println(param.getName() + " : " + param.getValue());
      }
   }

   private static String getDateAndTime() {
      String datePattern = "[HH:mm:ss] ";
      DateFormat d = new SimpleDateFormat(datePattern);
      Date today = Calendar.getInstance().getTime();
      return d.format(today);
   }

}