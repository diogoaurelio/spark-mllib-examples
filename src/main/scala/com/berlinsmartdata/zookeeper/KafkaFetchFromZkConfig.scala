package com.berlinsmartdata.zookeeper

import com.berlinsmartdata.utils.JobConfig
import kafka.admin.AdminUtils
import kafka.utils.ZKStringSerializer
import org.I0Itec.zkclient.ZkClient


object KafkaFetchFromZkConfig {
  val conf = JobConfig.getConfiguration
  val zkClient = new ZkClient(conf.getString("zookeeper.hosts"),
    conf.getString("zookeeper.sessionTimeoutMs").toInt, conf.getString("zookeeper.connectionTimeoutMs").toInt,
    ZKStringSerializer
  )

  def listTopics: Seq[String] = {
    AdminUtils.fetchAllTopicConfigs(zkClient).keys.toSeq
  }

  def topicProperties: Map[String, Int] = {
    listTopics.map(topic => (topic, numPartitions(topic))).toMap
  }

  def numPartitions(topic: String): Int = {
    AdminUtils.fetchTopicMetadataFromZk(topic, zkClient).partitionsMetadata.size
  }

  def main(args: Array[String]): Unit = {
    for(topic <- listTopics) {
      println(s"Topic: {$topic} has " +  numPartitions(topic)+" partitions." )
    }
  }

}
