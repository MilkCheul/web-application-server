package webserver;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream(); BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            String line = "";
            String httpRequest = "";

            while(!"".equals(line = br.readLine())){
                if(line == null){
                    return;
                }

                log.info("http request string : " + line);

                httpRequest += line;
            }
            br.readLine();

            String paramString = br.readLine();

            System.out.println(paramString);

            String url = HttpRequestUtils.getUrl(httpRequest);

            if("/user/create".equals(url)){
                Map<String, String> model = HttpRequestUtils.parseQueryString(HttpRequestUtils.getParamString(httpRequest));

                User user = (User)getModel(model, User.class);

                System.out.println(user.toString());
            }

            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private Object getModel(Map<String, String> model, Class clazz) throws Exception{
        Constructor constructor = clazz.getConstructor();
        Object object = constructor.newInstance();

        Method[] methods = clazz.getMethods();

        for (Method method : methods) {
            if("set".equals(method.getName().substring(0,3))){
                String fieldName = method.getName().substring(3);

                method.invoke(object, model.get(changeToLowerCaseForFirstString(fieldName)));
            }
        }

        return object;
    }

    private String changeToLowerCaseForFirstString(String str){
        String transString = str.substring(0, 1);
        transString = transString.toLowerCase();
        transString += str.substring(1);

        return transString;
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
