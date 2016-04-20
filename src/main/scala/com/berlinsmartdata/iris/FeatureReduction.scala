package com.berlinsmartdata.iris

import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.sql.types.{StructField, StructType, StringType}
import org.apache.spark.mllib.linalg.{VectorUDT, Vector, Vectors}
import org.apache.spark.ml.feature.PCA
import org.apache.log4j.{Level, Logger}


object FeatureReduction {

  def main(args: Array[String]) {

    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("aka").setLevel(Level.OFF)

    val master = if(args.length > 0) args(0) else "local[*]"
    val appName = if(args.length > 1) args(1) else "Feature Reduction example"
    val fileName = if(args.length > 2) args(2) else "iris-multiclass.csv"
    val path = if(args.length > 3) args(3) + "/" + fileName else getClass().getResource("/"+fileName).toURI().toString()

    val sc = init_context(master, appName)
    val sqlContext = new SQLContext(sc)

    val input = sc.textFile(path).cache()

    val vec = input.map(line => line.split(',')).map(mapped => Vectors.dense(
      mapped(0).toDouble, mapped(1).toDouble, mapped(2).toDouble, mapped(3).toDouble
    ))
    vec.map(println)
    println(vec.take(3))
    val data = input.map(line => line.split(',')).map(line => Iris(line(0).toDouble, line(1).toDouble, line(2).toDouble, line(3).toDouble, line(4).toString))
    val df = sqlContext.createDataFrame(data)


    /* Or something like this to avoid collect() */


    val myVecArray = Array(Vectors.dense(1.0, 3.0, 5.0), Vectors.dense(9.0, 4.5, 2.5), Vectors.dense(12.9, 3.4, 5.6))
    val vecRdd = sc.parallelize(myVecArray).map(line=> Row(line))
    val schema = new StructType().add("features", new VectorUDT())

    val myDf = sqlContext.createDataFrame(vecRdd, schema)


    val pca = new PCA().setInputCol("features").setOutputCol("outputfeatures").setK(2)
    val pcsModel = pca.fit(myDf)

    val pcaDF = pcsModel.transform(myDf)
    val result = pcaDF.select("outputfeatures").show()

    val matrix = new RowMatrix(vec)
    val comps = matrix.computePrincipalComponents(2)
    println(comps)

  }

  def init_context(appMaster: String, appName: String): SparkContext = {
    val conf = new SparkConf().setMaster(appMaster).setAppName(appName)
    new SparkContext(conf)
  }

}