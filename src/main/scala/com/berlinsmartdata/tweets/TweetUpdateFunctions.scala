package com.berlinsmartdata.tweets

import org.apache.spark.SparkContext
import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql.SQLContext

/**
  * Created by diogo on 21.04.16.
  */
class TweetUpdateFunctions extends Serializable {

  def updateKeyTweetText(tweetTexts: Seq[Tweets], probability: Option[(Int, Int)] = None) : Option[(Int, Int)] = None = {
    val prob = probability.getOrElse(0,0)
    val sc = SparkContext.getOrCreate()
    val model = PipelineModel.load("/models/TweetsLogisticRegression.fit")
    val tweetStreamingContext = new TweetsStreamingContext(model)
    val tweets = sc.parallelize(tweetTexts)
    val outter = tweetStreamingContext.test(new SQLContext(sc), tweets)
    val output = (prob._1=outter._1, prob._2 = outter._2)

  }
}
