package com.kelvin.whoseturn.kafka

import com.codahale.metrics.{Gauge, MetricRegistry, SharedMetricRegistries}
import org.apache.kafka.common.MetricName
import org.apache.kafka.common.metrics.{KafkaMetric, MetricsReporter}

import java.util
import scala.jdk.CollectionConverters._

class KafkaMetricsReporter extends MetricsReporter{
  val registry: MetricRegistry = SharedMetricRegistries.getOrCreate("kafka-metrics")
  private val metricsPackageName = classOf[MetricsReporter].getPackage.getName

  override def init(metrics: util.List[KafkaMetric]): Unit = {
    metrics.asScala.foreach(metricChange)
  }

  override def metricChange(metric: KafkaMetric): Unit = {
    metricRemoval(metric)

    val qualifiedName = toQualifiedNameWithTags(metric.metricName())

    val gauge: Gauge[AnyRef] = () => metric.metricValue()
    registry.gauge(qualifiedName, () => gauge)

    ()
  }

  override def metricRemoval(metric: KafkaMetric): Unit = {
    registry.remove(toQualifiedNameWithTags(metric.metricName()))
    ()
  }

  override def configure(configs: util.Map[String, _]): Unit =
    ()

  override def close(): Unit =
    ()

  private def toQualifiedNameWithTags(metricName: MetricName): String = {

    val qualifiedName = toQualifiedName(metricName)

    metricName.tags() match {
      case t if t.isEmpty => qualifiedName
      case t              => s"$qualifiedName${toTagSuffix(t)}"
    }
  }

  private def toQualifiedName(metricName: MetricName): String = {
    s"$metricsPackageName.${metricName.group}.${metricName.name}"
  }

  private def toTagSuffix(tags: util.Map[String, String]): String = {
    tags.asScala.map(_.productIterator.mkString("=")).mkString(".(", ",", ")")
  }
}
