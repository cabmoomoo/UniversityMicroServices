package UniversityMicroServices.APIGateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import io.javalin.Javalin;
import io.javalin.http.Context;

public class App {
    public static void main(String[] args) {
        Javalin app = Javalin.create();
        app.get("/students", APIGateway::getAllStudents);
        app.post("/students", APIGateway::insertStudent);
        app.get("/courses", APIGateway::getAllCourses);
        app.post("/courses", APIGateway::insertCourse);
        app.patch("/courses/{id}", APIGateway::updateCourse);
        app.start(7000);
    }
}

class APIGateway {
    
    /*
     * Endpoints: Students
     */
    static void getAllStudents(Context ctx) {
        try {
            String studentsAddress = getServiceAddress("students");
            URL studentsURL = new URI("http://" + studentsAddress + "/students").toURL();
            HttpURLConnection conn = (HttpURLConnection) studentsURL.openConnection();
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

    static void insertStudent(Context ctx) {
        try {
            String studentsAddress = getServiceAddress("students");
            URL studentsURL = new URI("http://" + studentsAddress + "/students").toURL();
            HttpURLConnection conn = (HttpURLConnection) studentsURL.openConnection();
            conn.setConnectTimeout(2500);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            String requestBody = ctx.body();
            try (OutputStream os = conn.getOutputStream()) {
                byte[] queryBytes = requestBody.getBytes("utf-8");
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
    
    /*
     * Endpoints: Courses
     */
    static void getAllCourses(Context ctx) {
        try {
            String coursesAddress = getServiceAddress("courses");
            URL coursesURL = new URI("http://" + coursesAddress + "/courses").toURL();
            HttpURLConnection conn = (HttpURLConnection) coursesURL.openConnection();
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
            String coursesAddress = getServiceAddress("courses");
            URL coursesURL = new URI("http://" + coursesAddress + "/courses").toURL();
            HttpURLConnection conn = (HttpURLConnection) coursesURL.openConnection();
            conn.setConnectTimeout(2500);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            String requestBody = ctx.body();
            try (OutputStream os = conn.getOutputStream()) {
                byte[] queryBytes = requestBody.getBytes("utf-8");
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
            String coursesAddress = getServiceAddress("courses");
            URL coursesURL = new URI("http://" + coursesAddress + "/courses/" + ctx.pathParam("id")).toURL();
            HttpURLConnection conn = (HttpURLConnection) coursesURL.openConnection();
            conn.setConnectTimeout(2500);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            String requestBody = ctx.body();
            try (OutputStream os = conn.getOutputStream()) {
                byte[] queryBytes = requestBody.getBytes("utf-8");
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