package org.ebyhr.trino.storage.operator;

import okhttp3.*;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.io.IOException;

public class GPTApiHelper {

    final static  String GPT_API_KEY = "GPT.OPENAI.KEY";
    // Define the Retrofit interface for the OpenAI chatcompletion endpoint
    public interface OpenAIChatService {
        @Headers({"Content-Type: application/json"})
        @POST("v1/completions")
        Call<ResponseBody> chatCompletion(@Body ChatRequest chatRequest);
    }

    // Define the request body for the OpenAI completion endpoint
    public static class ChatRequest {
        String prompt;
        String model;
        int max_tokens;

        public ChatRequest(String model,String prompt, int max_tokens) {
            this.prompt = prompt;
            this.model = model;
            this.max_tokens = max_tokens;
        }
    }

    public static String getCSVFileFromGPT(String userPrompt) throws IOException {
        // Get the OpenAI API token from an environment variable
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

        // Create an instance of the OpenAI chatcompletion service interface
        OpenAIChatService openAIChatService = retrofit.create(OpenAIChatService.class);
        String SystemPrompt = "you are data bot, who will provide data based on user description , you will try your maximum to understand user response and " +
                "produce csv string output, you strictly outputs csv string output,you dont know anything else, you reply is only the csv string output." +
                " if you are not able to frame a tabular structure for user response output empty string. user: ";
        // Create a chat request with a prompt and max_tokens
        ChatRequest chatRequest = new ChatRequest("text-davinci-003",
                SystemPrompt + userPrompt + "your csv goes here ...", 250);

        // Call the OpenAI chatcompletion endpoint with the chat request
        Call<ResponseBody> call = openAIChatService.chatCompletion(chatRequest);
        retrofit2.Response<ResponseBody> responseBody = call.execute();
        System.out.println(responseBody.code());
        if (responseBody.isSuccessful()){
            assert responseBody.body() != null;
            String responseString = responseBody.body().string();
            System.out.println(responseString);
            return responseString;
        }
        else{
            System.out.println(responseBody.errorBody().string());
            return null;
        }

    }

    public static void main(String[] args) throws IOException {

    }
}
