package com.marlinl.code.takephoto.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by MarlinL on 2015/6/11.
 */
public class HttpRequest {
    private  HttpRequest() {

    }
    private  static  HttpRequest request = new HttpRequest();

    public static HttpRequest newInstanec() {
        return  request;
    }

    public static void doPost(String url,File img) {
        try {
            final String p = "--";
            final String newLine = "\r\n";
            String b = "========7d4a6d158c9";
            URL httpURL = new URL(url);
            URLConnection conn = httpURL.openConnection();
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
            OutputStream out = new DataOutputStream(conn.getOutputStream());

            StringBuffer sb = new StringBuffer();
            sb.append(p);
            sb.append(b);
            sb.append(newLine);
            sb.append("Content-Disposition: form-data;name=\\\"img\\\";filename=\\\""+
                    img.getName()+"\"");
            sb.append(newLine);
            out.write(sb.toString().getBytes());
            DataInputStream in = new DataInputStream(new FileInputStream(img));
            byte[] bytes = new byte[1024];
            int flag = 0;
            while ((flag = in.read(bytes)) != -1) {
                out.write(bytes,0,flag);
            }
            out.write(newLine.getBytes());
            in.close();

            byte[] bb = (newLine+p+b+p+newLine).getBytes();
            out.write(bb);
            out.flush();
            out.close();


        }catch (Exception e) {
            e.printStackTrace();
        }
    }

}
