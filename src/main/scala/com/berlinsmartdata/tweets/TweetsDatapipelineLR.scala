package com.berlinsmartdata.tweets


import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.sql.Row

import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.util.MLUtils

object TweetsDatapipelineLR {

  def main(args: Array[String]) = {

    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)
    // Reference: https://databricks.com/blog/2015/01/07/ml-pipelines-a-new-high-level-api-for-mllib.html

    val appName = if(args.length > 0) args(0) else "ML_pipeline"
    val appMaster = if(args.length > 1) args(1) else "local[*]"
    val trainDataFile = if(args.length > 2) args(2) else "training-tweets.csv"
    val testDataFile = if(args.length > 3) args(3) else "test-tweets.csv"
    val trainDataPath = if(args.length > 4) args(4) else getClass.getResource("/"+ trainDataFile).toURI.toString
    val testDataPath = if(args.length > 5) args(5) else getClass.getResource("/"+ testDataFile).toURI.toString

    // initialize spark stuff
    val sc = init_context(appMaster, appMaster)
    val sqlContext = new SQLContext(sc)

    //val training = sc.textFile(trainDataPath)
    //training.foreach(line => println(line))
    val trainDf = sqlContext.createDataFrame(preStructureRawData(sc.textFile(trainDataPath))).cache()
    val testDf = sqlContext.createDataFrame(preStructureRawData(sc.textFile(testDataPath))).cache()

    // Configure a Pipeline
    val tokenizer = new Tokenizer().setInputCol("text").setOutputCol("words")
    val hashingTF = new HashingTF().setNumFeatures(1000).setInputCol(tokenizer.getOutputCol).setOutputCol("features")
    val lr = new LogisticRegression().setMaxIter(10).setRegParam(0.01)

    // Create Pipeline
    val pipeline = new Pipeline().setStages(Array(tokenizer, hashingTF, lr))
    val model = pipeline.fit(trainDf)

    //Note: only binary LogisticRegression supported for PMML
    //model.toPMML("SentimentAnalysisLogisticRegressionPredictiveModelMarkupLanguage.xml")
    model.save("./models/TweetsLogisticRegression.fit")

    val modelem = model.transform(testDf).select("id", "label","text","probability","prediction")
    modelem.collect().foreach {
      case Row(id: Int, label: Double, text: String, probability: Vector, prediction: Double) =>
        println(s"($id, $text) --> prob=$probability, prediction=$prediction, label=$label")
    }
    // accuracy:
    val acc = (modelem.filter("label = prediction").count().toDouble / modelem.count().toDouble) * 100D
    println(s"Accuracy: $acc")

    // Now with CrossValidation -> and we use Hyperparameter tuning for Regularization and number of features
    // extract/transform numFeatures = 10, 20, 40
    val paramGrid = new ParamGridBuilder()
      .addGrid(hashingTF.numFeatures, Array(100, 500, 1000))
      .addGrid(lr.regParam, Array(0.01, 0.1, 1.0))
      .build()

    val cv = new CrossValidator().setNumFolds(3)
      .setEstimator(pipeline)
      .setEstimatorParamMaps(paramGrid)
      .setEvaluator(new BinaryClassificationEvaluator)

    // fit model with CrossValidation
    val cvModel = cv.fit(trainDf)

    val modelem2 = cvModel.transform(testDf).select("id", "label", "text", "probability", "prediction")
    modelem2.collect().foreach {
      case Row(id: Int, label: Double, text: String, probability: Vector, prediction: Double) =>
        println(s"($id, $text) --> prob=$probability, prediction=$prediction, label=$label")
    }
    // accuracy:
    val acc2 = (modelem2.filter("label = prediction").count().toDouble / modelem2.count().toDouble) * 100D
    println(s"Accuracy first model: $acc")
    println(s"Accuracy second model: $acc2")

    println("Oddly enough, first outperforms second...")



    // Get evaluation metrics.
    /*
    val metrics = new BinaryClassificationMetrics(modelem2)
    val auROC = metrics.areaUnderROC()

    println("Area under ROC = " + auROC)

    // Save and load model

    //val sameModel = SVMModel.load(sc, "myModelPath")
    */


  }

  def init_context(appName: String, appMaster: String): SparkContext = {
    val conf = new SparkConf().setAppName(appName).setMaster(appMaster)
    new SparkContext(conf)
  }

  def preStructureRawData(dataSet:RDD[String]): RDD[Tweets] = {
    dataSet.zipWithIndex.filter(_._2 > 0)
      .map(line => line._1.split(",")).map(tw => Tweets(tw(0).toInt, tw(1).toDouble, tw(2), tw(3) ))
  }

}
