package de.sciss.synth.proc

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


   private class AudioReaderImpl( val bus: RichAudioBus ) extends DynamicAudioBusUser with RichAudioBus.User {

   }

   private class AudioWriterImpl( val bus: RichAudioBus ) extends DynamicAudioBusUser with RichAudioBus.User {

   }

   private class ControlReaderImpl( val bus: RichControlBus ) extends DynamicControlBusUser with RichControlBus.User {

   }

   private class ControlWriterImpl( val bus: RichControlBus ) extends DynamicControlBusUser with RichControlBus.User {

   }
}