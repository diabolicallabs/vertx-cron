package integration_tests.groovy

import static org.vertx.testtools.VertxAssert.*
import org.vertx.groovy.testtools.VertxTests;

def testBadTimezone() {

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            address: 'rat',
            result_address: 'squid',
            action: 'send',
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('is not a valid timezone'))
        testComplete()
    }
}

VertxTests.initialize(this)

def config = [
        address_base: 'cron.test',
        timezone_name: 'USSR/Honolulu'
]

container.deployModule(System.getProperty("vertx.modulename"), config) { result ->

    if (!result.succeeded) {
       container.logger.error "Deployment failed with: ${result.cause()}"
    }

    assertTrue(result.succeeded)
    assertNotNull("deploymentID should not be null", result.result())

    VertxTests.startTests(this)
}



