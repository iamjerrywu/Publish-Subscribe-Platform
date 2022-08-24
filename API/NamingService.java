package API;

import static spark.Spark.port;
import static spark.Spark.post;

import com.google.gson.Gson;
import jsonhelper.*;

import java.io.*;

public class NamingService {

    public static void main(String args[]) {
        port(8080);
        Gson g = new Gson();
        post("/is_valid_path", (request, response) -> {
            String content = request.body();
            PathRequest req;
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "File/path cannot be found.");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            BooleanReturn booleanReturn = new BooleanReturn(req.path != null);
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });

        post("/getstorage", (request, response) -> {

            String content = request.body();
            PathRequest req;
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "File/path cannot be found.");
                String ret = g.toJson(excepRet);
                response.status(404);
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
            ServerInfo serverInfo = new ServerInfo("localhost", 1111);
            String ret = g.toJson(serverInfo);
            response.status(200);
            response.type("application/json");
            return ret;
        });

        post("/delete", (request, response) -> {
            String content = request.body();
            PathRequest req;
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "File/path cannot be found.");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            boolean err = false;
            if (!err) {
                ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "the object or parent directory does not exist.");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            BooleanReturn booleanReturn = new BooleanReturn(req.path != null);
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });

        post("/create_directory", (request, response) -> {
            String content = request.body();
            PathRequest req;
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "File/path cannot be found.");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            boolean err = false;
            if (!err) {
                ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "parent directory does not exist.");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            BooleanReturn booleanReturn = new BooleanReturn(req.path != null);
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });

        post("/create_file", (request, response) -> {
            String content = request.body();
            PathRequest req;
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "File/path cannot be found.");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            Object err = new FileNotFoundException();
            if (err instanceof Throwable) {
                ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "parent directory does not exist.");
                if (err instanceof IllegalStateException) {
                    response.status(409);
                    excepRet = new ExceptionReturn("IllegalStateException", "no storage servers are connected to the naming server.");
                } else {
                    response.status(404);
                }
                String ret = g.toJson(excepRet);
                response.type("application/json");
                return ret;
            }
            BooleanReturn booleanReturn = new BooleanReturn(req.path != null);
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });

        post("/list", (request, response) -> {
            String content = request.body();
            PathRequest req;
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "File/path cannot be found.");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            Object err = new FileNotFoundException();
            if (err instanceof Throwable) {
                ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "given path does not refer to a directory.");
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

        post("/is_directory", (request, response) -> {
            String content = request.body();
            PathRequest req;
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "File/path cannot be found.");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            Object err = new FileNotFoundException();
            if (err instanceof Throwable) {
                ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "given path does not refer to a directory.");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            BooleanReturn booleanReturn = new BooleanReturn(req.path != null);
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });

        post("/unlock", (request, response) -> {
            String content = request.body();
            LockRequest req;
            try {
                req = g.fromJson(content, LockRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "File/path cannot be found.");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            Object err = new IllegalArgumentException();
            if (err instanceof Throwable) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "path cannot be found.");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            response.status(200);
            response.type("application/json");
            return "";
        });

        post("/lock", (request, response) -> {
            String content = request.body();
            LockRequest req;
            try {
                req = g.fromJson(content, LockRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "File/path cannot be found.");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            Object err = new FileNotFoundException();
            if (err instanceof Throwable) {
                ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "path cannot be found.");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            response.status(200);
            response.type("application/json");
            return "";
        });

    }
}
