package com.windschief.client;

import java.io.IOException;

public interface HttpClientService {
    <T> T get(String url, String bearerToken, Class<T> responseType) throws IOException, InterruptedException;
}
