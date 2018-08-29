package com.thehecklers.coffeeservice

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit4.SpringRunner
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration

@RunWith(SpringRunner::class)
@WebFluxTest(CoffeeService::class)
class CoffeeServiceApplicationTests() {
    @Autowired
    lateinit var service: CoffeeService

    @MockBean
    lateinit var repo: CoffeeRepo

    lateinit var coffee1: Coffee
    lateinit var coffee2: Coffee

    @Before
    fun setup() {
        coffee1 = Coffee("000-TEST-111", "Tester's Choice")
        coffee2 = Coffee("000-TEST-222", "Maxfail House")

        Mockito.`when`(repo.findAll()).thenReturn(Flux.just(coffee1, coffee2))
        Mockito.`when`(repo.findById(coffee1.id)).thenReturn(Mono.just(coffee1))
        Mockito.`when`(repo.findById(coffee2.id)).thenReturn(Mono.just(coffee2))
    }

    @Test
    fun `Get Coffee Orders, Take 10, verify` () {
        StepVerifier.withVirtualTime { service.getOrdersForCoffee(coffee1.id).take(10) }
                .thenAwait(Duration.ofHours(10))
                .expectNextCount(10)
                .verifyComplete()
    }

}
