/*
 *  ProcBuffer.scala
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

import de.sciss.synth
import de.sciss.synth.{Server, GE}

trait ProcBuffer {
   def controlName : String
   private[proc] def create( server: Server )( implicit tx: ProcTxn ) : RichBuffer

   private[proc] def disposeWith( rb: RichBuffer, rs: RichSynth ) {
      rs.synth.onEnd { rb.server ! rb.buf.closeMsg( rb.buf.freeMsg )} // XXX update RichBuffer fields !
   }

   // ---- scope : graph (ProcGraphBuilder) ----

//   def id : GE
   def id : GE = {
      import synth._
      ProcGraphBuilder.local.includeBuffer( this )
      controlName.kr
   }

   def numChannels : Int
}
