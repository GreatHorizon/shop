package com.example.main.configuration;

import com.example.payment.client.api.BalanceApi;
import com.example.payment.client.api.PayApi;
import com.example.payment.client.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PaymentClientConfig {
    @Bean
    public ApiClient paymentApiClient(@Value("${payment.base-url}") String baseUrl, WebClient paymentWebClient) {
        final var client = new ApiClient(paymentWebClient);

        client.setBasePath(baseUrl);

        return client;
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrations,
            ServerOAuth2AuthorizedClientRepository authorizedClients) {
        var authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();
        var authorizedClientManager = new DefaultReactiveOAuth2AuthorizedClientManager(
                clientRegistrations, authorizedClients);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    @Bean
    public WebClient webClient(@Value("${payment.base-url}") String baseUrl, ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        // Создаём функцию-фильтр для WebClient, которая будет автоматически
        // запрашивать и прикреплять OAuth2-токены к каждому HTTP-запросу
        var oauth2Client = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        // Указываем ID регистрации OAuth2-клиента по умолчанию (должен совпадать с именем в application.yml)
        oauth2Client.setDefaultClientRegistrationId("keycloak");
        return WebClient.builder()
                // Добавляем OAuth2-авторизацию ко всем запросам
                .filter(oauth2Client)
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
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