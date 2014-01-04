package com.mind_era.knime.vega.batch

import org.knime.core.node.NodeDialogPane
import org.knime.core.node.NodeFactory
import org.knime.core.node.NodeView

/**
 * <code>NodeFactory</code> for the "BatchVegaViewer" Node.
 * Converts data to vega (https://github.com/trifacta/vega) images using custom figure descriptor.
 *
 * @author Mind Eratosthenes Kft.
 */
class BatchVegaViewerNodeFactory 
        extends NodeFactory[BatchVegaViewerNodeModel] {

  /**
   * @inheritdoc
   */
  override def createNodeModel = new BatchVegaViewerNodeModel

  /**
   * @inheritdoc
   */
  override def getNrNodeViews = 0

  /**
   * @inheritdoc
   */
  override def createNodeView(viewIndex: Int,
    nodeModel: BatchVegaViewerNodeModel): NodeView[BatchVegaViewerNodeModel] = {
    throw new IndexOutOfBoundsException("No views: " + viewIndex)
    //new BatchVegaViewerNodeView(nodeModel)
  }

  /**
   * @inheritdoc
   */
  override def hasDialog: Boolean = true

  /**
   * @inheritdoc
   */
  override def createNodeDialogPane: NodeDialogPane = new BatchVegaViewerNodeDataAwareDialog
}

