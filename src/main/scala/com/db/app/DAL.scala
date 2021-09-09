package com.db.app

trait DAL {
  def get(): String
}

class SimpleDAL(volumePath: Option[String]) extends DAL {
  override def get(): String = ???
}

class VolumeDAL(volumePath: String) extends DAL {
  override def get(): String = ???
}

class MysqlDAL extends DAL {
  override def get(): String = ???
}

class S3DAL extends DAL {
  override def get(): String = ???
}
