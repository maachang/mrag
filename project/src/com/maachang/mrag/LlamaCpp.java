package com.maachang.mrag;

import java.io.*;
import java.util.*;
import java.net.*;
import java.net.http.*;

/**
 * llama.cpp アクセス処理.
 */
public class LlamaCpp {
    // HttpClient.
    private static final HttpClient HTTPCLIENT = HttpClient.newHttpClient();

    // llama.cppの server にPOSTでアクセス.
    // baseUrl: http://domain:port までのURLを設定します.
    // endpoint: path/.../key を設定します.
    // body: POST送信対象のJSON情報を設定します.
    // 戻り値: JSON結果が返却されます.
    private static final Object fetch(String baseUrl, String endpoint, Object body) {
        if(baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if(endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);
        }
        if(!(body instanceof String)) {
            body = Json.encode(body);
        }
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + endpoint))
                .header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString((String)body))
                .build();
        try {
            final HttpResponse<String> httpResponse = HTTPCLIENT.send(
                httpRequest, HttpResponse.BodyHandlers.ofString());
            String resBody = httpResponse.body();
            Object res = Json.decode(resBody);
            // エラーの場合は以下のように返却される.
            // {error={code, message}}
            if(res instanceof Map && ((Map)res).containsKey("error")) {
                // エラー返却.
                MRagException mre = new MRagException(resBody); // 最低限のエラー.
                try {
                    // errorCodeとメッセージを分離できた場合.
                    Map err = (Map)((Map)res).get("error");
                    mre = new MRagException(
                        ((Number)err.get("code")).intValue(),
                        (String)err.get("message"));
                } catch(Exception ee) {}
                throw mre;
            }
            // 正常終了.
            return res;
        } catch(MRagException me) {
            throw me;
        } catch(Exception e) {
            throw new MRagException(e);
        }
    }

    // ヘルスチェック.
    // baseUrl: http://domain:port までのURLを設定します.
    // 戻り値: true の場合、利用可能です.
    public static final boolean health(String baseUrl) {
        if(baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        try {
            final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health"))
                .build();
            final HttpResponse<String> httpResponse = HTTPCLIENT.send(
                httpRequest, HttpResponse.BodyHandlers.ofString());
            httpResponse.body();
        } catch(Exception e) {
            // つながらない場合は false返却.
            return false;
        }
        // 接続できたら true 返却.
        return true;
    }

    // 埋め込みベクトルを取得.
    // baseUrl: http://domain:port までのURLを設定します.
    // text: ベクトル変換対象の文字列を設定します.
    // 戻り値: ベクトル変換された double[] が返却されます.
    public static final double[] getEmbedding(String baseUrl, String text) {
        Map<String, Object> body = new HashMap<String,Object>();
        body.put("content", text);
        Object data = fetch(baseUrl, "embedding", body);
        // llama-server は { embedding: [...] } または [{ embedding: [...] }] を返す.
        Object res;
        if(data instanceof List) {
            // data[0].embedding
            res = Conv.getList(data).get(0);
            res = Conv.getMap(res).get("embedding");
        } else {
            // data.embedding
            res = Conv.getMap(data).get("embedding");
        }
        // List型じゃない場合.
        if(res == null || !(res instanceof List)) {
            throw new MRagException("Failed to get embedding: " + res);
        }
        // [[1,2,3,4,5.... ]] の場合.
        List list = Conv.getList(res);        
        if(list.get(0) instanceof List) {
            // list[0]
            list = Conv.getList(list.get(0));
        }
        // リスト情報をdouble[]変換.
        double[] ret;
        Object n;        
        int len = list.size();
        ret = new double[len];
        for(int i = 0; i < len; i ++) {
            ret[i] = Conv.getDouble(list.get(i));
        }
        return ret;
    }

    // 推論 (チャット補完)
    // baseUrl: http://domain:port までのURLを設定します.
    // userPrompt: ユーザ質問内容が設定されます.
    // systemPrompt: システム側が設定する質問補足内容が設定されます.
    // 戻り値: 回答が返却されます.
    public static final String chatCompletion(
        String baseUrl, String userPrompt, String systemPrompt) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("prompt", formatPrompt(systemPrompt, userPrompt));
        body.put("n_predict", 1024);
        body.put("temperature", 0.3);
        body.put("stop", STOP_LIST);
        Object data = fetch(baseUrl, "completion", body);
        return ((String)Conv.getMap(data).get("content")).trim();
    }

    // chatCompletion のPOSTパラメータ=stopにセットする内容.
    private static final List STOP_LIST;
    static {
        List<String> s = new ArrayList<String>();
        s.add("<|end|>");
        s.add("<|user|>");
        s.add("</s>");
        STOP_LIST = s;
    }

    // フォーマットプロンプトを作成.
    private static final String formatPrompt(String systemPrompt, String userPrompt) {
        StringBuilder ret = new StringBuilder();
        if(systemPrompt != null || systemPrompt.length() == 0) {
            ret.append("<|system|>\n").append(systemPrompt).append("<|end|>\n");
        }
        ret.append("<|user|>\n").append(userPrompt).append("<|end|>\n<|assistant|>\n");
        return ret.toString();
    }
}

