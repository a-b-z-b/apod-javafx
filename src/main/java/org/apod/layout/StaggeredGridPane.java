package org.apod.layout;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.Arrays;

public class StaggeredGridPane extends Pane {
    private final int columnCount;
    private final double gap;

    public StaggeredGridPane(final int columnCount, final double gap) {
        this.columnCount = columnCount;
        this.gap = gap;
    }

    public double getGap() {
        return gap;
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double columnWidth = (width - (columnCount - 1) * gap)/ columnCount;
        double[] columnHeights = new double[columnCount];

        for (Node node: getChildren()) {
            int col = findShortestColumn(columnHeights);
            double x = col * (columnWidth + gap);
            double y = columnHeights[col];

            double prefHeight = node.prefHeight(columnWidth);
            node.resizeRelocate(x, y, columnWidth, prefHeight);

            columnHeights[col] += prefHeight + gap;
        }

        double maxHeight = Arrays.stream(columnHeights).max().orElse(0);
        setMinHeight(maxHeight);
    }

    protected int findShortestColumn(double[] columnHeights) {
        int minIndex = 0;
        for (int i = 1; i < columnHeights.length; i++) {
            if (columnHeights[minIndex] > columnHeights[i] ) {
                minIndex = i;
            }
        }
        return minIndex;
    }
}
