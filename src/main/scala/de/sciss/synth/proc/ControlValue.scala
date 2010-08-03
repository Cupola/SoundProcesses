package de.sciss.synth.proc

object ControlValue {
   def instant( value: Double ) = ControlValue( value, None )
}

case class ControlValue( target: Double, mapping: Option[ ControlMapping ]) {
   def current( implicit txn: ProcTxn ) : Double = mapping match {
      case Some( cg: ControlGliding ) => cg.currentValue
      case _ => target
   }

   def instant = ControlValue( target, None )

   /**
    *    Transaction-less coarse approximation
    *    of the current value.  Useful for a GUI
    *    which does not want to mess around with
    *    txn.
    */
   def currentApprox : Double = mapping match {
      case Some( cg: ControlGliding ) => cg.currentValueApprox
      case _ => target
   }
}

sealed trait ControlMapping extends TxnPlayer {
   def mapBus( implicit tx: ProcTxn ) : RichBus
}

trait ControlGliding extends ControlMapping {
   def startNorm : Double
   def targetNorm : Double
   def ctrl: ProcControl
   def glide : Glide

   def currentNorm( implicit tx: ProcTxn ) : Double = {
      val w = glide.position( tx.time )
      startNorm * (1 - w) + targetNorm * w
   }

   def currentNormApprox : Double = {
      val w = glide.positionApprox
      startNorm * (1 - w) + targetNorm * w
//println( "currentNormApprox : pos = " + w + "; startNorm = " + startNorm + "; targetNorm = " + targetNorm +" ; res = " +res )
//      res
   }

   def startValue    = ctrl.spec.map( startNorm )
   def targetValue   = ctrl.spec.map( targetNorm )

   def currentValue( implicit tx: ProcTxn ) : Double = ctrl.spec.map( currentNorm )
   def currentValueApprox : Double = ctrl.spec.map( currentNormApprox )
}

sealed trait ControlBusMapping extends ControlMapping {

}

trait ControlABusMapping extends ControlBusMapping {
//   def edge : ProcEdge// ( implicit tx: ProcTxn )
   def in: ProcAudioInput
   def out: ProcAudioOutput
}

//trait ControlKBusMapping extends ControlBusMapping {
//
//}