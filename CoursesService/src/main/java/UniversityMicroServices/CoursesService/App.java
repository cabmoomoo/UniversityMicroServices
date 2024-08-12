package UniversityMicroServices.CoursesService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import io.javalin.Javalin;
import io.javalin.http.Context;

public class App {
    public static void main(String[] args) {
        Javalin app = Javalin.create();
        app.get("/courses", Course::getAllCourses);
        app.post("/courses", Course::insertCourse);
        app.patch("/courses/{id}", Course::updateCourse);
        app.post("/dev/setuptable", Course::setupCoursesTable);
        app.get("/", App::acknowledgePing);
        app.start(7000);

        Timer registerServiceTimer = new Timer("registerService");
        registerServiceTimer.scheduleAtFixedRate(
            new TimerTask() {
                @Override
                public void run() {
                    boolean registered = App.registerService();
                    if (registered) {
                        System.out.println("Service successfully registered");
                        registerServiceTimer.cancel();
                    }
                }
            }, 
            0, 
            5000
        );
    }

    static boolean registerService() {
        try {
            URL url = new URI("http://192.168.6.200:7000/courses").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2500);
            conn.setRequestMethod("POST");
            conn.connect();
            if (conn.getResponseCode() != 200) {
                throw new IOException("Discovery service exists, but failed to register me.");
            }
        } catch (IOException | URISyntaxException ex) {
            System.err.println("Could not register with discovery service...");
            System.err.println(ex.getMessage());
            return false;
        }
        return true;
    }

    /*
     * Register Heartbeat
     */
    static void acknowledgePing(Context ctx) {
        ctx.result("I am indeed still alive");
    }
}

class Course {
    private int id;
    private String name;
    private String description;

    /*
     * REST Endpoints
     */
    static void getAllCourses(Context ctx) {
        try {
            String databaseAddress = getServiceAddress("database");
            URL databaseURL = new URI("http://" + databaseAddress + "/courses").toURL();
            HttpURLConnection conn = (HttpURLConnection) databaseURL.openConnection();
            conn.setConnectTimeout(2500);
            conn.setDoInput(true);
            conn.connect();
            ctx.status(conn.getResponseCode());
            if (conn.getContentType() != null) {
                ctx.contentType(conn.getContentType());
            }
            ctx.result(conn.getInputStream());
        } catch (URISyntaxException | IOException ex) {
            ctx.status(500);
            ctx.result(ex.getMessage());
        }
    }

    static void insertCourse(Context ctx) {
        try {
            String databaseAddress = getServiceAddress("database");
            URL databaseURL = new URI("http://" + databaseAddress).toURL();
            HttpURLConnection conn = (HttpURLConnection) databaseURL.openConnection();
            conn.setConnectTimeout(2500);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            String requestBody = ctx.body();
            String[] courseParts = requestBody.split(",");
            String query = "INSERT INTO courses (name, description) VALUES (\'%s\', \'%s\');".formatted(courseParts[0], courseParts[1]);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] queryBytes = query.getBytes("utf-8");
                os.write(queryBytes, 0, queryBytes.length);
            }
            conn.connect();
            ctx.status(conn.getResponseCode());
            if (conn.getContentType() != null) {
                ctx.contentType(conn.getContentType());
            }
            ctx.result(conn.getInputStream());
        } catch (URISyntaxException | IOException ex) {
            ctx.status(500);
            ctx.result(ex.getMessage());
        }
    }

    static void updateCourse(Context ctx) {
        try {
            String databaseAddress = getServiceAddress("database");
            URL databaseURL = new URI("http://" + databaseAddress).toURL();
            HttpURLConnection conn = (HttpURLConnection) databaseURL.openConnection();
            conn.setConnectTimeout(2500);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            String courseID = ctx.pathParam("id");
            String requestBody = ctx.body();
            String[] courseParts = requestBody.split(",");
            String query = "UPDATE courses SET name = %s, description = %s WHERE id = %s".formatted(courseParts[0], courseParts[1], courseID);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] queryBytes = query.getBytes("utf-8");
                os.write(queryBytes, 0, queryBytes.length);
            }
            conn.connect();
            ctx.status(conn.getResponseCode());
            if (conn.getContentType() != null) {
                ctx.contentType(conn.getContentType());
            }
            ctx.result(conn.getInputStream());
        } catch (URISyntaxException | IOException ex) {
            ctx.status(500);
            ctx.result(ex.getMessage());
        }
    }

    static void setupCoursesTable(Context ctx) {
        try {
            String databaseAddress = getServiceAddress("database");
            URL databaseURL = new URI("http://" + databaseAddress).toURL();
            HttpURLConnection conn = (HttpURLConnection) databaseURL.openConnection();
            conn.setConnectTimeout(2500);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            String query = 
                """
                CREATE TABLE IF NOT EXISTS courses (
                    id INTEGER AUTO_INCREMENT PRIMARY KEY,
                    name TEXT,
                    description TEXT
                );""";
            try (OutputStream os = conn.getOutputStream()) {
                byte[] queryBytes = query.getBytes("utf-8");
                os.write(queryBytes, 0, queryBytes.length);
            }
            conn.connect();
            ctx.status(conn.getResponseCode());
            ctx.result(conn.getInputStream());
        } catch (URISyntaxException | IOException ex) {
            ctx.status(500);
            ctx.result(ex.getMessage());
        }
    }



    /*
     * Utilities
     */
    static String getServiceAddress(String serviceName) throws URISyntaxException, IOException {
        URL discoveryServiceURL = new URI("http://192.168.6.200:7000/" + serviceName).toURL();
        HttpURLConnection discoveryConn = (HttpURLConnection) discoveryServiceURL.openConnection();
        discoveryConn.setConnectTimeout(2500);
        discoveryConn.setDoInput(true);
        discoveryConn.connect();
        char[] bodyArray = new char[discoveryConn.getContentLength()];
        try (InputStream discoveryOS = discoveryConn.getInputStream(); 
            BufferedReader br = new BufferedReader(new InputStreamReader(discoveryOS));)
        {
            br.read(bodyArray);
        }
        discoveryConn.disconnect();
        return new String(bodyArray);
    }
}
