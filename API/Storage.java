package API;

import com.google.gson.Gson;
import jsonhelper.*;

import static spark.Spark.port;
import static spark.Spark.post;

public class Storage {
    public static void main(String args[]) {
        if (args.length > 0) {
            int portNum = Integer.parseInt(args[0]);
            port(portNum);
        }
        Gson g = new Gson();
        post("/storage_size", (request, response) -> {
            String content = request.body();
            PathRequest req;
            ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "File/path cannot be found.");
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            boolean err = false;
            if (!err) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            SizeReturn sizeReturn = new SizeReturn(111);
            String ret = g.toJson(sizeReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });

        post("/storage_read", (request, response) -> {
            String content = request.body();
            ReadRequest req;
            ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "File/path cannot be found.");
            try {
                req = g.fromJson(content, ReadRequest.class);
            } catch (Exception e) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            boolean err = false;
            if (!err) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            DataReturn dataReturn = new DataReturn("");
            String ret = g.toJson(dataReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });

        post("/storage_write", (request, response) -> {
            String content = request.body();
            WriteRequest req;
            ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "File/path cannot be found.");
            try {
                req = g.fromJson(content, WriteRequest.class);
            } catch (Exception e) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            boolean err = false;
            if (!err) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            BooleanReturn booleanReturn = new BooleanReturn(true);
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });

        post("/storage_create", (request, response) -> {
            String content = request.body();
            PathRequest req;
            ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "IllegalArgumentException: path invalid.");
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            boolean err = false;
            if (!err) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            BooleanReturn booleanReturn = new BooleanReturn(true);
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });

        post("/storage_delete", (request, response) -> {
            String content = request.body();
            PathRequest req;
            ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "IllegalArgumentException: path invalid.");
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            boolean err = false;
            if (!err) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            BooleanReturn booleanReturn = new BooleanReturn(true);
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });

        post("/storage_copy", (request, response) -> {
            String content = request.body();
            CopyRequest req;
            ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "IllegalArgumentException: path invalid.");
            try {
                req = g.fromJson(content, CopyRequest.class);
            } catch (Exception e) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            boolean err = false;
            if (!err) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            BooleanReturn booleanReturn = new BooleanReturn(true);
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

}
