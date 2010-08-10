import xml._
import sbt.{ FileUtilities => FU, _}

/**
 *    @version 0.14, 02-Aug-10
 */
class SoundProcessesProject( info: ProjectInfo ) extends DefaultProject( info ) {
   // stupidly, we need to redefine the dependancy here, because
   // for some reason, sbt will otherwise try to look in the maven repo
   val dep2 = "de.sciss" %% "scalaaudiofile" % "0.13"
   val dep3 = "de.sciss" %% "scalaosc" % "0.19"
   val dep4 = "de.sciss" %% "scalacollider" % "0.17"
   val dep9 = "Stanford CS - Pervasive Parallelism Laboratory" %% "ccstm" % "0.2"

   // ---- ccstm dependancies ----
   val scalatest = crossScalaVersionString match {
     // RC7 is the same as the release, but scalatest against the release is not
     // yet available
     case "2.8.0" => "org.scalatest" % "scalatest" % "1.2-for-scala-2.8.0.RC7-SNAPSHOT"
     case _ => "org.scalatest" % "scalatest" % ("1.2-for-scala-" + crossScalaVersionString + "-SNAPSHOT")
   }
   val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
}
