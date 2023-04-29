package ie.daithi.electricityprices.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Configuration
open class AppConfig(@Value("\${ree.url}") private val reeUrl: String, @Value("\${esios.url}") private val esiosUrl: String) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    @Bean
    open fun esiosRest(): WebClient {
        logger.info("Creating esiosRest bean with url: $esiosUrl")

        val size = 16 * 1024 * 1024
        val strategies = ExchangeStrategies.builder()
            .codecs { codecs: ClientCodecConfigurer ->
                codecs.defaultCodecs().maxInMemorySize(size)
            }
            .build()
        return WebClient.builder()
            .exchangeStrategies(strategies)
            .baseUrl(esiosUrl)
            .build()
    }

    @Bean
    open fun reeRest(): WebClient {
        val size = 16 * 1024 * 1024
        val strategies = ExchangeStrategies.builder()
            .codecs { codecs: ClientCodecConfigurer ->
                codecs.defaultCodecs().maxInMemorySize(size)
            }
            .build()
        return WebClient.builder()
            .exchangeStrategies(strategies)
            .baseUrl(reeUrl)
            .build()
    }

}