import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.conn.ssl.NoopHostnameVerifier;

@Configuration
public class CustomRestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Create a custom HttpClient that trusts all certificates and hostname verifier
        CloseableHttpClient httpClient = HttpClients.custom()
            .setSslcontext(SSLContexts.custom().loadTrustMaterial((chain, authType) -> true).build())
            .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(factory);
    }
}
