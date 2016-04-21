package com.berlinsmartdata.interfaceExamples

import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{StreamingContext, Seconds}
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.streaming.Duration
import org.apache.spark.streaming.kinesis._
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream



object Streaming extends App {

  val conf = new SparkConf().setAppName("streaming dummy sample")
  val sc = new SparkContext(conf)

  val ssc = new StreamingContext(sc, Seconds(5))
  ssc.checkpoint("./checkpoint")

  /* Queues */
  /* microsoft eventHub
  val eventHubParams = ("","")
  val eventHubStream = EventHubsUtils.createUnionStream(ssc, eventHubParams)
  */

  /* aws Kinesis
  val kinesisAppName = ""
  val kinesisStreamName = ""
  val kinesisendpointURL = ""
  val awsRegion = ""
  val initialPosition = 0
  val checkpointInterval = 5
  val messageHandler = ""
  val awsAccessKeyId = ""
  val awsSecretKey = ""
  val kinesisStream = KinesisUtils.createStream(
    ssc, kinesisAppName, kinesisStreamName, kinesisendpointURL,
    awsAccessKeyId, awsSecretKey, awsRegion, initialPosition,
    checkpointInterval, StorageLevel.MEMORY_AND_DISK_2,
    messageHandler)
    */

  ssc.start()
  ssc.awaitTerminationOrTimeout(10L)


}
