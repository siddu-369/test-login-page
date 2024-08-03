import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;

import java.io.*;
import java.nio.file.Files;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SimpleHttpServer {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new StaticFileHandler());
        server.createContext("/login", new LoginHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String root = "web";
            String path = t.getRequestURI().getPath();
            File file = new File(root + (path.equals("/") ? "/index.html" : path)).getCanonicalFile();

            if (!file.getPath().startsWith(new File(root).getCanonicalPath())) {
                String response = "403 (Forbidden)\n";
                t.sendResponseHeaders(403, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else if (!file.isFile()) {
                String response = "404 (Not Found)\n";
                t.sendResponseHeaders(404, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                t.sendResponseHeaders(200, file.length());
                OutputStream os = t.getResponseBody();
                Files.copy(file.toPath(), os);
                os.close();
            }
        }
    }

    static class LoginHandler implements HttpHandler {
        private static final Random RANDOM = new Random();
        private static final int OTP_LENGTH = 4;
        private static final String EMAIL = "example@example.com";
        private static final long MOBILE_NUMBER = 6301009290;
        private static int currentOtp;
        
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                String json = sb.toString();

                Gson gson = new Gson();
                Map<String, String> data = gson.fromJson(json, Map.class);

                String email = data.get("email");
                String mobileNumberStr = data.get("mobileNumber");
                String otp = data.get("otp");

                Map<String, String> response = new HashMap<>();

                if (mobileNumberStr != null) {
                    long mobileNumber = Long.parseLong(mobileNumberStr);
                    if (mobileNumber == MOBILE_NUMBER) {
                        if (otp == null) {
                            // Generate and send OTP
                            currentOtp = RANDOM.nextInt((int) Math.pow(10, OTP_LENGTH) - 1) + 1000;
                            sendOtp(mobileNumber, currentOtp);
                            response.put("message", "OTP sent to your mobile number. Please verify.");
                        } else if (Integer.parseInt(otp) == currentOtp) {
                            response.put("message", "Login successful.");
                        } else {
                            response.put("message", "Invalid OTP.");
                        }
                    } else {
                        response.put("message", "Invalid mobile number.");
                    }
                } else if (email != null) {
                    if (email.equals(EMAIL)) {
                        if (otp == null) {
                            // Generate and send OTP
                            currentOtp = RANDOM.nextInt((int) Math.pow(10, OTP_LENGTH) - 1) + 1000;
                            sendOtp(email, currentOtp);
                            response.put("message", "OTP sent to your email. Please verify.");
                        } else if (Integer.parseInt(otp) == currentOtp) {
                            response.put("message", "Login successful.");
                        } else {
                            response.put("message", "Invalid OTP.");
                        }
                    } else {
                        response.put("message", "Invalid email address.");
                    }
                } else {
                    response.put("message", "Missing required fields.");
                }

                String responseJson = gson.toJson(response);
                t.getResponseHeaders().set("Content-Type", "application/json");
                t.sendResponseHeaders(200, responseJson.length());
                OutputStream os = t.getResponseBody();
                os.write(responseJson.getBytes());
                os.close();
            }
        }

        private void sendOtp(long mobileNumber, int otp) {
            // Implement actual OTP sending logic (e.g., using an SMS API)
            System.out.println("Sending OTP " + otp + " to mobile number " + mobileNumber);
        }

        private void sendOtp(String email, int otp) {
            // Implement actual OTP sending logic (e.g., using an email API)
            System.out.println("Sending OTP " + otp + " to email " + email);
        }
    }
}
