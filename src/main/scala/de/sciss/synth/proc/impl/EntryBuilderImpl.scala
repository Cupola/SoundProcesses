package de.sciss.synth.proc.impl

import de.sciss.synth.proc.{ProcParam, ProcEntryBuilder}

trait EntryBuilderImpl extends ProcEntryBuilder {
   var usedParams = Set.empty[ ProcParam ]

   // XXX we might not even need this, at least for graphs
   // as the included parameters are directly accessible
   // from the SynthGraph !
   def includeParam( param: ProcParam ) {
      usedParams += param
   }
}