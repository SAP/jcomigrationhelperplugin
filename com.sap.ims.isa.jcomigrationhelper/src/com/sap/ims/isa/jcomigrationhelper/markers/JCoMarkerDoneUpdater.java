package com.sap.ims.isa.jcomigrationhelper.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.IMarkerUpdater;

public class JCoMarkerDoneUpdater implements IMarkerUpdater {

    public JCoMarkerDoneUpdater() {}

    @Override
    public String[] getAttribute() {
        return null;
    }

    @Override
    public String getMarkerType() {
        return "com.sap.ims.isa.jcomigrationhelper.marker.done"; //$NON-NLS-1$
    }

    @Override
    public boolean updateMarker(IMarker marker, IDocument document, Position position) {
        return JCoMarkerFactory.updateMarker(marker, document, position);
    }

}
