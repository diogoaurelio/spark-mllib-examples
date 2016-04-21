package com.berlinsmartdata.utils

import com.typesafe.config.{Config, ConfigFactory}
/**
  * Created by diogo on 21.04.16.
  */
object JobConfig {

  val CONF_FILE = "application.conf"

  def getConfiguration: Config = {
    ConfigFactory.load(CONF_FILE)
  }

}
