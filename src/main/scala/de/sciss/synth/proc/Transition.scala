package de.sciss.synth.proc

/**
 *    @version 0.10, 03-Jul-10
 */
//object Transition {
//   private val currentRef = TxnLocal[ Transition ]( Instant )
//   def current( implicit tx: ProcTxn ) : Transition = currentRef()
//   def use[ T ]( trns: Transition )( thunk: => T )( implicit tx: ProcTxn ) : T = {
//      val old = currentRef.swap( trns )
//      try {
//         thunk
//      } finally {
//         currentRef.swap( old )
//      }
//   }
//}

sealed abstract class Transition {
//   import Transition._
//
//   def apply[ T ]( thunk: => T )( implicit tx: ProcTxn ) : T = {
//      use( this )( thunk )
//    }

   def position( time: Double ) : Double
   def positionApprox : Double
   def finish( implicit tx: ProcTxn ) : Unit
}

case object Instant extends Transition {
   def position( time: Double ) = 1.0
   def positionApprox = 1.0
   def finish( implicit tx: ProcTxn ) {}
}

sealed abstract class DurationalTransition extends Transition {
   private val sys = System.currentTimeMillis

   def start: Double
   def dur: Double

   def position( time: Double ) = {
//      val res =
      if( dur > 0.0 ) math.max( 0.0, math.min( 1.0, (time - start) / dur )) else 1.0
//      println( "time = " + time + "; start = " + start + "; off = " + (time-start) + "; dur = " + dur + "; res = " + res )
//      res
   }

   def positionApprox = {
      if( dur > 0.0 ) math.max( 0.0, math.min( 1.0, (System.currentTimeMillis - sys) * 0.001 / dur )) else 1.0
   }
}

object XFade {
   var verbose = false
}

case class XFade( start: Double, dur: Double )
extends DurationalTransition {
   import XFade._
   
   private var markMap = Map.empty[ TxnPlayer, Boolean ]

   def isMarked( p: TxnPlayer ) : Boolean = markMap.contains( p )

   def markSendToBack( p: TxnPlayer, replay: Boolean ) /* ( implicit tx: ProcTxn ) */ : Boolean = {
//      val s       = markSet()
      val isNew   = !markMap.contains( p )
         if( verbose ) println( "xfade markSendToBack(" + p + ", " + replay + ")" )
//      if( isNew ) {
         markMap += (p -> replay)
//      }
      isNew
   }

   def finish( implicit tx: ProcTxn ) {
      if( markMap.isEmpty ) return
      
      markMap foreach { tup =>
         val (p, replay) = tup
         if( verbose ) println( "xfade finish : " + tup )
         if( replay ) p.play
      }
      markMap = Map.empty
   }
}

case class Glide( start: Double, dur: Double )
extends DurationalTransition {
   def finish( implicit tx: ProcTxn ) {}
}
