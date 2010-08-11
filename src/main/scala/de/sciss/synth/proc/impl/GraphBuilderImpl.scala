package de.sciss.synth.proc.impl

import collection.breakOut
import collection.immutable.{ Queue => IQueue }
import de.sciss.synth.proc.{ Proc, ProcBuffer, ProcGraphBuilder, ProcParam, ProcParamAudioInput,
   ProcParamAudioOutput, ProcParamFloat, ProcRunning, ProcTxn,
   RichAudioBus, RichControlBus, RichSynth, RichSynthDef }
import de.sciss.synth.{ ControlSetMap, SingleControlSetMap, SynthGraph }

/**
 *    @version 0.12, 03-Aug-10
 */
class GraphBuilderImpl( graph: GraphImpl, val tx: ProcTxn )
extends EntryBuilderImpl with ProcGraphBuilder {
   var buffers    = Set.empty[ ProcBuffer ]
   private var bufCount = 0

   def includeBuffer( b: ProcBuffer ) {
      buffers += b
   }

   def bufEmpty( numFrames: Int, numChannels: Int ) : ProcBuffer = {
      val unique = bufCount
      bufCount += 1
      new BufferEmptyImpl( unique, numFrames, numChannels )
   }

   def bufCue( path: String, startFrame: Long ) : ProcBuffer = {
      val unique = bufCount
      bufCount += 1
      new BufferCueImpl( unique, path, startFrame )
   }
   
   /**
    */
   def play : ProcRunning = {
      implicit val t = tx
      ProcGraphBuilder.use( this ) {
         val p             = Proc.local
         val g             = SynthGraph( graph.fun() )

         val server        = p.server
         val rsd           = RichSynthDef( server, g )
         val bufSeq        = buffers.toSeq
         val bufs          = bufSeq.map( _.create( server ))

         var setMaps       = Vector.empty[ ControlSetMap ]  // warning: rs.newMsg doesn't support setn style! XXX
         var accessories   = IQueue.empty[ RichSynth => AudioBusPlayerImpl ]

         usedParams.foreach( _ match {
            case pFloat: ProcParamFloat => {
               val name = pFloat.name
               val cv   = p.control( name ).cv
               cv.mapping match {
                  case None => setMaps :+= SingleControlSetMap( name, cv.target.toFloat )
                  case Some( m ) => m.mapBus match {
                     case rab: RichAudioBus => {
                        accessories = accessories.enqueue( rs => AudioBusPlayerImpl( m, rs.map( rab -> name )))
//                        audioMappings  = audioMappings.enqueue( rab -> name )
                     }
                     case rcb: RichControlBus => {
                        println( "WARNING: Mapping to control bus not yet supported" )
                        setMaps :+= SingleControlSetMap( name, cv.target.toFloat )
                     }
                  }
               }
            }
            case pAudioBus: ProcParamAudioInput => {
               val name       = pAudioBus.name
               val b          = p.audioInput( name )
               val rab        = b.bus.get
               accessories    = accessories.enqueue( rs => AudioBusPlayerImpl( b, rs.read( rab -> name )))
//               accessories    = accessories.enqueue( b )
//               audioInputs    = audioInputs.enqueue( b.bus.get -> name )
            }
            case pAudioBus: ProcParamAudioOutput => {
               val name       = pAudioBus.name
               val b          = p.audioOutput( name )
               val rab        = b.bus.get
               accessories    = accessories.enqueue( rs => AudioBusPlayerImpl( b, rs.write( rab -> name )))
//               accessories    = accessories.enqueue( b )
//               audioOutputs   = audioOutputs.enqueue( b.bus.get -> name )
            }
            case x => println( "Ooops. what parameter is this? " + x ) // scalac doesn't check exhaustion...
         })

         val (target, addAction) = p.runningTarget( false )
         val bufsZipped = bufSeq.zip( bufs )
         setMaps ++= bufsZipped.map( tup => SingleControlSetMap( tup._1.controlName, tup._2.buf.id ))
         val rs = rsd.play( target, setMaps, addAction, bufs )

         val accMap: Map[ String, AudioBusPlayerImpl ] = accessories.map( fun => {
            val abp = fun( rs )
//println( "acc : " + abp )
            abp.player.play // stop is in RunningGraphImpl
            abp.setter.controlName -> abp
         })( breakOut )

         bufsZipped foreach { tup =>
            val (b, rb) = tup
            b.disposeWith( rb, rs )        // XXX should also go in RunningGraphImpl
         }
         new RunningGraphImpl( rs, accMap )
      }
   }
}