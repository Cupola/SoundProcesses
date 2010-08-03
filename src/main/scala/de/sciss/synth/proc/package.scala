package de.sciss.synth

import proc.{ProcEdge, Proc, Topology}

package object proc {
   type ProcTopology = Topology[ Proc, ProcEdge ]
//   type AudioBusNodeSetter = BusNodeSetter[ AudioBus ]
//   type ControlBusNodeSetter = BusNodeSetter[ ControlBus ]
}