package de.sciss.synth.proc.impl

import de.sciss.synth.proc.{TxnPlayer, AudioBusNodeSetter}

case class AudioBusPlayerImpl( player: TxnPlayer, setter: AudioBusNodeSetter )