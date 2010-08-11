/*
 *  ParamImpl.scala
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

import de.sciss.synth.ugen.{ In, Out }
import de.sciss.synth.{ audio, control, GE }
import de.sciss.synth.proc.{ ParamSpec, Proc, ProcEntryBuilder, ProcParamAudio, ProcParamAudioInput,
   ProcParamAudioOutput, ProcParamControl, ProcParamScalar,
   ProcParamUnspecifiedException, RichAudioBus, RichBus }
import de.sciss.synth

/**
 *    @version 0.13, 02-Aug-10
 */
class ParamScalarImpl( val name: String, val spec: ParamSpec, val default: Double )
extends ProcParamScalar {
   def ir : GE = {
      import synth._
      val p             = Proc.local
      val pb            = ProcEntryBuilder.local
      implicit val tx   = pb.tx
      val c             = p.control( name )
      pb.includeParam( this )
      name.ir( default )
   }

   def v : Double = {
      val p             = Proc.local
      val pb            = ProcEntryBuilder.local
      implicit val tx   = pb.tx
      val c             = p.control( name )
      c.v
   }
}

class ParamControlImpl( val name: String, val spec: ParamSpec, val default: Double )
extends ProcParamControl {
   def kr : GE = {
      import synth._
      val p             = Proc.local
      val pb            = ProcEntryBuilder.local
      implicit val tx   = pb.tx
      val c             = p.control( name )
      pb.includeParam( this )
      c.cv.mapping match {
         case None => name.kr( default )
         case Some( m ) => {
            val outBus = m.mapBus // .get
            require( outBus.rate == control )
            // stupidly we have two arguments...
            name.kr( default, List.fill( outBus.numChannels - 1 )( default ): _* )
         }
      }
   }
}

class ParamAudioImpl( val name: String, val spec: ParamSpec, val default: Double )
extends ProcParamAudio {
   def ar : GE = {
      import synth._
      val p             = Proc.local
      val pb            = ProcEntryBuilder.local
      implicit val tx   = pb.tx
      val c             = p.control( name )
      pb.includeParam( this )
      c.cv.mapping match {
         case None => name.ar( default )
         case Some( m ) => {
            val outBus = m.mapBus // .get
            require( outBus.rate == audio )
            // stupidly we have two arguments...
            name.ar( default, List.fill( outBus.numChannels - 1 )( default ): _* )
         }
      }
   }
}

//class ParamStringImpl( val name: String, val default: Option[ String ])
//extends ProcParamString

class ParamAudioInputImpl( val name: String, val default: Option[ RichAudioBus ], val physical: Boolean )
extends ProcParamAudioInput {
   private def resolveBusAndIncludeParam : RichAudioBus = {
      val p    = Proc.local
      val pb   = ProcEntryBuilder.local
      implicit val tx   = pb.tx
      val ain = p.audioInput( name )
      val b = ain.bus.getOrElse({
         if( ain.synthetic ) {
            // YYY we might solves this differently, by rolling back the transaction
            // and somehow requesting e.out.proc to play in the retried transaction?
            // ... XXX this may also be dangerous when we implement control outputs
            // as we could produce cyclic calls here
            // ... XXX another idea would be to create something like e.out.proc.prepare
            // so that the graph is there, but no actual synth yet created??
            val e = ain.edges.headOption.getOrElse( error( "Bus " + ain + " not connected" ))
            if( !e.out.proc.isPlaying ) e.out.proc.play
            // now retry
            ain.bus.getOrElse( error( "Bus " + ain + " must be specified" ))
//            val res = RichBus.audio( p.server, 1 )
//            ain.bus = Some( res )
//            res
         } else if( physical ) {
            val res = RichBus.soundIn( p.server, 1 )
            ain.bus = Some( res )
            res
         } else pError( name ) // throw e
      })
      pb.includeParam( this ) // important: must be after ain.bus_= NO NOT TRUE - DOES NOT MATTER
      b
   }

   def ar : GE = {
      import synth._
      val b = resolveBusAndIncludeParam
      In.ar( name.kr, b.numChannels )
   }

   def numChannels : Int = {
      val b = resolveBusAndIncludeParam
      b.numChannels
   }

   private def pError( name: String ) = throw new ProcParamUnspecifiedException( name )
}

class ParamAudioOutputImpl( val name: String, val default: Option[ RichAudioBus ], val physical: Boolean )
extends ProcParamAudioOutput {
   private def resolveBusAndIncludeParam( numChannels: Int ) : RichAudioBus = {
      val p             = Proc.local
      val pb            = ProcEntryBuilder.local
      implicit val tx   = pb.tx
      val aout          = p.audioOutput( name )
      val b             = aout.bus getOrElse {
         if( aout.synthetic ) {
            val res = RichBus.audio( p.server, numChannels )
            aout.bus = Some( res )
            res
         } else if( physical ) {
            val res = RichBus.soundOut( p.server, numChannels )
            aout.bus = Some( res )
            res
         } else {
            aout.bus = default
            default.getOrElse( pError( name ))
         }
      }
      pb.includeParam( this ) // important: must be after aout.bus_= NO NOT TRUE DOES NOT MATTER
      b
   }

   def ar( sig: GE ) : GE = {
      import synth._
      val numCh   = sig.numOutputs
      val b       = resolveBusAndIncludeParam( numCh )
      val sig2: GE = if( b.numChannels == numCh ) {
         sig
      } else {
         println( "WARNING: Coercing output signal from " + numCh + " into " + b.numChannels + " channels" )
         val chans   = sig.outputs
         List.tabulate( b.numChannels )( ch => chans( ch % numCh ))
      }
      Out.ar( name.kr, sig2 )
   }

   def numChannels_=( n: Int ) {
      val b = resolveBusAndIncludeParam( n )
      if( b.numChannels != n ) {
         error( "Output bus already has " + b.numChannels + " channels. Cannot change to  " + n )
      }
   }

   private def pError( name: String ) = throw new ProcParamUnspecifiedException( name )
}
