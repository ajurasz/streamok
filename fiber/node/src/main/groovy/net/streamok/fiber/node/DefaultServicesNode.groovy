/**
 * Licensed to the Streamok under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.streamok.fiber.node

import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import net.streamok.fiber.node.api.*
import net.streamok.fiber.node.vertx.VertxOperationContext

import static java.lang.System.currentTimeMillis
import static java.util.UUID.randomUUID
import static org.apache.commons.lang3.SystemUtils.javaIoTmpDir

class DefaultServicesNode implements ServicesNode {

    private final def Vertx vertx

    private final id = randomUUID().toString()

    def dependencies = [:]

    DefaultServicesNode() {
        System.setProperty('vertx.cacheDirBase', javaIoTmpDir.absolutePath)
        vertx = Vertx.vertx()
    }

    ServicesNode start() {
        vertx.eventBus().send('metrics.put', null, new DeliveryOptions().addHeader('key', "fiber.node.${id}.started").addHeader('value', "${currentTimeMillis()}"))
        this
    }

    @Override
    ServicesNode close() {
        vertx.close()
        this
    }

    @Override
    String id() {
        id
    }

    DefaultServicesNode addFiber(OperationDefinition fiberDefinition) {
        vertx.eventBus().consumer(fiberDefinition.address()) {
            try {
                fiberDefinition.handler().handle(new VertxOperationContext(it, this))
            } catch (Exception e) {
                it.fail(100, e.message)
            }
        }
        this
    }

    DefaultServicesNode addEndpoint(Endpoint endpoint) {
        endpoint.connect(this)
        this
    }

    DefaultServicesNode addSuite(Service fiberSuite) {
        if(fiberSuite instanceof FiberNodeAware) {
            fiberSuite.fiberNode(this)
        }
        fiberSuite.dependencies().each { addDependency(it) }
        fiberSuite.operations().each { addFiber(it) }
        fiberSuite.endpoints().each { addEndpoint(it) }
        this
    }

    // Dependency injection

    DefaultServicesNode addDependency(DependencyProvider dependencyProvider) {
        dependencies[dependencyProvider.key()] = dependencyProvider.dependency()
        this
    }

    Object dependency(String key) {
        dependencies[key]
    }

    def <T> T dependency(Class<T> type) {
        dependencies.values().find { type.isAssignableFrom(it.getClass()) }
    }

    // Getters

    Vertx vertx() {
        vertx
    }

}