import static org.vertx.testtools.VertxAssert.*
import org.vertx.groovy.testtools.VertxTests;

def test5SecondNoRepeat() {

    def scheduled_address = "address_${new Date().format('yyyyMMddhhmmssSSS')}".toString()

    vertx.eventBus.registerHandler(scheduled_address) { message ->
        container.logger.info "Handler message: ${message.body}"
        assertEquals('goose', message.body)
        message.reply(null)
        testComplete()
    }

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            address: scheduled_address,
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('ok', result.body.status)
        assertNotNull(result.body.scheduler_id)
        assertNotNull(result.body.next_run_time)
    }
}
def testInvalidCron() {

    def event = [
            cron_expression: '*/5 squid * * * ?',
            repeat: false,
            address: 'none',
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('Illegal characters'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testCronMissing() {

    def event = [
            repeat: true,
            address: 'none',
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('cron_expression must be specified'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testCronBadType() {

    def event = [
            cron_expression: 5,
            repeat: false,
            address: 'none',
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('cron_expression must be a string'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testInvalidRepeat() {

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: 'rat',
            address: 'none',
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('repeat must be true or false'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testMissingAddress() {

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('address must be specified'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testAddressInvalidType() {

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            address: 777,
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('address must be a string'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testResultAddressInvalidType() {

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            address: 'rat',
            result_address: 88,
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('result_address must be a string'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testActionInvalidType() {

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            address: 'rat',
            result_address: 'squid',
            action: 23,
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('action must be a string'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}
def testActionInvalidValue() {

    def event = [
            cron_expression: '*/5 * * * * ?',
            repeat: false,
            address: 'rat',
            result_address: 'squid',
            action: 'cry',
            message: 'goose'
    ]

    container.logger.info "about to send ${event} to cron.test.create"

    vertx.eventBus.send('cron.test.schedule', event) { result ->
        container.logger.info "create result: ${result.body}"
        assertEquals('error', result.body.status)
        assertTrue(result.body.message.contains('action must be "send" or "publish"'))
        assertNull(result.body.scheduler_id)
        assertNull(result.body.next_run_time)
        testComplete()
    }
}

VertxTests.initialize(this)

def config = [
        address_base: 'cron.test'
]

container.deployModule(System.getProperty("vertx.modulename"), config) { asyncResult ->

    if (!asyncResult.succeeded) {
       container.logger.error "Deployment failed with: ${asyncResult.cause()}"
    }

    assertTrue(asyncResult.succeeded)
    assertNotNull("deploymentID should not be null", asyncResult.result())

    VertxTests.startTests(this)
}



