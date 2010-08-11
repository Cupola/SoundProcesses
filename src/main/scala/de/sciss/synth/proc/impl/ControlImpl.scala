/*
 *  ControlImpl.scala
 *  (ScalaCollider-Proc)
 *
 *  Copyright (c) 2010 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either
 *  version 2, june 1991 of the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License (gpl.txt) along with this software; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 *
 *
 *  Changelog:
 */

package de.sciss.synth.proc.impl

import de.sciss.synth.{ control, audio, scalar, freeSelf, addToTail, AudioBus, Rate, SynthGraph }
import de.sciss.synth.proc.{ ControlABusMapping, ControlBusMapping, ControlGliding, ControlValue, Glide, Instant,
   Proc, ProcAudioInput, ProcAudioOutput, ProcControl, ProcEdge, ProcParamFloat, ProcTxn, Ref,
   RichAudioBus, RichBus, RichControlBus, RichGroup, RichSynth, RichSynthDef, XFade }
import de.sciss.synth.ugen.{ A2K, Clip, In, Line, Mix, Out }
import de.sciss.{ synth => syn }

/**
 *    @version 0.12, 03-Aug-10
 */
class ControlImpl( val proc: ProcImpl, param: ProcParamFloat, val _rate: Rate )
extends ProcControl {
   ctrl =>

//   private var valueRef = Ref( default )
   private var valueRef = Ref( ControlValue.instant( default ))

   def rate    = Some( _rate )
   def default = param.default // .getOrElse( 0f )
   def spec    = param.spec
   def name    = param.name

/*
old trns new trns resolution
------------------------------------------
instant  instant  instant  if( oldValue != newValue)
instant  glide    glide    if( oldValue != newValue)
instant  xfade    xfade    if( oldValue != newValue)

glide    instant  stop glide + instant
glide    glide    stop glide + glide
glide    xfade    xfade

xfade    instant  instant  if( oldValue != newValue)
xfade    glide    glide    if( oldValue != newValue)
xfade    xfade    xfade
 */

   def cv( implicit tx: ProcTxn ) = valueRef()

   def v( implicit tx: ProcTxn ) : Double = valueRef().current

   def v_=( newValue: Double )( implicit tx: ProcTxn ) {
      val oldCV   = valueRef()
      val transit = tx.transit
      val newCVO  = oldCV.mapping match {
         case None => {
            transit match {
               case Instant => {
                  if( oldCV.target != newValue ) {
                     Some( oldCV.copy( target = newValue ))
                  } else None
               }
               case fade: XFade => {
                  if( oldCV.target != newValue ) {
                     proc.sendToBack( fade )
                     Some( oldCV.copy( target = newValue ))
                  } else None
               }
               case glide: Glide => {
                  val current = oldCV.target
                  if( current != newValue ) {
                     val startNorm  = spec.unmap( current )
                     val targetNorm = spec.unmap( newValue )
                     val newCG = _rate match {
                        case `control` => new ControlKGliding( ctrl, startNorm, targetNorm, glide )
                        case `audio`   => new ControlAGliding( ctrl, startNorm, targetNorm, glide )
                        case _ => error( "Cannot glide rate " + _rate )
                     }
                     Some( ControlValue( newValue, Some( newCG )))
                  } else None
               }
            }
         }
         case Some( _: ControlBusMapping ) => {
            if( oldCV.target != newValue ) {
               // just remember the value, but don't do anything
               // ; we could also undo the mapping hereby?
               Some( oldCV.copy( target = newValue ))
            } else None
         }
         case Some( oldCG: ControlGliding ) => {
            transit match {
               case Instant => {
                  val current = oldCG.currentValue
                  oldCG.stop
                  Some( ControlValue.instant( newValue ))
               }
               case fade: XFade => {
                  error( "NOT YET IMPLEMENTED" ) // XXX missing: we need to stop the gliss just for the new synth!
//                  proc.sendToBack( fade )
//                  Some( ControlValue.instant( newValue ))
               }
               case glide: Glide => {
                  val startNorm  = spec.unmap( oldCG.currentValue )
                  val targetNorm = spec.unmap( newValue )
                  oldCG.stop
                  val newCG = _rate match {
                     case `control` => new ControlKGliding( ctrl, startNorm, targetNorm, glide )
                     case `audio`   => new ControlAGliding( ctrl, startNorm, targetNorm, glide )
                     case _ => error( "Cannot glide rate " + _rate )
                  }
                  Some( ControlValue( newValue, Some( newCG )))
               }
            }
         }
      }
      newCVO.foreach( newCV => {
         valueRef.set( newCV )
         proc.controlChanged( ctrl, newCV )
      })
   }

   private[proc] def glidingDone( implicit tx: ProcTxn ) { v = cv.target } 

   def map( aout: ProcAudioOutput )( implicit tx: ProcTxn ) : ControlABusMapping = {
      val oldCV   = valueRef()
      oldCV.mapping match {
         case None => // Ok
         case Some( cg: ControlGliding ) => {
            v = cg.currentValue // stops the thing XXX carefull with transit
         }
         case Some( m: ControlBusMapping ) => error( "Already mapped" )
      }
      // lazy completion a bit tricky... needed because the ProcAudioOuput
      // completes the connection after map returns
      def fireControlChanged( m: ControlABusMapping ) {
         val newCV = ControlValue( oldCV.current, Some( m ))
         valueRef.set( newCV )
         proc.controlChanged( ctrl, newCV )
      }
      val m = _rate match {
         case `control` => new ControlABusToKMapping( aout, ctrl, fireControlChanged )
         case `audio`   => new ControlABusToAMapping( aout, ctrl, fireControlChanged )
         case _ => error( "Cannot map rate " + _rate )
      }
      tx.transit match {
         case Instant =>
         case fade: XFade => proc.sendToBack( fade )
         case glide: Glide => error( "NOT YET SUPPORTED" )
      }
      m
   }

   def isMapable = _rate != scalar
   def canMap( aout: ProcAudioOutput )( implicit tx: ProcTxn ) : Boolean = isMapable && !isMapped
}

trait ControlMappingImpl /* extends ControlMapping*/ {
   private val synthRef = Ref[ Option[ RichSynth ]]( None )

   // ---- abstract ----
   def ctrl: ControlImpl
   def play( implicit tx: ProcTxn ) : Unit
//   protected def addMapBusConsumers( implicit tx: ProcTxn ) : Unit
//   protected def removeMapBusConsumers( implicit tx: ProcTxn ) : Unit
//   protected def targetNode( implicit tx: ProcTxn ): RichNode
   protected def addMapBusConsumer( rs: RichSynth )( implicit tx: ProcTxn ) : Unit

   def proc    = ctrl.proc
   def name    = ctrl.name + "#map"

   def stop( implicit tx: ProcTxn ) {
println( "ControlMappingImpl.stop" )
      synthRef.swap( None ).foreach( _.free( true ))
   }

   protected def synth( implicit tx: ProcTxn ) : Option[ RichSynth ] = synthRef()
   protected def synth_=( rso: Option[ RichSynth ])( implicit tx: ProcTxn ) {
      val oldSynth = synthRef.swap( rso )
//      addMapBusConsumers   // requires that synth has been assigned!
      rso.foreach( addMapBusConsumer( _ ))
      oldSynth.foreach( _.free( true ))
   }

   def isPlaying( implicit tx: ProcTxn ) : Boolean
}

trait ControlGlidingImpl
extends ControlGliding with ControlMappingImpl {
//   def cv: ControlValue

//   def isPlaying( implicit tx: ProcTxn ) : Boolean = synth().map( _.isOnline.get ).getOrElse( false )
   def isPlaying( implicit tx: ProcTxn ) : Boolean = synth.map( _.isOnline.get ).getOrElse( false )

   def play( implicit tx: ProcTxn ) {
      val g       = graph
      val rsd     = RichSynthDef( proc.server, g )
//      val dur     = cv.transit.asInstanceOf[ Glide ].dur // XXX not so pretty
      val spec    = ctrl.spec
//      val startN  = spec.unmap( /*spec.clip(*/ startValue /*)*/)
//      val targetN = spec.unmap( targetValue )
      val rs      = rsd.play( proc.preGroup,
         List( "$start" -> startNorm, "$stop" -> targetNorm, "$dur" -> glide.dur ))

      synth = Some( rs )
//      val oldSynth = synth.swap( Some( rs ))
//      addMapBusConsumers   // requires that synth has been assigned!
//      oldSynth.foreach( _.free( true ))

      rs.onEnd { tx0 =>
         synth( tx0 ).foreach( rs2 => if( rs == rs2 ) {
//            synth.set( None )( tx0 )
            ctrl.glidingDone( tx0 )  // invokes stop and hence removeMapBusConsumers!
         })
      }
   }

   protected def graph : SynthGraph
}

class ControlKGliding( val ctrl: ControlImpl, val startNorm: Double, val targetNorm: Double, val glide: Glide )
extends ControlGlidingImpl with ControlToKMapping {
   def mapBus( implicit tx: ProcTxn ) = {
      mapBusRef().getOrElse({
         val res = RichBus.control( proc.server, 1 ) // XXX numChannels
         mapBusRef.set( Some( res ))
         res
      })
   }

//   protected def targetNode( implicit tx: ProcTxn ) = target.proc.group // XXX anyway wrong!

   protected def graph = SynthGraph {
      import syn._
      val line    = Line.kr( "$start".ir, "$stop".ir, "$dur".ir, freeSelf )
      val sig     = ctrl.spec.map( line )
      // XXX multichannel expansion
      Out.kr( "$out".kr, sig )
   }
}

class ControlAGliding( val ctrl: ControlImpl, val startNorm: Double, val targetNorm: Double, val glide: Glide )
extends ControlGlidingImpl with ControlToAMapping {
   def mapBus( implicit tx: ProcTxn ) = {
      mapBusRef().getOrElse({
         val res = RichBus.audio( proc.server, 1 ) // XXX numChannels
         mapBusRef.set( Some( res ))
         res
      })
   }

//   protected def targetNode( implicit tx: ProcTxn ) = target.proc.group // XXX anyway wrong!

   protected def graph = SynthGraph {
      import syn._
      val line    = Line.ar( "$start".ir, "$stop".ir, "$dur".ir, freeSelf )
      val sig     = ctrl.spec.map( line )
      // XXX multichannel expansion
      Out.ar( "$out".kr, sig )
   }
}

abstract class ControlABusMappingImpl
extends AbstractAudioInputImpl with ControlMappingImpl with ControlABusMapping {
   def out: ProcAudioOutput
   def in: ProcAudioInput = this
   def sourceVertex : Proc = out.proc
//   def targetVertex: Proc = proc

   // XXX override (because of AudioBusImpl)
   override def isPlaying( implicit tx: ProcTxn ) : Boolean = synth.map( _.isOnline.get ).getOrElse( false )

   def notification: ControlABusMapping => Unit
//      def connect( implicit tx: ProcTxn ) { source ~> this } // XXX NO WE DON'T NEED TO ENFORCE TOPOLOGY !!!

//   lazy val edge = ProcEdge( source, this )

//      protected val edges = Ref( Set.empty[ ProcEdge ])

//      def input : ProcAudioInput = this

//   def init( implicit tx: ProcTxn ) {
//      addEdge( edge )
//   }

   protected def edgeAdded( e: ProcEdge )( implicit tx: ProcTxn ) {
      notification.apply( this )
   }

   protected def edgeRemoved( e: ProcEdge )( implicit tx: ProcTxn ) {} // XXX should still fire

   def sendToBack( xfade: XFade, backGroup: RichGroup )( implicit tx: ProcTxn ) {
// XXX what now?
//      death.node.onEnd {
//
//      }
   }

//   /**
//    *    That means the mapping source bus changed.
//    *    If numChannels changes we need to rebuild.
//    *    Otherwise the mapping synth's "in" param
//    *    needs update.
//    */
//   def busChanged( bus: AudioBus )( implicit tx: ProcTxn ) {
//      // XXX check numChannels
//      synth().foreach( _.set( true, "in" -> bus.index ))
//   }

   def play( implicit tx: ProcTxn ) {
println( "ControlABusMappingImpl.play" )
      val inBus   = bus.get.busOption.get
      val g       = graph( inBus )
      val rsd     = RichSynthDef( inBus.server, g )
      val rs      = rsd.play( sourceVertex.postGroup, List( "$in" -> inBus.index ), addToTail )

      synth = Some( rs )
//      val oldSynth = synth.swap( Some( rs ))
//      addMapBusConsumers   // requires that synth has been assigned!
//      oldSynth.foreach( _.free( true ))
      // XXX rs.onEnd ... -> mapping stopped
   }

   protected def graph( inBus: AudioBus ) : SynthGraph
}

trait ControlToKMapping extends ControlMappingImpl {
   protected var mapBusRef = Ref[ Option[ RichControlBus ]]( None )

   // ---- abstract ----
   def mapBus( implicit tx: ProcTxn ) : RichControlBus

//   private val mapBusReader = new RichControlBus.User {
//      def busChanged( bus: ControlBus )( implicit tx: ProcTxn ) {
//         target.proc.anchorNode.mapn( true, target.name -> bus )
//      }
//   }
//
//   private val mapBusWriter = new RichControlBus.User {
//      def busChanged( bus: ControlBus )( implicit tx: ProcTxn ) {
//         synth().foreach( _.set( true, "$out" -> bus.index ))
//      }
//   }

   protected def addMapBusConsumer( rs: RichSynth )( implicit tx: ProcTxn ) {
      val rb = mapBus
      rs.write( rb -> "$out" )
      // XXX que mierda. how do we remove the setter?
      // this should go in proc.controlChanged
//      proc.anchorNode.map( rb -> ctrl.name )
   }

//   protected def addMapBusConsumers( implicit tx: ProcTxn ) {
//      mapBus.addReader( mapBusReader )
//      mapBus.addWriter( mapBusWriter )
//   }
//
//   protected def removeMapBusConsumers( implicit tx: ProcTxn ) {
//      mapBus.removeReader( mapBusReader )
//      mapBus.removeWriter( mapBusWriter )
//   }
}

trait ControlToAMapping extends ControlMappingImpl {
   protected var mapBusRef = Ref[ Option[ RichAudioBus ]]( None )

   // ---- abstract ----
   def mapBus( implicit tx: ProcTxn ) : RichAudioBus

//   private val mapBusWriter = new RichAudioBus.User {
//      def busChanged( bus: AudioBus )( implicit tx: ProcTxn ) {
//         synth().foreach( _.set( true, "$out" -> bus.index ))
//      }
//   }
//
//   private val mapBusReader = new RichAudioBus.User {
//      def busChanged( bus: AudioBus )( implicit tx: ProcTxn ) {
//         target.proc.anchorNode.mapan( true, target.name -> bus )
//      }
//   }

   protected def addMapBusConsumer( rs: RichSynth )( implicit tx: ProcTxn ) {
      val rb = mapBus
      rs.write( rb -> "$out" )
      // XXX que mierda. how do we remove the setter?
      // this should go in proc.controlChanged
//      proc.anchorNode.map( rb -> ctrl.name )
   }

//   protected def addMapBusConsumers( implicit tx: ProcTxn ) {
//      mapBus.addReader( mapBusReader )
//      mapBus.addWriter( mapBusWriter )
//   }
//
//   protected def removeMapBusConsumers( implicit tx: ProcTxn ) {
//      mapBus.removeReader( mapBusReader )
//      mapBus.removeWriter( mapBusWriter )
//   }
}

class ControlABusToKMapping( val out: ProcAudioOutput, val ctrl: ControlImpl,
                             val notification: ControlABusMapping => Unit )
extends ControlABusMappingImpl with ControlToKMapping {
   override def toString = "aIn(" + proc.name + " @ " + name + ")"

   def mapBus( implicit tx: ProcTxn ) = {
      mapBusRef() getOrElse {
         val inBus   = bus.get.busOption.get
         val res     = RichBus.control( inBus.server, 1 ) // XXX inBus.numChannels
         mapBusRef.set( Some( res ))
         res
      }
   }

//   protected def targetNode( implicit tx: ProcTxn ) = target.proc.group // XXX anyway wrong!

   protected def graph( inBus: AudioBus ) = SynthGraph {
      import syn._
//      val sig0    = A2K.kr( In.ar( "$in".kr, inBus.numChannels ))
      val sig0    = A2K.kr( Mix( In.ar( "$in".kr, inBus.numChannels ))) // XXX eventually handle multi-channel
      val clipped = Clip.kr( sig0, -1, 1 )
      val sig     = ctrl.spec.map( clipped.madd( 0.5f, 0.5f ))
      Out.kr( "$out".kr, sig )
   }
}

class ControlABusToAMapping( val out: ProcAudioOutput, val ctrl: ControlImpl,
                             val notification: ControlABusMapping => Unit )
extends ControlABusMappingImpl with ControlToAMapping {
   override def toString = "aIn(" + proc.name + " @ " + name + ")"

   def mapBus( implicit tx: ProcTxn ) = {
      mapBusRef() getOrElse {
         val inBus   = bus.get.busOption.get
         val res     = RichBus.audio( inBus.server, 1 ) // XXX inBus.numChannels
         mapBusRef.set( Some( res ))
         res
      }
   }

//   protected def targetNode( implicit tx: ProcTxn ) = target.proc.group // XXX anyway wrong!

   protected def graph( inBus: AudioBus ) = SynthGraph {
      import syn._
//      val sig0    = In.ar( "$in".kr, inBus.numChannels )
      val sig0    = Mix( In.ar( "$in".kr, inBus.numChannels )) // XXX eventually handle multi-channel
      val clipped = Clip.ar( sig0, -1, 1 )
      val sig     = ctrl.spec.map( clipped.madd( 0.5f, 0.5f ))
      Out.ar( "$out".kr, sig )
   }
}