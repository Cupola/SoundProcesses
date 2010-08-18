import xml._
import sbt.{ FileUtilities => FU, _}

/**
 *    @version 0.15, 11-Aug-10
 */
class SoundProcessesProject( info: ProjectInfo ) extends DefaultProject( info ) {
   val scalaCollider    = "de.sciss" %% "scalacollider" % "0.17"
//   val ccstm            = "edu.stanford.ppl" % "ccstm" % "0.2.1-for-scala-2.8.0"
//   val ccstmRepo        = "CCSTM Release Repository at PPL" at "http://ppl.stanford.edu/ccstm/repo-releases"
   val ccstm = "edu.stanford.ppl" % "ccstm" % "0.2.2-for-scala-2.8.0-SNAPSHOT"
   val ccstmRepo = "CCSTM Release Repository at PPL" at "http://ppl.stanford.edu/ccstm/repo-releases"
   val ccstmSnap = "CCSTM Snapshot Repository at PPL" at "http://ppl.stanford.edu/ccstm/repo-snapshots"
}
