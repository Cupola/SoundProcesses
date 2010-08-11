/*
 *  ProcParam.scala
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

import de.sciss.synth.GE

/**
 *    @version 0.12, 01-Jul-10
 */
sealed trait ProcParam {
//   type t = T
   def name : String
//   def default : Option[ T ]
}

class ProcParamUnspecifiedException( name: String )
extends RuntimeException( "Proc parameter unspecified: "  + name )
                                                                                                             
trait ProcParamFloat extends ProcParam {
   def spec : ParamSpec
   def default : Double
}

trait ProcParamScalar extends ProcParamFloat {
   def ir : GE
   def v : Double
}

trait ProcParamControl extends ProcParamFloat {
   def kr : GE
}

trait ProcParamAudio extends ProcParamFloat {
   def ar : GE
}

sealed trait ProcParamAudioBus extends ProcParam {
   // ---- scope: graph ----
// currently disabled:
//   def numChannels : Int

   def physical: Boolean
}

trait ProcParamAudioInput extends ProcParamAudioBus {
   // ---- scope: graph ----
   def ar : GE
   def numChannels : Int
}

trait ProcParamAudioOutput extends ProcParamAudioBus {
   // ---- scope: graph ----
   def ar( sig: GE ) : GE
//   def numChannels : Int
   def numChannels_=( n: Int ) : Unit
}
