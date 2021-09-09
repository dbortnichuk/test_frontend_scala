package com.db

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

package object app {

  val language = "scala"
  val latestVersion = "latest"

  implicit val system = ActorSystem(Behaviors.empty, "my-system")
  implicit val executionContext = system.executionContext

  val ParamDataSource = "ds"
  val ParamApiKey= "apiKey"

  val SegmentProtected = "protected"
  val SegmentLocal = "local"
  val SegmentDirect = "direct"
  val SegmentHealth = "health"
  val SegmentReady = "ready"
}
