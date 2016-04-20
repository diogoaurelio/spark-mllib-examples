package com.berlinsmartdata.iris

import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS
import org.apache.spark.mllib.linalg.{Vectors, Vector}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

import org.apache.log4j.Logger
import org.apache.log4j.Level

import java.io._

object IrisLogisticRegression {

  def main(args: Array[String]): Unit = {

    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    val master = if(args.length > 0) args(0) else "local[*]"
    val appName = if(args.length > 1) args(1) else "IrisDataSetSample"
    val dataPath = if(args.length > 2) args(2) else ""
    val dataFile = if(args.length >= 3) args(2) else "iris-multiclass.csv"
    val fullPath = getFullDataPath(dataPath, dataFile)

    val sc = init_context(appMaster=master, appName=appName)
    val sqlContext = new SQLContext(sc)

    val input = getTextFile(fullPath, sc).cache()

    notebookPipeline(input, sqlContext, sc)

  }

  def init_context(appMaster:String, appName: String): SparkContext = {
    val conf = new SparkConf().setMaster(appMaster).setAppName(appName)
    val sparkContext = new SparkContext(conf)
    sparkContext
  }

  def getFullDataPath(path: String, fileName: String) = {
    val uri = getClass.getResource("/"+fileName).toURI.toString
    val filePath = if(path.length() == 0) uri else s"$path/$fileName"
    filePath

  }

  def getTextFile(fullPath:String, sc:SparkContext):RDD[String] = sc.textFile(fullPath)

  def saveModelParamsToFile(intercept: Double, weights: Vector) {
    val file = new File("./models/iris_params.txt")
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(s"intercept:${intercept},weights:${weights}")
    bw.close()
  }

  def notebookPipeline(input: RDD[String], sqlContext:SQLContext, sc: SparkContext) = {

    println("Count number of rows: " + input.count())
    println("First element " + input.first())
    print("Splitting into lines")
    val splitter = input.map(line => line.split(','))
    println("Count number of lines")
    val num = splitter.map(line => (line(4), 1)).reduceByKey((a,b) => a+b)

    println("3 Examples: " + num.take(3))

    val newSplitter = input.map(line => line.split(',')).map { line =>
      Iris(line(0).toDouble, line(1).toDouble, line(2).toDouble, line(3).toDouble, line(4))
    }
    val df = sqlContext.createDataFrame(newSplitter)
    /* OR:
    import sqlContext.implicits._

    val asplitter = input.map(line => line.split(',')).map{ line =>
      Iris(line(0).toDouble, line(1).toDouble, line(2).toDouble, line(3).toDouble, line(4))
    }
    val df = asplitter.toDF()
    */
    df.groupBy("Species").count().show()

    df.registerTempTable("iris")
    sqlContext.sql("SELECT Species, COUNT(Species) FROM iris GROUP BY Species").show()

    val classMap = Map("Iris-setosa"-> 0.0, "Iris-versicolor" -> 1.0, "Iris-virginica" -> 2.0)
    val data = input.map { line =>
      val lineSplit = line.split(',')
      val values = Vectors.dense(lineSplit.take(4).map(_.toDouble))
      LabeledPoint(classMap(lineSplit(4)), values)
    }.persist()

    val allData = data.randomSplit(Array(0.7, 0.3), seed=11L)
    val (training, test) = (allData(0), allData(1))
    val model = new LogisticRegressionWithLBFGS()
      .setNumClasses(3)
      .setIntercept(true)
      .run(training)

    val predictionAndLabelsAndFeatures = test.map { case LabeledPoint(label, features) =>
      val prediction = model.predict(features)
      (prediction, label, features)
    }
    predictionAndLabelsAndFeatures.take(5).foreach({ case (v,p,f) => println(s"Features: ${f}, Predicted: ${p}, Actual: ${v}")})

    val predictionAndLabels = predictionAndLabelsAndFeatures.map { case (p, l, f) =>
      (p, l)
    }

    // Count precision
    val mappedPredLabs = predictionAndLabels.map(mapped =>
      (classMap.filter(kv => kv._2 == mapped._1).toList(0)._1, classMap.filter(kv => kv._2 == mapped._2).toList(0)._1)
    )
    println(mappedPredLabs.take(5))
    val countWrong = mappedPredLabs.filter(items => items._1 != items._2).count()
    val countCorrect = mappedPredLabs.filter(items => items._1 == items._2).count
    val precision = countCorrect.toDouble / (countCorrect + countWrong).toDouble
    println("Precision: "+precision)

    val metrics = new MulticlassMetrics(predictionAndLabels)
    val metricsRecall = metrics.recall
    val cf = metrics.confusionMatrix
    val metricsPrecision = metrics.precision
    println("Metrics:")
    println("Precision "+metricsPrecision)
    println("Recall " + metricsRecall)
    println("Confusion Matrix \n" + cf)


    // Save and load model
    //model.save(sc, "./models/LogisticRegressionIrisDS")

    println(s">>>> Model intercept: ${model.intercept}; Model weights: ${model.weights}")
    saveModelParamsToFile(model.intercept, model.weights)

  }


}
