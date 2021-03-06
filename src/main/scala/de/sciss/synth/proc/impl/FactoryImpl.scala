/*
 *  FactoryImpl.scala
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

import de.sciss.synth.Server
import collection.immutable.{ IndexedSeq => IIdxSeq }
import de.sciss.synth.proc.{ Proc, ProcAnatomy, ProcDemiurg, ProcEntry, ProcFactory, ProcGraph, ProcParam,
   ProcParamAudioInput, ProcParamAudioOutput, ProcTxn, Ref }

/**
 *    @version 0.11, 20-Jul-10
 */
class FactoryImpl( val name: String, val anatomy: ProcAnatomy,
                   val entry: ProcEntry,
                   val paramMap: Map[ String, ProcParam ],
                   val params: IIdxSeq[ ProcParam ],
                   val pAudioIns: IIdxSeq[ ProcParamAudioInput ],
                   val pAudioOuts: IIdxSeq[ ProcParamAudioOutput ])
extends ProcFactory {
   private val count = Ref( 1 )

   def make( implicit tx: ProcTxn ) : Proc = {
      val cnt = count()
      count.set( cnt + 1 )
      val res = new ProcImpl( this, cnt, Server.default )
      ProcDemiurg.addVertex( res )
//      res.init
      res
   }

   def param( name: String ) : ProcParam = paramMap( name )

   lazy val bypassGraph : ProcGraph = new GraphImpl( () => {
      val p = Proc.local
      val sig = p.param( "in" ).asInstanceOf[ ProcParamAudioInput ].ar 
      p.param( "out" ).asInstanceOf[ ProcParamAudioOutput ].ar( sig )
   })
   
   override def toString = "fact(" + name + ")"
}
