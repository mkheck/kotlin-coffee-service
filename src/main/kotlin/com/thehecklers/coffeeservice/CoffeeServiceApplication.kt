package com.thehecklers.coffeeservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.annotation.PostConstruct

@SpringBootApplication
class CoffeeServiceApplication

fun main(args: Array<String>) {
    runApplication<CoffeeServiceApplication>(*args)
}

@Configuration
class RouterConfig(private val service: CoffeeService) {
    @Bean
    fun routerFunction() = router {
        listOf(GET("/coffees", ::all),
                GET("/coffees/{id}", ::byId),
                GET("/coffees/{id}/orders", ::orders))
    }

    fun all(req: ServerRequest) = ServerResponse.ok()
            .body<Coffee>(service.getAllCoffees())

    fun byId(req: ServerRequest) = ServerResponse.ok()
            .body<Coffee>(service.getCoffeeById(req.pathVariable("id")))

    fun orders(req: ServerRequest) = ServerResponse.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body<CoffeeOrder>(service.getOrdersForCoffee(req.pathVariable("id")))
}

//@RestController
//@RequestMapping("/coffees")
//class CoffeeController(private val service: CoffeeService) {
//    @GetMapping
//    fun all() = service.getAllCoffees()
//
//    @GetMapping("/{id}")
//    fun byId(@PathVariable id: String) = service.getCoffeeById(id)
//
//    @GetMapping("/{id}/orders", produces = arrayOf(MediaType.TEXT_EVENT_STREAM_VALUE))
//    fun orders(@PathVariable id: String) = service.getOrdersForCoffee(id)
//}

@Service
class CoffeeService(private val repo: CoffeeRepo) {
    fun getAllCoffees(): Flux<Coffee> = repo.findAll()

    fun getCoffeeById(id: String) = repo.findById(id)

    fun getOrdersForCoffee(coffeeId: String) = Flux.generate<CoffeeOrder> { it.next(CoffeeOrder(coffeeId, Instant.now())) }
            .delayElements(Duration.ofSeconds(1))
}

@Component
class DataLoader(private val repo: CoffeeRepo) {
    @PostConstruct
    private fun load() {
        repo.deleteAll().thenMany(
                Flux.just("Kaldi's Coffee", "Esmeralda Especial", "Sol y Café")
                        .map { Coffee(UUID.randomUUID().toString(), it) }
                        .flatMap { repo.save(it) })
                .thenMany(repo.findAll())
                .subscribe { println(it) }
    }
}

interface CoffeeRepo : ReactiveCrudRepository<Coffee, String>

class CoffeeOrder(val coffeeId: String, val whenOrdered: Instant)

@Table
data class Coffee(@PrimaryKey val id: String, val name: String)