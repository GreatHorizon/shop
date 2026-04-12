package com.example.main.configuration;

import com.example.payment.client.api.BalanceApi;
import com.example.payment.client.api.PayApi;
import com.example.payment.client.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentClientConfig {

    @Bean
    public ApiClient paymentApiClient(@Value("${payment.base-url}") String baseUrl) {
        ApiClient apiClient = new ApiClient();

        apiClient.setBasePath(baseUrl);

        return apiClient;
    }

    @Bean
    public PayApi paymentApi(ApiClient paymentApiClient) {
        return new PayApi(paymentApiClient);
    }

    @Bean
    public BalanceApi balanceApi(ApiClient paymentApiClient) {
        return new BalanceApi(paymentApiClient);
    }
}