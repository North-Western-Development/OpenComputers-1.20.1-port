package li.cil.oc.common.tileentity.traits

import li.cil.oc.api

trait ImmibisMicroblock extends BlockEntity {
  val ImmibisMicroblocks_TransformableBlockEntityMarker = null

  def ImmibisMicroblocks_isSideOpen(side: Int) = true

  def ImmibisMicroblocks_onMicroblocksChanged() {
    api.Network.joinOrCreateNetwork(this)
  }
}
