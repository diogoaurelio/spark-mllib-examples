package com.berlinsmartdata.tweets

import com.berlinsmartdata.utils.QueuesHelpers
import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming.{StreamingContext, Seconds}
import org.apache.spark.{SparkConf, SparkContext}

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.{LogisticRegressionModel, LogisticRegression}
import org.apache.spark.ml.feature.{IDF, HashingTF, Tokenizer}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.sql.Row
import org.apache.spark.sql.functions._

import scala.collection.mutable
;

case class TweetsStream(id: Int, label: Double, source: String, text: String) extends Serializable

object TweetsStreaming extends App with QueuesHelpers {

  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("akka").setLevel(Level.OFF)
  // Reference: https://databricks.com/blog/2015/01/07/ml-pipelines-a-new-high-level-api-for-mllib.html

  val appName = if(args.length > 0) args(0) else "TweetStreaming"
  val appMaster = if(args.length > 1) args(1) else "local[*]"

  val trainDataFile = if(args.length > 2) args(2) else "datasets/training-tweets.csv"
  val testDataFile = if(args.length > 3) args(3) else "datasets/test-tweets.csv"
  val trainDataPath = if(args.length > 4) args(4) else getClass.getResource("/datasets/"+ trainDataFile).toURI.toString
  val testDataPath = if(args.length > 5) args(5) else getClass.getResource("/datasets/"+ testDataFile).toURI.toString

  val checkpoint = if(args.length > 6) args(6) else "./checkpoint"
  val queueType = if(args.length > 7) args(7) else "microsoft"

  /* party starts here */
  val conf = new SparkConf().setAppName(appName).setMaster(appMaster)
  val sc = new SparkContext(conf)
  val sqlContext = new SQLContext(sc)

  // Note: tweet anatomy reminder: (ItemID,Sentiment,SentimentSource,SentimentText,)
  // note2: zipWithIndex() would: (Index, ItemID,Sentiment,SentimentSource,SentimentText,,0)
  val raw = sc.textFile(trainDataPath)
  //raw.take(5).foreach(line => println(line))
  val training = raw
    .zipWithIndex()
    .filter(line => line._2 > 0) // clean out the first raw, since its the header
    .map(line => line._1.split(","))
    .map(tw => TweetsStream(tw(0).toInt, tw(1).toDouble, tw(2), tw(3)))

  val trainingDf = sqlContext.createDataFrame(training).cache()

  println(trainingDf.show(5))
  println("---")
  val tokenizer = new Tokenizer().setInputCol("text").setOutputCol("words")
  val tokenized = tokenizer.transform(trainingDf)

  tokenized.select("words", "label").take(3).foreach(println)
  val hashingTF = new HashingTF()
    .setInputCol(tokenizer.getOutputCol)
    .setOutputCol("rawFeatures")
    .setNumFeatures(1000)
  val featurizedData = hashingTF.transform(tokenized)
  val idf = new IDF().setInputCol("rawFeatures").setOutputCol("features")
  val idfModel = idf.fit(featurizedData)
  val rescaledData = idfModel.transform(featurizedData)

  val lr = new LogisticRegression()
    .setMaxIter(10)
    .setRegParam(0.01)

  val pipeline = new Pipeline().setStages(Array(tokenizer, hashingTF, idf, lr))
  val pipelineModel = pipeline.fit(trainingDf)
  val lastModel = pipelineModel.stages.last.asInstanceOf[LogisticRegressionModel]


  println("pipelineModel: " + pipelineModel.explainParams())
  println(s">>>> Model Weights: ${lastModel.coefficients}; Model Intercept: ${lastModel.intercept}")
  //println(s">>>> Model intercept: ${pipelineModel.intercept}; Model weights: ${pipelineModel.weights}")

  /*
  val transformedDF = pipelineModel.transform(trainingDf)
  val meta: org.apache.spark.sql.types.Metadata = transformedDF
    .schema(transformedDF.schema.fieldIndex("features"))
    .metadata
  println(meta)
  val attrs = meta.getMetadata("ml_attr").getMetadata("attrs")
  val attrs_names = attrs.getMetadataArray("numeric")(0).getString("name")
  println(s"Attributes")
  println(s"${attrs_names}")
  println(s"${attrs}")


  pipelineModel.write.overwrite().save("./models/TweetsLR2")
  */


  val ssc = new StreamingContext(sc, Seconds(5))
  ssc.checkpoint(checkpoint)

  val total = sc.accumulator(0, "Total")
  val current = sc.accumulator(0, "Current")


  /* TODO
  val eventStream = queueStream(ssc, queueType, checkpoint)
  val mapped = eventStream.map { bytes =>
    val tweetLine = (new String(bytes)).split(",")
    Tweets(tweetLine(0).toInt, tweetLine(1).toDouble, tweetLine(2),tweetLine(3))
  }

  mapped.foreachRDD { tweets =>
    val result = context.test(sqlContext, tweets)
    total += result._1
    current += result._2
    println("*************************************************")
    println("----------> " + "%.2f".format(current.value.toDouble *1000/total.value.toDouble) + "correct guess")
    println("*************************************************")
  }
  */




}