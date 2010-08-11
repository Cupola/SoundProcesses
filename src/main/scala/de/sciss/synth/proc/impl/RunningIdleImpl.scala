/*
 *  RunningIdleImpl.scala
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

import de.sciss.synth.proc.{ ControlValue, Glide, Instant, ProcAudioBus, ProcControl, ProcRunning, ProcTxn, Ref,
   RichAudioBus, RichGroup, RichNode, RichSynth, XFade }

/**
 *    @version 0.11, 03-Aug-10
 */
class RunningIdleImpl( _anchor: RichNode, _accMap: Map[ String, AudioBusPlayerImpl ])
extends ProcRunning {

   private val anchorRef = Ref( _anchor )
   private val accMapRef = Ref( _accMap )

   def anchorNode( implicit tx: ProcTxn ) : RichNode = anchorRef()

   def stop( implicit tx: ProcTxn ) = {
//      tx transit match {
//         case Instant      => rs.free()
//         case glide: Glide => error( "NOT YET SUPPORTED" )
//         case xfade: XFade => // nada. Proc calls setGroup already
//      }
      accMapRef().foreach( _._2.player.stop )
      // XXX forward to children
   }

   def controlChanged( ctrl: ProcControl, newValue: ControlValue )( implicit tx: ProcTxn ) {
      // XXX forward to children
   }

   def busChanged( pbus: ProcAudioBus, newBus: Option[ RichAudioBus ])( implicit tx: ProcTxn ) {
      val acc  = accMapRef()
      val name = pbus.name
      acc.get( name ) foreach { abp =>
         val newABus = newBus.getOrElse( error( "Bus is used and hence must be defined : " + name ))
         // migrateTo takes care of rejecting numChannels change
         accMapRef.set( acc + (name -> abp.copy( setter = abp.setter.migrateTo( newABus ))))
      }
      // XXX forward to children
   }

   def setGroup( g: RichGroup )( implicit tx: ProcTxn ) {
      anchorRef.set( g )
      // XXX forward to children
   }
}

