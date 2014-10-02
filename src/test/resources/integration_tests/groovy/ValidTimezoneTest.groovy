package integration_tests.groovy

import static org.vertx.testtools.VertxAssert.*
import org.vertx.groovy.testtools.VertxTests;

def testTimezone() {

    def calendar = new GregorianCalendar()

    calendar.setTimeZone(TimeZone.getTimeZone('Pacific/Midway'))

    def hour = calendar.get(Calendar.HOUR_OF_DAY)
    def minute = calendar.get(Calendar.MINUTE)
    def second = calendar.get(Calendar.SECOND) + 3

    def expression = "$second $minute $hour * * ?"

    def event = [
            cron_expression: expression.toString(),
            repeat: false,
            address: 'rat',
            action: 'send',
            message: 'goose'
    ]

    vertx.eventBus.registerHandler("rat") { message ->
        assertEquals('goose', message.body)
        testComplete()
    }

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('ok', result.body.status)
    }
}

VertxTests.initialize(this)

def config = [
        address_base: 'cron.test',
        timezone_name: 'Pacific/Midway'
]

container.deployModule(System.getProperty("vertx.modulename"), config) { result ->

    if (!result.succeeded) {
       container.logger.error "Deployment failed with: ${result.cause()}"
    }

    assertTrue(result.succeeded)
    assertNotNull("deploymentID should not be null", result.result())

    VertxTests.startTests(this)
}



