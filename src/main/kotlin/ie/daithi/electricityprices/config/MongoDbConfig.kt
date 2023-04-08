package ie.daithi.electricityprices.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoRepositories(basePackages = ["ie.daithi.electricityprices.repos"])
open class MongoDbConfig