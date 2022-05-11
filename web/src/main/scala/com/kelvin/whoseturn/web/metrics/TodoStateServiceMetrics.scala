package com.kelvin.whoseturn.web.metrics

import com.codahale.metrics.MetricRegistry

trait TodoStateServiceMetrics {
  def incrementTodoItemCreatedCounter()(implicit metricRegistry: MetricRegistry): Unit = {
    val metricName = "service.todostate.create"
    metricRegistry.meter(metricName).mark()
  }
}
