package net.streamok.distribution.chaosmonkey

import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import io.vertx.core.json.Json
import net.streamok.lib.vertx.Blocking
import org.apache.commons.lang3.Validate

import java.util.concurrent.CountDownLatch

import static java.util.concurrent.TimeUnit.SECONDS
import static net.streamok.lib.vertx.Blocking.block
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import static org.assertj.core.api.Assertions.assertThat

class ChaosMonkey {

    private final Vertx vertx

    private final HttpClient http

    ChaosMonkey(Vertx vertx) {
        this.vertx = vertx
        this.http = vertx.createHttpClient()
    }

    ChaosMonkey() {
        this(Vertx.vertx())
    }

    void run() {
        Thread.sleep(6000)

        checkConfigurationServiceApiHeartbeat()
        accessConfigurationServiceApi()

        // Document service
        checkDocumentServiceHeartbeat()
        accessDocumentServiceSaveOperation()
        accessDocumentServiceCountOperation()
        checkDocumentServiceCountMetric()

        // Machine learning service
        triggerTrainingDataIngestionFromTweeter()
        trainTextLabelModel()
    }

    private void checkConfigurationServiceApiHeartbeat() {
        def latch = new CountDownLatch(1)
        vertx.createHttpClient().getNow(8080, 'localhost', "/metrics/get?key=service.configuration.heartbeat") {
            it.bodyHandler {
                assertThat(it.toString().toLong()).isGreaterThan(0L)
                latch.countDown()
            }
        }
        Validate.isTrue(latch.await(15, SECONDS), "Configuration service doesn't send heartbeats..")
    }

    private void accessConfigurationServiceApi() {
        def latch = new CountDownLatch(1)
        def key = randomAlphanumeric(20)
        def value = randomAlphanumeric(20)
        vertx.createHttpClient().getNow(8080, 'localhost', "/configuration/write?key=${key}&value=${value}") {
            vertx.createHttpClient().getNow(8080, 'localhost', "/configuration/read?key=${key}") {
                it.bodyHandler {
                    assertThat(it.toString()).isEqualTo(value)
                    latch.countDown()
                }
            }
        }
        Validate.isTrue(latch.await(15, SECONDS), 'Configuration service API not available.')
    }

    // Document service

    private void checkDocumentServiceHeartbeat() {
        def latch = new CountDownLatch(1)
        vertx.createHttpClient().getNow(8080, 'localhost', "/metrics/get?key=service.document.heartbeat") {
            it.bodyHandler {
                assertThat(it.toString().toLong()).isGreaterThan(0L)
                latch.countDown()
            }
        }
        Validate.isTrue(latch.await(5, SECONDS), 'Cannot read document service heartbeat.')
    }

    private void checkDocumentServiceCountMetric() {
        def latch = new CountDownLatch(1)
        vertx.createHttpClient().getNow(8080, 'localhost', "/metrics/get?key=service.document.count") {
            it.bodyHandler {
                assertThat(it.toString().toLong()).isGreaterThanOrEqualTo(0L)
                latch.countDown()
            }
        }
        Validate.isTrue(latch.await(30, SECONDS), 'Cannot read document service documents count metric.')
    }

    private void accessDocumentServiceCountOperation() {
        def latch = new CountDownLatch(1)
        def collection = randomAlphanumeric(20)
        vertx.createHttpClient().getNow(8080, 'localhost', "/document/count?collection=${collection}") {
            it.bodyHandler {
                assertThat(it.toString().toLong()).isGreaterThanOrEqualTo(0L)
                latch.countDown()
            }
        }
        Validate.isTrue(latch.await(15, SECONDS), "Can't invoke document count operation.")
    }

    private void accessDocumentServiceSaveOperation() {
        def latch = new CountDownLatch(1)
        def collection = randomAlphanumeric(20)
        vertx.createHttpClient().post(8080, 'localhost', "/document/save?collection=${collection}") {
            assertThat(it.toString()).isNotNull()
            latch.countDown()
        }.end(Json.encode([foo: 'bar']))
        Validate.isTrue(latch.await(5, SECONDS))
    }

    // Machine learning service

    private void triggerTrainingDataIngestionFromTweeter() {
        block { semaphore ->
            def collection = randomAlphanumeric(20)
            http.getNow(8080, 'localhost', "/machineLearning/ingestTrainingData?collection=${collection}&source=twitter:iot") {
                http.getNow(8080, 'localhost', "/document/count?collection=training_texts_${collection}") {
                    it.bodyHandler {
                        assertThat(it.toString().toLong()).isGreaterThan(100L)
                        semaphore.countDown()
                    }
                }
            }
        }
    }

    private void trainTextLabelModel() {
        block { semaphore ->
            def collection = randomAlphanumeric(20)
            http.getNow(8080, 'localhost', "/machineLearning/ingestTrainingData?collection=${collection}&source=twitter:iot") {
                http.getNow(8080, 'localhost', "/document/count?collection=training_texts_${collection}") {
                    it.bodyHandler {
                        http.getNow(8080, 'localhost', "/machineLearning/trainTextLabelModel?dataset=${collection}") {
                            it.bodyHandler {
                                def response = it.toString()
                                assertThat(response).isEqualTo('null')
                                semaphore.countDown()
                            }
                        }
                    }
                }
            }
        }
    }

}