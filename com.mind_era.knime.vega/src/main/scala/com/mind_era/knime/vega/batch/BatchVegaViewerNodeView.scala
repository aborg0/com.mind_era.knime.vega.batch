package com.mind_era.knime.vega.batch;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "BatchVegaViewer" Node.
 * Converts data to vega (https://github.com/trifacta/vega) images using custom figure descriptor.
 *
 * @constructor Creates a new view.
 * 
 * @param nodeModel The model (class: [[BatchVegaViewerNodeModel]])
 *
 * @author Mind Eratosthenes Kft.
 */
class BatchVegaViewerNodeView(nodeModel: BatchVegaViewerNodeModel) extends NodeView[BatchVegaViewerNodeModel](nodeModel) {
  // TODO instantiate the components of the view here.

  /**
   * @inheritdoc
   */
  protected override def modelChanged {

    // TODO retrieve the new model from your nodemodel and 
    // update the view.
    val nodeModel = getNodeModel()
    assert(nodeModel != null)

    // be aware of a possibly not executed nodeModel! The data you retrieve
    // from your nodemodel could be null, empty, or invalid in any kind.

  }

  /**
   * @inheritdoc
   */
  protected override def onClose {

    // TODO things to do when closing the view
  }

  /**
   * @inheritdoc
   */
  protected override def onOpen {

    // TODO things to do when opening the view
  }
}

