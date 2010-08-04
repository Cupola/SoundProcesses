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
   def add( implicit tx: ProcTxn ) : Unit
   def remove( implicit tx: ProcTxn ) : Unit
   def bus: RichBus
   def node: RichNode
   def controlName: String
//   def migrateTo( newBus: RichBus )( implicit tx: ProcTxn ) : BusNodeSetter
}

trait AudioBusNodeSetter extends BusNodeSetter {
   def bus: RichAudioBus
   def migrateTo( newBus: RichAudioBus )( implicit tx: ProcTxn ) : AudioBusNodeSetter
}

trait ControlBusNodeSetter extends BusNodeSetter {
   def bus: RichControlBus
   def migrateTo( newBus: RichControlBus )( implicit tx: ProcTxn ) : ControlBusNodeSetter
}

object BusNodeSetter {
   def reader( controlName: String, bus: RichAudioBus, node: RichNode ) : AudioBusNodeSetter =
      new AudioReaderImpl( controlName, bus, node )

   def reader( controlName: String, bus: RichControlBus, node: RichNode ) : ControlBusNodeSetter =
      new ControlReaderImpl( controlName, bus, node )

   def writer( controlName: String, bus: RichAudioBus, node: RichNode ) : AudioBusNodeSetter =
      new AudioWriterImpl( controlName, bus, node )

   def writer( controlName: String, bus: RichControlBus, node: RichNode ) : ControlBusNodeSetter =
      new ControlWriterImpl( controlName, bus, node )

   def readerWriter( controlName: String, bus: RichAudioBus, node: RichNode ) : AudioBusNodeSetter =
      new AudioReaderWriterImpl( controlName, bus, node )

   def readerWriter( controlName: String, bus: RichControlBus, node: RichNode ) : ControlBusNodeSetter =
      new ControlReaderWriterImpl( controlName, bus, node )

   def mapper( controlName: String, bus: RichAudioBus, node: RichNode ) : AudioBusNodeSetter =
      new AudioMapperImpl( controlName, bus, node )

   def mapper( controlName: String, bus: RichControlBus, node: RichNode ) : ControlBusNodeSetter =
      new ControlMapperImpl( controlName, bus, node )

   private trait ImplLike extends BusNodeSetter {
      val added = Ref( false )
   }

   private trait AudioSetterLike extends ImplLike {
      def busChanged( b: AudioBus )( implicit tx: ProcTxn ) {
         node.setIfOnline( controlName -> b.index )
      }
   }

   private trait ControlSetterLike extends ImplLike {
      def busChanged( b: ControlBus )( implicit tx: ProcTxn ) {
         node.setIfOnline( controlName -> b.index )
      }
   }

   private trait AudioMapperLike extends ImplLike {
      def busChanged( b: AudioBus )( implicit tx: ProcTxn ) {
         node.mapan( true, controlName -> b )
      }
   }

   private trait ControlMapperLike extends ImplLike {
      def busChanged( b: ControlBus )( implicit tx: ProcTxn ) {
         node.mapn( true, controlName -> b )
      }
   }

   private abstract class AbstractAudioImpl
   extends ImplLike with RichAudioBus.User with AudioBusNodeSetter {
      def migrateTo( newBus: RichAudioBus )( implicit tx: ProcTxn ) : AudioBusNodeSetter = {
         require( newBus.numChannels == bus.numChannels )
         val wasAdded = added()
         if( wasAdded ) remove
         val res = newInstance( newBus )
         if( wasAdded ) res.add
         res
      }

      def newInstance( newBus: RichAudioBus ) : AudioBusNodeSetter
   }

   private abstract class AbstractControlImpl
   extends ImplLike with RichControlBus.User with ControlBusNodeSetter {
      def migrateTo( newBus: RichControlBus )( implicit tx: ProcTxn ) : ControlBusNodeSetter = {
         require( newBus.numChannels == bus.numChannels )
         val wasAdded = added()
         if( wasAdded ) remove
         val res = newInstance( newBus )
         if( wasAdded ) res.add
         res
      }

      def newInstance( newBus: RichControlBus ) : ControlBusNodeSetter
   }

   private abstract class AbstractAudioReader extends AbstractAudioImpl {
      def add( implicit tx: ProcTxn ) {
         val wasAdded = added.swap( true )
         if( wasAdded ) error( "Was already added : " + this )
         bus.addReader( this )
      }

      def remove( implicit tx: ProcTxn ) {
         val wasAdded = added.swap( false )
         if( wasAdded ) bus.removeReader( this )
      }
   }

   private abstract class AbstractControlReader extends AbstractControlImpl {
      def add( implicit tx: ProcTxn ) {
         val wasAdded = added.swap( true )
         if( wasAdded ) error( "Was already added : " + this )
         bus.addReader( this )
      }

      def remove( implicit tx: ProcTxn ) {
         val wasAdded = added.swap( false )
         if( wasAdded ) bus.removeReader( this )
      }
   }

   /*
    *    Careful not use case classes here, as multiple readers / writers for the
    *    same combo might be wanted in a read / write set!
    */
   private class AudioReaderImpl( val controlName: String, val bus: RichAudioBus, val node: RichNode )
   extends AbstractAudioReader with AudioSetterLike {
      def newInstance( newBus: RichAudioBus ) = reader( controlName, newBus, node )
      override def toString = "BusNodeSetter.reader(" + controlName + ", " + bus + ", " + node + ")"
   }

   private class ControlReaderImpl( val controlName: String, val bus: RichControlBus, val node: RichNode )
   extends AbstractControlReader with ControlSetterLike {
      def newInstance( newBus: RichControlBus ) = reader( controlName, newBus, node )
      override def toString = "BusNodeSetter.reader(" + controlName + ", " + bus + ", " + node + ")"
   }

   private class AudioMapperImpl( val controlName: String, val bus: RichAudioBus, val node: RichNode )
   extends AbstractAudioReader with AudioMapperLike {
      def newInstance( newBus: RichAudioBus ) = mapper( controlName, newBus, node )
      override def toString = "BusNodeSetter.mapper(" + controlName + ", " + bus + ", " + node + ")"
   }

   private class ControlMapperImpl( val controlName: String, val bus: RichControlBus, val node: RichNode )
   extends AbstractControlReader with ControlMapperLike {
      def newInstance( newBus: RichControlBus ) = mapper( controlName, newBus, node )
      override def toString = "BusNodeSetter.mapper(" + controlName + ", " + bus + ", " + node + ")"
   }

   /*
    *    Careful not use case classes here, as multiple readers / writers for the
    *    same combo might be wanted in a read / write set!
    */
   private class AudioWriterImpl( val controlName: String, val bus: RichAudioBus, val node: RichNode )
   extends AbstractAudioImpl with AudioSetterLike {
      def add( implicit tx: ProcTxn ) {
         val wasAdded = added.swap( true )
         if( wasAdded ) error( "Was already added : " + this )
         bus.addWriter( this )
      }

      def remove( implicit tx: ProcTxn ) {
         val wasAdded = added.swap( false )
         if( wasAdded ) bus.removeWriter( this )
      }

      def newInstance( newBus: RichAudioBus ) = writer( controlName, newBus, node )
      override def toString = "BusNodeSetter.writer(" + controlName + ", " + bus + ", " + node + ")"
   }

   private class ControlWriterImpl( val controlName: String, val bus: RichControlBus, val node: RichNode )
   extends AbstractControlImpl with ControlSetterLike {
      def add( implicit tx: ProcTxn ) {
         val wasAdded = added.swap( true )
         if( wasAdded ) error( "Was already added : " + this )
         bus.addWriter( this )
      }

      def remove( implicit tx: ProcTxn ) {
         val wasAdded = added.swap( false )
         if( wasAdded ) bus.removeWriter( this )
      }

      def newInstance( newBus: RichControlBus ) = writer( controlName, newBus, node )
      override def toString = "BusNodeSetter.writer(" + controlName + ", " + bus + ", " + node + ")"
   }

   /*
    *    Careful not use case classes here, as multiple readers / writers for the
    *    same combo might be wanted in a read / write set!
    */
   private class AudioReaderWriterImpl( val controlName: String, val bus: RichAudioBus, val node: RichNode )
   extends AbstractAudioImpl with AudioSetterLike {
      object dummy extends RichAudioBus.User {
         def busChanged( b: AudioBus )( implicit tx: ProcTxn ) {}
      }

      def add( implicit tx: ProcTxn ) {
         val wasAdded = added.swap( true )
         if( wasAdded ) error( "Was already added : " + this )
         bus.addReader( this )
         bus.addWriter( dummy )
      }

      def remove( implicit tx: ProcTxn ) {
         val wasAdded = added.swap( false )
         if( wasAdded ) {
            bus.removeWriter( dummy )
            bus.removeReader( this )
         }
      }

      def newInstance( newBus: RichAudioBus ) = readerWriter( controlName, newBus, node )
      override def toString = "BusNodeSetter.readerWriter(" + controlName + ", " + bus + ", " + node + ")"
   }

   private class ControlReaderWriterImpl( val controlName: String, val bus: RichControlBus, val node: RichNode )
   extends AbstractControlImpl with ControlSetterLike {
      object dummy extends RichControlBus.User {
         def busChanged( b: ControlBus )( implicit tx: ProcTxn ) {}
      }

      def add( implicit tx: ProcTxn ) {
         val wasAdded = added.swap( true )
         if( wasAdded ) error( "Was already added : " + this )
         bus.addReader( this )
         bus.addWriter( dummy )
      }

      def remove( implicit tx: ProcTxn ) {
         val wasAdded = added.swap( false )
         if( wasAdded ) {
            bus.removeWriter( dummy )
            bus.removeReader( this )
         }
      }

      def newInstance( newBus: RichControlBus ) = readerWriter( controlName, newBus, node )
      override def toString = "BusNodeSetter.readerWriter(" + controlName + ", " + bus + ", " + node + ")"
   }
}