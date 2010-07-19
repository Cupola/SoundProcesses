/*
 *  SoundProcesses.scala
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

object SoundProcesses {
   val name          = "SoundProcesses"
   val version       = 0.12
   val copyright     = "(C)opyright 2010 Hanns Holger Rutz"
   def versionString = (version + 0.001).toString.substring( 0, 4 )

   def main( args: Array[ String ]) {
      printInfo
      System.exit( 1 )
   }

   def printInfo {
      println( "\n" + name + " v" + versionString + "\n" + copyright + ". All rights reserved.\n" +
         "This is a library which cannot be executed directly.\n" )
   }

   def test {
      import DSL._
      import de.sciss.synth._
      import de.sciss.synth.ugen._

      ProcTxn.atomic { implicit tx =>
         val achil = filter( "Achil") {
            val pspeed  = pAudio( "speed", ParamSpec( 0.125, 2.3511, ExpWarp ), 0.5 )
            val pmix    = pAudio( "mix", ParamSpec( 0, 1 ), 1 )

            graph { in =>
               val speed	   = Lag.ar( pspeed.ar, 0.1 )
               val numFrames  = sampleRate.toInt
               val numChannels= in.numOutputs
               val buf        = bufEmpty( numFrames, numChannels )
               val bufID      = buf.id
               val writeRate  = BufRateScale.kr( bufID )
               val readRate   = writeRate * speed
               val readPhasor = Phasor.ar( 0, readRate, 0, numFrames )
               val read			= BufRd.ar( numChannels, bufID, readPhasor, 0, 4 )
               val writePhasor= Phasor.ar( 0, writeRate, 0, numFrames )
               val old			= BufRd.ar( numChannels, bufID, writePhasor, 0, 1 )
               val wet0 		= SinOsc.ar( 0, ((readPhasor - writePhasor).abs / numFrames * math.Pi) )
               val dry			= 1 - wet0.squared
               val wet			= 1 - (1 - wet0).squared
               BufWr.ar( (old * dry) + (in * wet), bufID, writePhasor )
               LinXFade2.ar( in, read, pmix.ar * 2 - 1 )
            }
         }
      }
   }
}