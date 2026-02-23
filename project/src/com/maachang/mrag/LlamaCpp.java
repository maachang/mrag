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
    private static final Object fetch(
        String baseUrl, String endpoint, Object body) {
        return fetch(false, baseUrl, endpoint, body);
    }

    // llama.cppの server にPOSTでアクセス.
    // noResultJson: true の場合、JSON返却を行いません.
    // baseUrl: http://domain:port までのURLを設定します.
    // endpoint: path/.../key を設定します.
    // body: POST送信対象のJSON情報を設定します.
    // 戻り値: JSON結果が返却されます.
    //         また noResultJson=true の場合は文字列が返却されます.
    private static final Object fetch(
        boolean noResultJson, String baseUrl, String endpoint, Object body) {
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
            // JSONではなく文字列返却.
            if(noResultJson == true) {
                return resBody;
            }
            // JSON返却.
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
    // 戻り値: ベクトル変換された float[] が返却されます.
    public static final float[] getEmbedding(String baseUrl, String text) {
        // body-jsonをセット.
        Map<String, Object> body = new HashMap<String,Object>();
        body.put("model", "embeddinggemma");
        body.put("input", text);

        // v1/embeddings を利用.
        Object result = fetch(baseUrl, "v1/embeddings", body);
        // result = data[0].embedding[...];
        Object res;
        res = Conv.getMap(result).get("data");
        res = Conv.getList(res).get(0);
        res = Conv.getMap(res).get("embedding");
        List list = Conv.getList(res); 
        res = null;

        // リスト情報を float[]変換.
        float[] ret;
        Object n;        
        int len = list.size();
        ret = new float[len];
        for(int i = 0; i < len; i ++) {
            ret[i] = Conv.getFloat(list.get(i));
        }
        return ret;
    }


    // 推論 (チャット補完)
    // baseUrl: http://domain:port までのURLを設定します.
    // prompt: 質問内容が設定されます.
    // 戻り値: /v1/chat/completions のJSON結果が返却されます.
    public static final Object getChatCompletions(String baseUrl, String prompt) {
        return getChatCompletions(baseUrl, prompt, -1f, -1);
    }

    // 推論 (チャット補完)
    // baseUrl: http://domain:port までのURLを設定します.
    // prompt: 質問内容が設定されます.
    // temperature Temperatureパラメータ値(0に近いほど正確性)の値を設定します.
    //             ・ 0.1 - 0.3: 正確性重視（事実・指示）
    //             ・ 0.7 - 0.8: バランス重視（対話）
    //             ・ 1.0 - 1.2: 創造性重視（物語・創作）
    // maxTokens: 返却トークン値を設定します.
    // 戻り値: /v1/chat/completions のJSON結果が返却されます.
    public static final Object getChatCompletions(
        String baseUrl, String prompt, float temperature, int maxTokens) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("messages", Conv.newList(Conv.newMap("role", "user", "content", prompt)));
        if(temperature > 0) {
            body.put("temperature", temperature);
        } else {
            body.put("temperature", 0.3f);
        }
        if(maxTokens > 0) {
            body.put("max_tokens", maxTokens);
        }
        return fetch(baseUrl, "v1/chat/completions", body);
    }

    // getChatCompletions 返却値からMessage.conentを取得.
    // json: getChatCompletions で返却された内容を設定します.
    // 戻り値: Message.contentが返却されます.
    public static final String getResultChatCompletionsToText(Object json) {
        // getChatCompletions返却結果のjson解析して、有効なメッセージを返却.
        // (以下のJSONが返却される)
        // {created,
        //   usage:{completion_tokens, prompt_tokens, total_tokens},
        //   timings: {cache_n, predicted_ms, predicted_per_second, prompt_per_token_ms, prompt_n,
        //       prompt_ms, prompt_per_second, predicted_n, predicted_per_token_ms},
        //   model, id,
        //   choices: [{finish_reason, index=0, message: {role, content: test}}]}

        // 上ｎ内容から
        // {choices: [0].message.content}
        // これを取得する.
        Map top = Conv.getMap(json);
        List list = Conv.getList(top.get("choices"));
        Map choicesTop = Conv.getMap(list.get(0));
        Map message = Conv.getMap(choicesTop.get("message"));
        return Conv.getString(message.get("content"));
    }

    // 推論 (チャット補完)メッセージだけを取得.
    // baseUrl: http://domain:port までのURLを設定します.
    // prompt: 質問内容が設定されます.
    // 戻り値: String メッセージが返却されます.
    public static final String getChatMessage(String baseUrl, String prompt) {
        return getChatMessage(baseUrl, prompt, -1f, -1);
    }

    // 推論 (チャット補完)メッセージだけを取得.
    // baseUrl: http://domain:port までのURLを設定します.
    // prompt: 質問内容が設定されます.
    // temperature Temperatureパラメータ値(0に近いほど正確性)の値を設定します.
    //             ・ 0.1 - 0.3: 正確性重視（事実・指示）
    //             ・ 0.7 - 0.8: バランス重視（対話）
    //             ・ 1.0 - 1.2: 創造性重視（物語・創作）
    // maxTokens: 返却トークン値を設定します.
    // 戻り値: String メッセージが返却されます.
    public static final String getChatMessage(
        String baseUrl, String prompt, float temperature, int maxTokens) {
        Object res = getChatCompletions(baseUrl, prompt, temperature, maxTokens);
        return getResultChatCompletionsToText(res);
    } 

    // これは後で実装＋テストする.
    /*
    // [リアルタイム取得版]推論 (チャット補完)
    // baseUrl: http://domain:port までのURLを設定します.
    // prompt: 質問内容が設定されます.
    // temperature Temperatureパラメータ値(0に近いほど正確性)の値を設定します.
    //             ・ 0.1 - 0.3: 正確性重視（事実・指示）
    //             ・ 0.7 - 0.8: バランス重視（対話）
    //             ・ 1.0 - 1.2: 創造性重視（物語・創作）
    // maxTokens: 返却トークン値を設定します.
    // 戻り値: /v1/chat/completions のJSON結果が返却されます.
    public void getChatCompletionsToStream(
        Reader out, String baseUrl, String prompt, float temperature, int maxTokens) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("messages", Conv.newList(Conv.newMap("role", "user", "content", prompt)));
        body.put("temperature", 0.3);
        body.put("stream", true);
        if(temperature > 0) {
            body.put("temperature", temperature);
        } else {
            body.put("temperature", 0.3f);
        }
        if(maxTokens > 0) {
            body.put("max_tokens", maxTokens);
        }
        // リクエストをセット.
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(Config.LLM_URL + "/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(Json.encode(body)))
            .build();

        // InputStream でストリーミング受信
        HttpResponse<InputStream> res = HTTPCLIENT.send(
            req, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() != 200) {
            throw new MRagException(res.statusCode(), "llama.cppエラー");
        }

        // レスポンスオブジェクトを返却.
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(res.body(), "UTF8"))

        // ここから下は何か修正が必要.
        try (var reader = new BufferedReader(
                new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) continue;
                var json = line.substring(6).trim();
                if (json.equals("[DONE]")) break;
                try {
                    var tok = om.readTree(json)
                            .at("/choices/0/delta/content")
                            .asText("");
                    System.out.print(tok);
                    System.out.flush();
                } catch (Exception ignored) {}
            }
        }
    }
    */
}

