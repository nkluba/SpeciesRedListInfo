package com.example.speciesredlistinfo;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DataFetcher {

    private static final String BASE_URL = "https://apiv3.iucnredlist.org/api/v3/";
    private static final String API_TOKEN = BuildConfig.API_TOKEN;

    public static String fetchSpeciesInfo(String speciesName) {
        OkHttpClient client = new OkHttpClient();

        String url = BASE_URL + "species/narrative/" + speciesName + "?token=" + API_TOKEN;

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new RuntimeException("Failed: " + response.code());
            return response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}