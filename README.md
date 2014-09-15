# Cron Scheduler
This module allows you to schedule an event using a Cron specification. That event can be be
scheduled for a single execution or repeated periodically. It can either send or publish a message to the address of
your choice. If the handler of that message returns a response, you can specify where to send that response when
the handler completes.

## Name
The name of the module is vertx-cron.

## Configuration

    {
      "address_base": <string>
    }
    
The Cron Scheduler will use the string specified as the `<address_base>` for registering the handlers your
sender will interact with as well as some handlers used internally.

It will create public handlers for:
    
    <address_base>.schedule -- used to schedule an event
    <address_base>.cancel -- used to cancel a scheduled event
    
It will also create private handlers for:
    
    <address_base>.map.add -- to add the scheduler id and current timer id.
    <address_base>.map.remove -- to remove an unused scheduler id.
    
These private handlers allow the Cron Scheduler to transport shared information to Cron Scheduler verticals
running on separate nodes. *Note: At Diabolical Lab, vertical == verticle :-)*

## Configuration Example

    {
      "address_base": "cron.scheduler"
    }

This will cause the Cron Scheduler to create public handlers named: "cron.scheduler.schedule" and "cron.scheduler.cancel"
    


## Schedule an Event

To schedule an event, you need to send a message to this address: `<address_base>`.schedule where `<address_base>`
is the name specified in the configuration. Scheduled events are not persistent. If Vertx restarts, you will have to 
schedule your events again.

The message you send will conform to the following JSON schema:

    {
        "title": "A Cron Scheduler schedule message",
        "type":"object",
        "properties": {
            "cron_expression": {"type": "string"},
            "address": {"type": "string"},
            "message": {"type": "object"},
            "repeat": {"type": "boolean"},
            "action": {"enum": ["send", "publish"]},
            "result_address": {"type": "string"}
        },
        "required": ["cron_expression", "address"]
    }

**cron_expression** is a standard cron expression as is frequently used on Linux systems. As an example, "*/5 * * * * ?" would result in an event
being fired every 5 seconds. 

Most Cron implementation do not allow the scheduling of events down to the second, they will only allow minutely specifications. 
We are borrowing the org.quartz.CronExpression class from the Quartz Scheduler project that *does* allow the specification of seconds. 
Check this documentation [CronExpression] (http://quartz-scheduler.org/api/2.2.0/org/quartz/CronExpression.html) for allowed values.

This [CronMaker Calculator] (http://www.cronmaker.com/) may also help you form the cron_expression.

**address** is the address you want to send a message to on a scheduled basis.

**message** is the message that you want to send to the aforementioned address. It is not required. If it is not specified, the 
Cron Scheduler will send or publish a null message to the address at the time mentioned by the cron_expression.

**repeat** will instruct the Cron Scheduler to either send the message once at the specified time, or repeating it periodically.
It is not required. The default is true.

**action** is the action to take at the specified time. It can be "send" or "publish". If you specify "send" the Cron Scheduler
will send the message to the address and wait for any result. If you specify a result_address, the result of the send will be forwarded to 
that address. If you specify "publish" the message will be published to the address specified and the Cron Scheduler will not wait
to collect any result. The action is not required. If omitted, the default is "send".

**result_address** is the address to which you want any result sent. 

Here is an example schedule message:

    {
        "cron_expression": "0 0 16 1/1 * ? *",
        "address": "stock.quotes.list",
        "message": {
            "ticker": "RHT"
        },
        "repeat": true,
        "action": "send",
        "result_address": "stock.quotes.persist"
    }
  
This message would cause the Cron Scheduler to send the message {"ticker": "RHT"} to "stock.quotes.list" every day (including weekend days) at 16:00. The 
Cron Scheduler would then wait for a response from "stock.quotes.list" and forward the result to "stock.quotes.persist"

In Groovy it would look like this:

    def message = [
      cron_expression: '0 0 16 1/1 * ? *',
      address: "stock.quotes.list",
      message: [ticker: "RHT"],
      repeat: true,
      action: "send",
      result_address: "stock.quotes.persist"
    ]
    vertx.eventBus.send("cron.message.schedule", message) { result ->
      assert result.body.status == 'ok'
    }
    
    //This will be called every day at 16:00
    vertx.eventBus.registerHandler("stock.quotes.list") { message ->
      message.reply([close: 60.5])
    }
    //The Cron Scheduler will forward the reply from "stock.quotes.list" to this handler
    vertx.eventBus.registerHandler("stock.quotes.persist") { message ->
        // message.body == [close: 60.5] 
    }
  
## Scheduler Response

The `<address_base>`.schedule handler will return a message like the following when successful:

    {
        "status":"ok",
        "scheduler_id": "3857544d-cb68-4874-8ed8-82bee3713e3f",
        "next_run_time": "2001-09-11T13:46:30+0000"
    }
    
The `<address_base>`.schedule handler will return a message like the following when unsuccessful:

    {
        "status":"error",
        "message": "cron_expression must be specified"
    }

**status** can be "ok" or "error"

**message** is any informational message

**scheduler_id** is the string identifier of the schedule that the Cron Scheduler created

**next_run_time** is the string representing the time (in ISO format) that the next message will be sent.


## Cancel a Scheduled Event

To cancel a scheduled event you need to send a message to this address: `<address_base>`.cancel where `<address_base>`
is the name specified in the configuration.

The message will be the string returned by the `<address_base>`.schedule in the scheduler_id field.

In Groovy it would look like this:

    vertx.eventBus.send("cron.message.cancel", "3857544d-cb68-4874-8ed8-82bee3713e3f") { result ->
        assert result.body == 'ok'
    }
    
## Cancel Response

The `<address_base>`.cancel handler will return a message like the following when successful:

    {
        "status":"ok"
    }
    
The `<address_base>`.cancel handler will return a message like the following when unsuccessful:

    {
        "status":"error",
        "message": "The scheduler_id was not found"
    }

**status** can be "ok" or "error"

**message** is any informational message

## Additional Examples

## Dynamic message

If you need to send a message periodically, but some of the attributes of that message need to be 
determined at the scheduled time, you can do it like this:

    //this handler will return stock quotes for an exchange by date
    
    vertx.eventBus.registerHandler("stock.quotes.exchange.list") { message ->
      def exchange = message.body.exchange
      def date = message.body.date
      def quotes = StockQuoteService(exchange, date).getQuotes()
      message.reply(quotes)
    }
    
    //this handler will be called by the Cron scheduler.
    
    vertx.eventBus.registerHandler("stock.quotes.exchange.list.request") { message ->
        //the date below will be computed dynamically at the scheduled time
        def request = [
            exchange: 'NYSE',
            date: JsonOutput.toJson(new Date())
        ]
        vertx.eventBus.send("stock.quotes.exchange.list", request) { response ->
            def quotes = response.body
            //now save the quotes
            vertx.eventBus.send("stock.quotes.save", [quotes:quotes])
        }
    }
    
    //now schedule the event for 16:00 each day
    //there is no need to specify the result address since stock.quotes.exchange.list.request
    //will handle the result
    
    def message = [
      cron_expression: '0 0 16 1/1 * ? *',
      address: "stock.quotes.exchange.list.request",
      repeat: true,
    ]
    vertx.eventBus.send("cron.message.schedule", message) { result ->
      assert result.body.status == 'ok'
    }
    
## Publish message

If you wanted to clear a shared map each day at a particular time you can do it like this:

    //this will remove the shared map of the name passed on the message.
    //since in Vertx 2.x shared maps may exist on each vertx instance, we will need to publish 
    //to this address in order to clear all of them
    
    vertx.registerHandler("shared.map.clear") { message ->
        //message.body contains the map name
        vertx.sharedData.removeMap(message.body)
    }
    
    //schedule to publish a message to clear the map named "big.cache" each day at 23:00
    def message = [
      cron_expression: '0 0 23 1/1 * ? *',
      address: "shared.map.clear",
      message: "big.cache",
      repeat: true,
      action: "publish"
    ]
    vertx.eventBus.send("cron.message.schedule", message) { result ->
      assert result.body.status == 'ok'
    }
    
## Timezone issue

The date and time mentioned in the cron_expression are relative to the time of the server the Cron Scheduler vertical is
deployed to. If all vertx instances are in the same timezone, then there will not be any issue. If any instances
are configured to use other timezones, then the scheduler vertical running on that instance will fire events based
on that local timezone. 

A near future release will fix that problem. For now, we recommend setting all of your servers to use the same timezone
no matter where they are located. We set our servers to use UTC for this an a variety of other reasons not vertx related.