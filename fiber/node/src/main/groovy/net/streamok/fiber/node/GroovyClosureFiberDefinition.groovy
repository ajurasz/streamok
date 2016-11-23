package net.streamok.fiber.node

import net.streamok.fiber.node.api.Fiber
import net.streamok.fiber.node.api.FiberDefinition

class GroovyClosureFiberDefinition implements FiberDefinition {

    String address

    Closure closure

    GroovyClosureFiberDefinition(String address, Closure closure) {
        this.address = address
        this.closure = closure
    }

    static GroovyClosureFiberDefinition groovyClosureFiberDefinition(String address, String definition) {
        def closure = new GroovyShell().evaluate(definition) as Closure
        new GroovyClosureFiberDefinition(address, closure)
    }

    @Override
    String address() {
        address
    }

    @Override
    Fiber handler() {
        closure
    }

}