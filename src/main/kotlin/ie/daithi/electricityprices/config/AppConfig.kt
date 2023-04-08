package ie.daithi.electricityprices.config

import ie.daithi.electricityprices.service.PriceService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.*
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.util.*

@Configuration
@ComponentScan(basePackages = ["ie.daithi.electricityprices"])
@EnableSwagger2
open class AppConfig(@Value("\${ree.url}") private val reeUrl: String) {

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

    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo())
    }

    private fun apiInfo(): ApiInfo {
        return ApiInfo( "Electricity Prices API",
                "API for Electricity Prices",
                "0.0.1",
                "Whatever",
                Contact("Daithi Hearn","https://github.com/daithihearn", "daithi.hearn@gmail.com"),
                "", "", Collections.emptyList())
    }


}