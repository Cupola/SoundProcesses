/*
 *  BufferImpl.scala
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

import de.sciss.synth.proc._
import de.sciss.synth._
import de.sciss.synth.io.AudioFile

/**
 *    @version 0.11, 19-Jul-10
 */
abstract class BufferImpl extends ProcBuffer {
   protected def uniqueID : Int
   def controlName   = "buf$" + uniqueID
}

class BufferEmptyImpl( val uniqueID: Int, numFrames: Int, val numChannels: Int ) extends BufferImpl {
   def create( server: Server )( implicit tx: ProcTxn ) : RichBuffer = {
      val b = Buffer( server )
      val rb = RichBuffer( b )
      rb.alloc( numFrames, numChannels )
      rb.zero // XXX actually needed? to satisfy ProcTxn yes, but technically...?
      rb
   }
}


class BufferCueImpl( val uniqueID: Int, path: String, startFrame: Long ) extends BufferImpl {
   def create( server: Server )( implicit tx: ProcTxn ) : RichBuffer = {
      val b = Buffer( server )
      val rb = RichBuffer( b )
      rb.alloc( 32768, numChannels )
      rb.cue( path, startFrame.toInt ) // ( tx )
      rb
   }

   def numChannels : Int = {
      try { // XXX should call includeBuffer ?
         val spec = AudioFileCache.spec( path ) // ( ProcGraphBuilder.local.tx )
         spec.numChannels
      } catch {
         case e => e.printStackTrace()
         1  // XXX what should we do? --> FAIL
      }
   }
}
