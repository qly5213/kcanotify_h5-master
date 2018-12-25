package com.antest1.kcanotify.h5;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface KcaDownloader {
    @Headers({
            "Accept: application/json",
            "X-Identify: app/kcanotify",
            "Referer: app:/KCA/",
            "Cache-control: no-cache, no-store, must-revalidate",
            "Content-Type: application/x-www-form-urlencoded"
    })
    @GET("/kcanotify/v.php")
    Call<String> getRecentVersion();

    @Headers({
            "Accept: application/json",
            "X-Identify: app/kcanotify",
            "Referer: app:/KCA/",
            "Content-Type: application/x-www-form-urlencoded"
    })
    @GET("/kcanotify/list.php")
    Call<String> getResourceList();

    @Headers({
            "Accept: application/octet-stream",
            "X-Identify: app/kcanotify",
            "Referer: app:/KCA/",
            "Content-Type: application/x-www-form-urlencoded"
    })
    @GET("/kcanotify/kca_api_start2.php?")
    Call<String> getGameData(@Query("v") String v);


    @Headers({
            "Accept: application/json",
            "X-Identify: app/kcanotify",
            "Referer: app:/KCA/",
            "Cache-control: no-cache, no-store, must-revalidate",
            "Content-Type: application/x-www-form-urlencoded"
    })
    @GET("/kcanotify/version.json")
    Call<String> getH5RecentVersion();


    @Headers({
            "Accept: text/html",
            "X-Identify: app/kcanotify",
            "Referer: app:/KCA/",
            "Cache-control: no-cache, no-store, must-revalidate",
            "Content-Type: application/x-www-form-urlencoded"
    })
    @GET("/kcanotify/main-{local}.html")
    Call<String> getH5MainHtml(@Path("local") String local);
}
