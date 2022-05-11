package com.kelvin.whoseturn.kafka.producer

import com.codahale.metrics.MetricRegistry
import com.kelvin.whoseturn.kafka.messages.TodoItemUserActionEvent

trait TodoItemKafkaProducerMetrics {
  def incrementTodoItemEventProducedEvent(
      action: TodoItemUserActionEvent
  )(implicit metricRegistry: MetricRegistry): Unit = {
    val metricName = s"producer.todoevents[type=${action.action},source=${action.source}"
    metricRegistry.meter(metricName).mark()
  }

  def incrementTodoItemEventProducerError()(implicit metricRegistry: MetricRegistry): Unit = {
    val metricName = s"producer.todoevents.errors"
    metricRegistry.meter(metricName).mark()
  }
}
