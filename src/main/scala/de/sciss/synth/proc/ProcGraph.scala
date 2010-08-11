/*
 *  ProcGraph.scala
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

package de.sciss.synth.proc

trait ProcGraph extends ProcEntry
object ProcGraphBuilder extends ThreadLocalObject[ ProcGraphBuilder ] {
   override def use[ U ]( obj: ProcGraphBuilder )( thunk: => U ) : U = {
      val old = tl.get()
      tl.set( obj )
      try {
         ProcEntryBuilder.use( obj )( thunk )  // XXX bit stupid the nesting
      } finally {
         tl.set( old )
      }
   }
}

trait ProcGraphBuilder extends ProcEntryBuilder {
   def includeBuffer( b: ProcBuffer ) : Unit
   def bufCue( path: String, startFrame: Long ) : ProcBuffer
   def bufEmpty( numFrames: Int, numChannels: Int ) : ProcBuffer
}
