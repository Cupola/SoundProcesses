/*
 *  RunningGraphImpl.scala
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

import de.sciss.synth.AudioBus
import collection.immutable.{ Seq => ISeq }
import de.sciss.synth.proc._

/**
 *    @version 0.11, 03-Aug-10
 */
class RunningGraphImpl( rs: RichSynth, _accMap: Map[ String, AudioBusPlayerImpl ])
extends ProcRunning {

   private val accMapRef = Ref( _accMap )

   def anchorNode( implicit tx: ProcTxn ) : RichNode = rs

   def stop( implicit tx: ProcTxn ) = {
      tx transit match {
         case Instant      => rs.free()
         case glide: Glide => error( "NOT YET SUPPORTED" )
         case xfade: XFade => // nada. Proc calls setGroup already
      }
      accMapRef().foreach( _._2.player.stop )
   }

   def controlChanged( ctrl: ProcControl, newValue: ControlValue )( implicit tx: ProcTxn ) {
      val acc  = accMapRef()
      val name = ctrl.name
      acc.get( name ) map { abp =>
         newValue.mapping match {
            case None => {
               abp.setter.remove
               rs.set( true, ctrl.name -> newValue.current.toFloat )
               accMapRef.set( acc - name )
            }
            case Some( m ) => {
               val newABus = m.mapBus match {
                  case rab: RichAudioBus => rab
                  case _ => error( "NOT YET IMPLEMENTED" )
               }
               accMapRef.set( acc + (name -> abp.copy( setter = abp.setter.migrateTo( newABus ))))
            }
         }
      }  getOrElse {
         newValue.mapping match {
            case None => rs.set( true, ctrl.name -> newValue.current.toFloat )
            case Some( m ) => {
               val newABus = m.mapBus match {
                  case rab: RichAudioBus => rab
                  case _ => error( "NOT YET IMPLEMENTED" )
               }
               val abp = AudioBusPlayerImpl( m, rs.map( newABus -> name ))
               accMapRef.set( acc + (name -> abp) )
            }
         }
      }
   }

   def busChanged( pbus: ProcAudioBus, newBus: Option[ RichAudioBus ])( implicit tx: ProcTxn ) {
      val acc  = accMapRef()
      val name = pbus.name
      acc.get( name ) foreach { abp =>
         val newABus = newBus.getOrElse( error( "Bus is used and hence must be defined : " + name ))
         accMapRef.set( acc + (name -> abp.copy( setter = abp.setter.migrateTo( newABus ))))
      }
   }

   def setGroup( g: RichGroup )( implicit tx: ProcTxn ) {
      rs.moveToHead( true, g )
   }
}

