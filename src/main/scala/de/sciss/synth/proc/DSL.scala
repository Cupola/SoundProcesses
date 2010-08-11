/*
 *  DSL.scala
 *  (SoundProcesses)
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

package de.sciss.synth.proc

import reflect.ClassManifest
import java.io.{ IOException }
import de.sciss.synth.{GE}
import de.sciss.synth.io.AudioFileSpec

/**
 *    @version 0.15, 11-Aug-10
 */
object DSL {
   private val cmGE     = ClassManifest.fromClass( classOf[ GE ])
   private val cmUnit   = ClassManifest.Unit

   // ---- scope : anywhere ----

   @throws( classOf[ IOException ])
   def audioFileSpec( path: String ) : AudioFileSpec = AudioFileCache.spec( path )

   // ---- scope : outside ----

   /**
    * Generates a sound generating process factory
    * with the given name and described through
    * the given code block.
    */
   def gen( name: String )( thunk: => Unit )( implicit tx: ProcTxn ) : ProcFactory = {
      val res = ProcFactoryBuilder.gen( name )( thunk )
      ProcDemiurg.addFactory( res )
      res
   }

   /**
    * Generates a sound filtering (transformation) process factory
    * with the given name and described through
    * the given code block.
    */
   def filter( name: String )( thunk: => Unit )( implicit tx: ProcTxn ) : ProcFactory = {
      val res = ProcFactoryBuilder.filter( name )( thunk )
      ProcDemiurg.addFactory( res )
      res
   }

   /**
    * Generates a sound diffusing process factory
    * with the given name and described through
    * the given code block.
    */
   def diff( name: String )( thunk: => Unit )( implicit tx: ProcTxn ) : ProcFactory = {
      val res = ProcFactoryBuilder.diff( name )( thunk )
      ProcDemiurg.addFactory( res )
      res
   }

   /**
    * Performs a code block where all
    * transitions are considered instantly.
    */
   def instant[ T ]( thunk: => T )( implicit tx: ProcTxn ) = {
      tx.withTransition( Instant )( thunk )
   }

   /**
    * Performs a code block with transitional
    * semantics taken from a crossfade of
    * the given duration (in seconds).
    */
   def xfade[ T ]( dur: Double )( thunk: => T )( implicit tx: ProcTxn ) = {
      val trns = XFade( tx.time, dur )
      tx.withTransition( trns )( thunk )
   }

   /**
    * Performs a code block with transitional
    * semantics taken from gliding for
    * the given duration (in seconds).
    */
   def glide[ T ]( dur: Double )( thunk: => T )( implicit tx: ProcTxn ) = {
      val trns = Glide( tx.time, dur ) 
      tx.withTransition( trns )( thunk )
   }

   // ---- scope : gen (ProcFactoryBuilder) ----

   /**
    * The scope is inside a `gen { }`, `filter { }` or `diff { }` block.
    */
   def pScalar( name: String, spec: ParamSpec = ParamSpec(), default: Double ) : ProcParamScalar =
      ProcFactoryBuilder.local.pScalar( name, spec, default )
   /**
    * The scope is inside a `gen { }`, `filter { }` or `diff { }` block.
    */
   def pControl( name: String, spec: ParamSpec = ParamSpec(), default: Double ) : ProcParamControl =
      ProcFactoryBuilder.local.pControl( name, spec, default )
   /**
    * The scope is inside a `gen { }`, `filter { }` or `diff { }` block.
    */
   def pAudio( name: String, spec: ParamSpec = ParamSpec(), default: Double ) : ProcParamAudio =
      ProcFactoryBuilder.local.pAudio( name, spec, default )
//   def pString( name: String, default: Option[ String ] = None ) : ProcParamString =
//      ProcFactoryBuilder.local.pString( name, default )
   /**
    * The scope is inside a `gen { }`, `filter { }` or `diff { }` block.
    */
   def pAudioIn( name: String, default: Option[ RichAudioBus ] = None ) : ProcParamAudioInput =
      ProcFactoryBuilder.local.pAudioIn( name, default )
   /**
    * The scope is inside a `gen { }`, `filter { }` or `diff { }` block.
    */
   def pAudioOut( name: String, default: Option[ RichAudioBus ] = None ) : ProcParamAudioOutput =
      ProcFactoryBuilder.local.pAudioOut( name, default )

   /**
    * Creates the main entry point of the proc by providing a
    * synthdef builder. Typically this is called for ProcGen
    * anatomy, as the thunk does not take an input argument,
    * but it might nevertheless be used also in a ProcFilter
    * context.
    *
    * The scope is inside a `gen { }`, `filter { }` or `diff { }` block.
    */
   def graph( thunk: => GE ) : ProcGraph = {
      val b = ProcFactoryBuilder.local
      b.anatomy match {
         case ProcGen    => b.graphOut( () => thunk )
         case ProcFilter => b.graphOut( () => thunk ) // b.graphInOut( in => thunk )
         case ProcDiff   => b.graph( () => thunk )
      }
   }

   /**
    * Creates the main entry point of the proc by providing a
    * synthdef builder. The proc can have either a ProcFilter
    * or a ProcDiff anatomy. Since a ProcGen does not provide
    * a default input, calling this method for a ProcGen proc
    * will throw a runtime error.
    *
    * The scope is inside a `filter { }` or `diff { }` block.
    */
   def graph( fun: GE => GE ) : ProcGraph = {
      val b = ProcFactoryBuilder.local
      b.anatomy match {
         case ProcGen    => error( "Generators do not have a default input" )
         case ProcFilter => b.graphInOut( fun )
         case ProcDiff   => b.graphIn( fun )
      }
   }

   def idle( thunk: => Int ) : ProcIdle = {
      val b = ProcFactoryBuilder.local
//      b.idle( () => thunk )
      b.anatomy match {
         case ProcGen    => b.idleOut( () => thunk )
         case ProcFilter => b.idleOut( () => thunk ) // b.idleInOut( in => thunk )
         case ProcDiff   => error( "Diffusions do not have a default output" )
      }
   }

   def idle( fun: Int => Any ) : ProcIdle = {
      val b = ProcFactoryBuilder.local
      b.anatomy match {
         case ProcGen    => error( "Generators do not have a default input" )
         case ProcFilter => b.idleInOut( fun )
         case ProcDiff   => b.idleIn( fun )
      }
   }

   def idle : ProcIdle = {
      val b = ProcFactoryBuilder.local
      b.anatomy match {
         case ProcGen    => error( "Generators require explicit output" )
         case ProcFilter => idle( (i: Int) => i )
         case ProcDiff   => b.idle
      }
   }

   // ---- scope : graph (ProcGraphBuilder) ----

   /**
    * The scope is inside a `graph { }` block.
    */
   def bufCue( path: String, startFrame: Long = 0L ) : ProcBuffer =
      ProcGraphBuilder.local.bufCue( path, startFrame )
   /**
    * The scope is inside a `graph { }` block.
    */
   def bufEmpty( numFrames: Int, numChannels: Int = 1 ) : ProcBuffer =
      ProcGraphBuilder.local.bufEmpty( numFrames, numChannels )

   /**
    * Returns the sample-rate of the server on which the enclosing
    * proc is running. The scope is inside a `graph { }` block.
    */
   def sampleRate : Double = Proc.local.server.sampleRate

   implicit def procToAudioInput( p: Proc ) : ProcAudioInput   = p.audioInput( "in" )
   implicit def procToAudioOutput( p: Proc ) : ProcAudioOutput = p.audioOutput( "out" )
   implicit def procToAudioInOut( p: Proc ) : (ProcAudioInput, ProcAudioOutput) =
      p.audioInput( "in" ) -> p.audioOutput( "out" )
}

trait ProcFactory extends ProcSpec {
   def make( implicit tx: ProcTxn ) : Proc
}
