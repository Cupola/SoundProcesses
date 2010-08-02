/*
 *  AudioFileCache.scala
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

import collection.mutable.{ HashMap => MHashMap }
import java.io.{File, IOException}
import de.sciss.synth.io.{AudioFile, AudioFileSpec}

/**
 *    @version 0.10, 02-Aug-10
 */
object AudioFileCache {
   private val sync  = new AnyRef
   private val map   = MHashMap.empty[ File, Entry ]

   @throws( classOf[ IOException ])
   def spec( path: String ) : AudioFileSpec = sync.synchronized {
      val f       = new File( path )
      val mod     = f.lastModified()
      map.get( f ) match {
         case Some( entry ) if( entry.modified == mod ) => entry.spec
         case _ => {
            val spec    = AudioFile.readSpec( f )
            val entry   = Entry( mod, spec )
            map += f -> entry
            spec
         }
      }
   }

   def clear : Unit = sync.synchronized { map.clear }

   private case class Entry( modified: Long, spec: AudioFileSpec )
}