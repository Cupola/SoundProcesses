package de.sciss.synth.proc.impl

import de.sciss.synth.proc.{ Proc, ProcEntryBuilder, ProcParamAudioInput, ProcParamAudioOutput,
   ProcRunning, ProcTxn }

class IdleBuilderImpl( idle: IdleImpl, val tx: ProcTxn )
extends EntryBuilderImpl {
   def play : ProcRunning = {
      implicit val t = tx
      ProcEntryBuilder.use( this ) {
         val p             = Proc.local
         idle.fun()
//         var accessories   = IQueue.empty[ RichSynth => AudioBusPlayerImpl ]

         usedParams.foreach( _ match {
            case pAudioBus: ProcParamAudioInput => {
               val name       = pAudioBus.name
               val b          = p.audioInput( name )
               val rab        = b.bus.get
//               accessories    = accessories.enqueue( rs => AudioBusPlayerImpl( b, rs.read( rab -> name )))
//               accessories    = accessories.enqueue( rab
            }
            case pAudioBus: ProcParamAudioOutput => {
               val name       = pAudioBus.name
               val b          = p.audioOutput( name )
               val rab        = b.bus.get
//               accessories    = accessories.enqueue( rs => AudioBusPlayerImpl( b, rs.write( rab -> name )))
            }
            case x => println( "Ooops. what parameter is this? " + x ) // scalac doesn't check exhaustion...
         })

         val (target, addAction) = p.runningTarget( true )  // 'true' so that we have a stable group for internal graphs
//         val accMap: Map[ String, AudioBusPlayerImpl ] = accessories.map( fun => {
//            val abp = fun( rs )
//            abp.player.play // stop is in RunningGraphImpl
//            abp.setter.controlName -> abp
//         })( breakOut )
         if( true ) error( "SCHNUCK ")
         new RunningIdleImpl( null, null )
      }
   }
}
