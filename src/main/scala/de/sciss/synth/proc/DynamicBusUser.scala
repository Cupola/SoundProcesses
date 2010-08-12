package de.sciss.synth.proc

import de.sciss.synth.{ControlBus, AudioBus}

trait DynamicBusUser {
   def add( implicit tx: ProcTxn ) : Unit
   def remove( implicit tx: ProcTxn ) : Unit
   def bus: RichBus
}

trait DynamicAudioBusUser extends DynamicBusUser {
   def bus: RichAudioBus
   def migrateTo( newBus: RichAudioBus )( implicit tx: ProcTxn ) : DynamicAudioBusUser
}

trait DynamicControlBusUser extends DynamicBusUser {
   def bus: RichControlBus
   def migrateTo( newBus: RichControlBus )( implicit tx: ProcTxn ) : DynamicControlBusUser
}

object DynamicBusUser {
   def reader( bus: RichAudioBus ) : DynamicAudioBusUser =
      new AudioReaderImpl( bus )

   def reader( bus: RichControlBus ) : DynamicControlBusUser =
      new ControlReaderImpl( bus )

   def writer( bus: RichAudioBus ) : DynamicAudioBusUser =
      new AudioWriterImpl( bus )

   def writer( bus: RichControlBus ) : DynamicControlBusUser =
      new ControlWriterImpl( bus )

//   def readerWriter( bus: RichAudioBus ) : DynamicAudioBusUser =
//      new AudioReaderWriterImpl( bus )

//   def readerWriter( bus: RichControlBus ) : DynamicControlBusUser =
//      new ControlReaderWriterImpl( bus )

   private abstract class AbstractAudioImpl extends DynamicAudioBusUser with RichAudioBus.User {
      val added = Ref( false )
      def busChanged( bus: AudioBus )( implicit tx: ProcTxn ) {}
      def migrateTo( newBus: RichAudioBus )( implicit tx: ProcTxn ) : DynamicAudioBusUser = {
         require( newBus.numChannels == bus.numChannels )
         val wasAdded = added()
         if( wasAdded ) remove
         val res = newInstance( newBus )
         if( wasAdded ) res.add
         res
      }
      def newInstance( newBus: RichAudioBus ) : DynamicAudioBusUser
   }

   private class AudioReaderImpl( val bus: RichAudioBus ) extends AbstractAudioImpl {
      def add( implicit tx: ProcTxn ) { bus.addReader( this )}
      def remove( implicit tx: ProcTxn ) { bus.removeReader( this )}
      def newInstance( newBus: RichAudioBus ) : DynamicAudioBusUser = reader( newBus )
   }

   private class AudioWriterImpl( val bus: RichAudioBus ) extends AbstractAudioImpl {
      def add( implicit tx: ProcTxn ) { bus.addWriter( this )}
      def remove( implicit tx: ProcTxn ) { bus.removeWriter( this )}
      def newInstance( newBus: RichAudioBus ) : DynamicAudioBusUser = writer( newBus )
   }

   private abstract class AbstractControlImpl extends DynamicControlBusUser with RichControlBus.User {
      val added = Ref( false )
      def busChanged( bus: ControlBus )( implicit tx: ProcTxn ) {}
      def migrateTo( newBus: RichControlBus )( implicit tx: ProcTxn ) : DynamicControlBusUser = {
         require( newBus.numChannels == bus.numChannels )
         val wasAdded = added()
         if( wasAdded ) remove
         val res = newInstance( newBus )
         if( wasAdded ) res.add
         res
      }
      def newInstance( newBus: RichControlBus ) : DynamicControlBusUser
   }

   private class ControlReaderImpl( val bus: RichControlBus ) extends AbstractControlImpl {
      def add( implicit tx: ProcTxn ) { bus.addReader( this )}
      def remove( implicit tx: ProcTxn ) { bus.removeReader( this )}
      def newInstance( newBus: RichControlBus ) : DynamicControlBusUser = reader( newBus )
   }

   private class ControlWriterImpl( val bus: RichControlBus ) extends AbstractControlImpl {
      def add( implicit tx: ProcTxn ) { bus.addWriter( this )}
      def remove( implicit tx: ProcTxn ) { bus.removeWriter( this )}
      def newInstance( newBus: RichControlBus ) : DynamicControlBusUser = writer( newBus )
   }
}