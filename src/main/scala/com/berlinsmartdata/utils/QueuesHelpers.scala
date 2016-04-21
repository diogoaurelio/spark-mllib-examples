package com.berlinsmartdata.utils

import org.apache.spark.streaming.StreamingContext


trait QueuesHelpers {

  val conf = JobConfig.getConfiguration

  def queueStream(ssc: StreamingContext, queueType:String, checkpoint:String) = {
    val params = getQueueParams(queueType, checkpoint)
    val queue = queueType.toLowerCase()
    val eventStream = if(queue == "microsoft") {
      EventHubsUtils.createUnionStream(ssc, params)
    }
  }

  def getQueueParams(queueType: String, checkpoint: String): Map[String, String] = {
    val queue = queueType.toLowerCase()
    val params = if(queue == "microsoft") {
      /* Microsoft eventHub */
      Map[String, String](
        "eventhubs.namespace" -> conf.getString("eventhubs.namespace"),
        "eventhubs.name" -> conf.getString("eventhubs.name"),
        "eventhubs.policyname" -> conf.getString("eventhubs.policyname"),
        "eventhubs.policykey" -> conf.getString("eventhubs.policykey"),
        "eventhubs.consumergroup" -> conf.getString("eventhubs.consumergroup"),
        "eventhubs.partition.count" -> conf.getString("eventhubs.partition.count"),
        "eventhubs.checkpoint.interval" -> conf.getString("eventhubs.checkpoint.interval"),
        "eventhubs.checkpoint.dir" -> conf.getString("eventhubs.checkpoint.dir")
      )
    } else if(queue == "kinesis") {
      /* TODO AWS Kinesis */
      Map[String, String](

      )
    } else {
      /* TODO Apache Kafka */
      Map[String, String](
        "metadata.broker.list" -> conf.getString("kafka.brokers"),
        "checkpoint.dir" -> conf.getString("checkpointDir")
      )
    }
    params
  }

}
