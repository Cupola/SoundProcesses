/*
 *  BusNodeSetter.scala
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

import de.sciss.synth.{Bus, ControlBus, AudioBus}

/**
 *    @version 0.11, 03-Aug-10
 */
trait BusNodeSetter {
   def add( implicit tx: ProcTxn )     : BusNodeSetter
   def remove( implicit tx: ProcTxn )  : BusNodeSetter
   def bus: RichBus[ _ ]
}

trait AudioBusNodeSetter extends BusNodeSetter {
   def add( implicit tx: ProcTxn )     : AudioBusNodeSetter
   def remove( implicit tx: ProcTxn )  : AudioBusNodeSetter
   def migrateTo( newBus: RichAudioBus )( implicit tx: ProcTxn ) : AudioBusNodeSetter
   def bus: RichAudioBus
}

trait ControlBusNodeSetter extends BusNodeSetter {
   def add( implicit tx: ProcTxn )     : ControlBusNodeSetter
   def remove( implicit tx: ProcTxn )  : ControlBusNodeSetter
   def migrateTo( newBus: RichControlBus )( implicit tx: ProcTxn ) : ControlBusNodeSetter
   def bus: RichControlBus
}

object BusNodeSetter {
   def reader( name: String, rb: RichAudioBus, rn: RichNode ) : AudioBusNodeSetter = new AudioReaderImpl( name, rb, rn )
   def writer( name: String, rb: RichAudioBus, rn: RichNode ) : AudioBusNodeSetter = new AudioWriterImpl( name, rb, rn )
   def readerWriter( name: String, rb: RichAudioBus, rn: RichNode ) : AudioBusNodeSetter =
      new AudioReaderWriterImpl( name, rb, rn )
   def mapper( name: String, rb: RichAudioBus, rn: RichNode ) : AudioBusNodeSetter = new AudioMapperImpl( name, rb, rn )

   def reader( name: String, rb: RichControlBus, rn: RichNode ) : ControlBusNodeSetter = new ControlReaderImpl( name, rb, rn )
   def writer( name: String, rb: RichControlBus, rn: RichNode ) : ControlBusNodeSetter = new ControlWriterImpl( name, rb, rn )
   def readerWriter( name: String, rb: RichControlBus, rn: RichNode ) : ControlBusNodeSetter =
      new ControlReaderWriterImpl( name, rb, rn )
   def mapper( name: String, rb: RichControlBus, rn: RichNode ) : ControlBusNodeSetter = new ControlMapperImpl( name, rb, rn )

   private trait ImplLike extends BusNodeSetter {
      val added = Ref( false )
      def name: String
      def rn: RichNode
   }

   private trait AudioSetterLike extends ImplLike {
      def busChanged( b: AudioBus )( implicit tx: ProcTxn ) {
         rn.setIfOnline( name -> b.index )
      }
   }

   private trait ControlSetterLike extends ImplLike {
      def busChanged( b: ControlBus )( implicit tx: ProcTxn ) {
         rn.setIfOnline( name -> b.index )
      }
   }

   private trait AudioMapperLike extends ImplLike {
      def busChanged( b: AudioBus )( implicit tx: ProcTxn ) {
         rn.mapan( true, name -> b )
      }
   }

   private trait ControlMapperLike extends ImplLike {
      def busChanged( b: ControlBus )( implicit tx: ProcTxn ) {
         rn.mapn( true, name -> b )
      }
   }

   private abstract class AbstractAudioImpl
   extends ImplLike with RichAudioBus.User with AudioBusNodeSetter {
      def migrateTo( newBus: RichAudioBus )( implicit tx: ProcTxn ) : AudioBusNodeSetter = {
         require( newBus.numChannels == bus.numChannels )
         val wasAdded = added()
         if( wasAdded ) remove
         val res = newInstance( name, newBus, rn )
         if( wasAdded ) res.add
         res
      }

      def newInstance( name: String, bus: RichAudioBus, rn: RichNode ) : AudioBusNodeSetter
   }

   private abstract class AbstractControlImpl
   extends ImplLike with RichControlBus.User with ControlBusNodeSetter {
      def migrateTo( newBus: RichControlBus )( implicit tx: ProcTxn ) : ControlBusNodeSetter = {
         require( newBus.numChannels == bus.numChannels )
         val wasAdded = added()
         if( wasAdded ) remove
         val res = newInstance( name, newBus, rn )
         if( wasAdded ) res.add
         res
      }

      def newInstance( name: String, bus: RichControlBus, rn: RichNode ) : ControlBusNodeSetter
   }

   private abstract class AbstractAudioReader extends AbstractAudioImpl {
      def add( implicit tx: ProcTxn ) = {
         val wasAdded = added.swap( true )
         if( wasAdded ) error( "Was already added : " + this )
         bus.addReader( this )
         this
      }

      def remove( implicit tx: ProcTxn ) = {
         val wasAdded = added.swap( false )
         if( wasAdded ) bus.removeReader( this )
         this
      }
   }

   private abstract class AbstractControlReader extends AbstractControlImpl {
      def add( implicit tx: ProcTxn ) = {
         val wasAdded = added.swap( true )
         if( wasAdded ) error( "Was already added : " + this )
         bus.addReader( this )
         this
      }

      def remove( implicit tx: ProcTxn ) = {
         val wasAdded = added.swap( false )
         if( wasAdded ) bus.removeReader( this )
         this
      }
   }

   /*
    *    Careful not use case classes here, as multiple readers / writers for the
    *    same combo might be wanted in a read / write set!
    */
   private class AudioReaderImpl( val name: String, val bus: RichAudioBus, val rn: RichNode )
   extends AbstractAudioReader with AudioSetterLike {
      def newInstance( name: String, bus: RichAudioBus, rn: RichNode ) = reader( name, bus, rn )
   }

   private class ControlReaderImpl( val name: String, val bus: RichControlBus, val rn: RichNode )
   extends AbstractControlReader with ControlSetterLike {
      def newInstance( name: String, bus: RichControlBus, rn: RichNode ) = reader( name, bus, rn )
   }

   private class AudioMapperImpl( val name: String, val bus: RichAudioBus, val rn: RichNode )
   extends AbstractAudioReader with AudioMapperLike {
      def newInstance( name: String, bus: RichAudioBus, rn: RichNode ) = mapper( name, bus, rn )
   }

   private class ControlMapperImpl( val name: String, val bus: RichControlBus, val rn: RichNode )
   extends AbstractControlReader with ControlMapperLike {
      def newInstance( name: String, bus: RichControlBus, rn: RichNode ) = mapper( name, bus, rn )
   }

   private abstract class AbstractAudioWriter extends AbstractAudioImpl {
      def add( implicit tx: ProcTxn ) = {
         val wasAdded = added.swap( true )
         if( wasAdded ) error( "Was already added : " + this )
         bus.addWriter( this )
         this
      }

      def remove( implicit tx: ProcTxn ) = {
         val wasAdded = added.swap( false )
         if( wasAdded ) bus.removeWriter( this )
         this
      }
   }

   private abstract class AbstractControlWriter extends AbstractControlImpl {
      def add( implicit tx: ProcTxn ) = {
         val wasAdded = added.swap( true )
         if( wasAdded ) error( "Was already added : " + this )
         bus.addWriter( this )
         this
      }

      def remove( implicit tx: ProcTxn ) = {
         val wasAdded = added.swap( false )
         if( wasAdded ) bus.removeWriter( this )
         this
      }
   }

   /*
    *    Careful not use case classes here, as multiple readers / writers for the
    *    same combo might be wanted in a read / write set!
    */
   private class AudioWriterImpl( val name: String, val bus: RichAudioBus, val rn: RichNode )
   extends AbstractAudioWriter with AudioSetterLike {
      def newInstance( name: String, bus: RichAudioBus, rn: RichNode ) = writer( name, bus, rn )
   }

   private class ControlWriterImpl( val name: String, val bus: RichControlBus, val rn: RichNode )
   extends AbstractControlWriter with ControlSetterLike {
      def newInstance( name: String, bus: RichControlBus, rn: RichNode ) = writer( name, bus, rn )
   }

   /*
    *    Careful not use case classes here, as multiple readers / writers for the
    *    same combo might be wanted in a read / write set!
    */
   private class AudioReaderWriterImpl( val name: String, val bus: RichAudioBus, val rn: RichNode )
   extends AbstractAudioImpl with AudioSetterLike {
      object dummy extends RichAudioBus.User {
         def busChanged( b: AudioBus )( implicit tx: ProcTxn ) {}
      }

      def add( implicit tx: ProcTxn ) = {
         val wasAdded = added.swap( true )
         if( wasAdded ) error( "Was already added : " + this )
         bus.addReader( this )
         bus.addWriter( dummy )
         this
      }

      def remove( implicit tx: ProcTxn ) = {
         val wasAdded = added.swap( false )
         if( wasAdded ) {
            bus.removeWriter( dummy )
            bus.removeReader( this )
         }
         this
      }

      def newInstance( name: String, bus: RichAudioBus, rn: RichNode ) = readerWriter( name, bus, rn )
   }

   private class ControlReaderWriterImpl( val name: String, val bus: RichControlBus, val rn: RichNode )
   extends AbstractControlImpl with ControlSetterLike {
      object dummy extends RichControlBus.User {
         def busChanged( b: ControlBus )( implicit tx: ProcTxn ) {}
      }

      def add( implicit tx: ProcTxn ) = {
         val wasAdded = added.swap( true )
         if( wasAdded ) error( "Was already added : " + this )
         bus.addReader( this )
         bus.addWriter( dummy )
         this
      }

      def remove( implicit tx: ProcTxn ) = {
         val wasAdded = added.swap( false )
         if( wasAdded ) {
            bus.removeWriter( dummy )
            bus.removeReader( this )
         }
         this
      }

      def newInstance( name: String, bus: RichControlBus, rn: RichNode ) = readerWriter( name, bus, rn )
   }
}