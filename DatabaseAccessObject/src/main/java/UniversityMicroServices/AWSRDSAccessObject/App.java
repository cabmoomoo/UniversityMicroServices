package UniversityMicroServices.AWSRDSAccessObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.Timer;
import java.util.TimerTask;

import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class App {
    public static void main(String[] args) {
        Javalin app = Javalin.create();

        AWSRDSAccessObject dao = null;
        try {
            dao = new AWSRDSAccessObject();
        } catch (SQLException ex) {
            System.err.println("Failed to connect to database:");
            System.err.println(ex.getMessage());
            System.exit(-1);
        }
        app.get("/", dao::acknowledgePing);
        app.get("/{table}", dao::getTable);
        app.post("/", dao::executeArbitrary);

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
            URL url = new URI("http://192.168.6.200:7000/database").toURL();
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
}

class AWSRDSAccessObject {
    final Connection conn;

    AWSRDSAccessObject() throws SQLException {
        String dbName = "university";
        String userName = "admin";
        String password = "pantsmcdance12";
        String hostname = "university-db.cnasoe6q8gc3.us-east-1.rds.amazonaws.com";
        String port = "3306";
        String jdbcUrl = "jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password;
        this.conn = DriverManager.getConnection(jdbcUrl);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                this.conn.close();
            } catch (SQLException ex) {
            }
        }));
    }

    /*
     * Endpoints
     */
    void getTable(Context ctx) {
        String tableName = ctx.pathParam("table");
        try (PreparedStatement stmt = this.conn.prepareStatement("SELECT * FROM %s;".formatted(tableName))) {
            stmt.execute();
            String result = AWSRDSAccessObject.statementToJSON(stmt);
            ctx.contentType(ContentType.APPLICATION_JSON);
            ctx.result(result);
        } catch (SQLException ex) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.result(ex.getMessage());
        }
    }

    void executeArbitrary(Context ctx) {
        String query = ctx.body();
        try (Statement stmt = this.conn.createStatement()) {
            boolean isResultSet = stmt.execute(query);
            if (isResultSet) {
                String result = AWSRDSAccessObject.statementToJSON(stmt);
                ctx.contentType(ContentType.APPLICATION_JSON);
                ctx.result(result);
            } else {
                ctx.result("Success");
            }
        } catch (SQLException ex) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.result(ex.getMessage());
        }
    }

    /*
     * Utilities
     */
    private static String statementToJSON(Statement stmt) throws SQLException {
        ResultSet rs = stmt.getResultSet();

        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            columns.add(rs.getMetaData().getColumnName(i));
        }

        StringJoiner jsonList = new StringJoiner(",", "[", "]");
        while (rs.next()) {
            StringJoiner jsonObject = new StringJoiner(",", "{", "}");
            for (String columnName : columns) {
                jsonObject.add("\"%s\":\"%s\"".formatted(
                    columnName,
                    rs.getObject(columnName)
                ));
            }
            jsonList.add(jsonObject.toString());
        }
        return jsonList.toString();
    }

    /*
     * Register Heartbeat
     */
    void acknowledgePing(Context ctx) {
        ctx.result("I am indeed still alive");
    }
}