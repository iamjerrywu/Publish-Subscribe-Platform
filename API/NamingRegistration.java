package API;

import com.google.gson.Gson;
import jsonhelper.*;

import static spark.Spark.port;
import static spark.Spark.post;

public class NamingRegistration {
    public static void main(String args[]) {
        port(8090);
        Gson g = new Gson();
        post("/register", (request, response) -> {
            String content = request.body();
            RegisterRequest req;
            try {
                req = g.fromJson(content, RegisterRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "File/path cannot be found.");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            boolean err = false;
            if (!err) {
                ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "File/path cannot be found.");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            FilesReturn filesReturn = new FilesReturn(null);
            String ret = g.toJson(filesReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }
}
