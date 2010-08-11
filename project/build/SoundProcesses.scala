import xml._
import sbt.{ FileUtilities => FU, _}

/**
 *    @version 0.14, 02-Aug-10
 */
class SoundProcessesProject( info: ProjectInfo ) extends DefaultProject( info ) {
   // stupidly, we need to redefine the dependancy here, because
   // for some reason, sbt will otherwise try to look in the maven repo
   val scalaAudioFile   = "de.sciss" %% "scalaaudiofile" % "0.13"
   val scalaOSC         = "de.sciss" %% "scalaosc" % "0.19"
   val scalaCollider    = "de.sciss" %% "scalacollider" % "0.17"
   val ccstm            = "edu.stanford.ppl" % "ccstm" % "0.2.1-for-scala-2.8.0"
   val ccstmRepo        = "CCSTM Release Repository at PPL" at "http://ppl.stanford.edu/ccstm/repo-releases"
}
