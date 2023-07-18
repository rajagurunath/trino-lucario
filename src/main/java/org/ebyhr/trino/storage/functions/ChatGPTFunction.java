package org.ebyhr.trino.storage.functions;

import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.trino.hive.$internal.org.json.JSONObject;
import io.trino.spi.function.Description;
import io.trino.spi.function.ScalarFunction;
import io.trino.spi.function.SqlNullable;
import io.trino.spi.function.SqlType;
import io.trino.spi.type.StandardTypes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import okhttp3.*;
import org.ebyhr.trino.storage.operator.GPTApiHelper;
import org.ebyhr.trino.storage.operator.GPTApiHelper.*;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public final class ChatGPTFunction {
    public interface OpenAIChatService {
        @Headers({"Content-Type: application/json"})
        @POST("v1/completions")
        Call<ResponseBody> chatCompletion(@Body ChatRequest chatRequest);
    }
    final static  String GPT_API_KEY = "GPT.OPENAI.KEY";
    public static final String NAME = "ask_chatgpt";

    @ScalarFunction(value=NAME, deterministic = false)
    @Description("generic ChatGPT function caller which can be controlled by passing relevant prompts")
    @SqlType(StandardTypes.VARCHAR)
    public static Slice ask_chatgpt(
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice SystemPrompt,
            @SqlNullable @SqlType(StandardTypes.VARCHAR) Slice userPrompt) throws IOException {

        String res  = callGPT(SystemPrompt,userPrompt,200);
        return Slices.utf8Slice(res);
    }

    public static String callGPT(Slice SystemPrompt,Slice userPrompt,int maxTokens) throws IOException {
        ChatRequest chatRequest = new ChatRequest("text-davinci-003",
                SystemPrompt.toStringUtf8() + userPrompt.toStringUtf8() , maxTokens);
        String openaiToken = System.getenv(GPT_API_KEY);

        // Create an OkHttpClient with an interceptor to add the authorization header
        OkHttpClient okHttpClient = new OkHttpClient.Builder()//.callTimeout(Duration.ofMinutes(0)).connectTimeout(Duration.ofMinutes(0))
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        Request requestWithAuth = originalRequest.newBuilder()
                                .header("Authorization", "Bearer " + openaiToken)
                                .build();
                        return chain.proceed(requestWithAuth);
                    }
                })
                .build();
        // Create a Retrofit instance with the GsonConverterFactory and OkHttpClient
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        // Call the OpenAI chatcompletion endpoint with the chat request
        GPTApiHelper.OpenAIChatService openAIChatService = retrofit.create(GPTApiHelper.OpenAIChatService.class);
        Call<ResponseBody> call = openAIChatService.chatCompletion(chatRequest);
        retrofit2.Response<ResponseBody> responseBody = call.execute();

        System.out.println(responseBody.code());
        if (responseBody.isSuccessful()){
            assert responseBody.body() != null;
            String responseString = responseBody.body().string();
            JSONObject responseJson = new JSONObject(responseString);
            String finalString = responseJson.getJSONArray("choices").getJSONObject(0).getString("text");
            System.out.println(finalString);
            return finalString.strip();
        }
        else{
            System.out.println(responseBody.errorBody().string());
            return null;
        }

    }

}
