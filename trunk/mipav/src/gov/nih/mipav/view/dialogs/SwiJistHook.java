package gov.nih.mipav.view.dialogs;

import swi.PlugInDialogSWI;
import gov.nih.mipav.model.scripting.parameters.ParameterTable;

/**
 * This class is an example class for tying existing plugins into the JIST interface.  This class can be
 * inserted into any build of MIPAV which has a compatible build of JIST.  JIST will then discover the plugin
 * and execute it appropriately.
 * 
 * @author senseneyj
 *
 */
public class SwiJistHook implements ActionDiscovery {

    private PlugInDialogSWI internalDialog;
    
    public SwiJistHook() {
        this.internalDialog = new PlugInDialogSWI();
    }
    
    public ActionMetadata getActionMetadata() {
        return internalDialog.getActionMetadata();
    }

    @Override
    public ParameterTable createInputParameters() {
        return internalDialog.createInputParameters();
    }

    @Override
    public ParameterTable createOutputParameters() {
        return internalDialog.createOutputParameters();
    }

    @Override
    public void scriptRun(ParameterTable table) {
        internalDialog.scriptRun(table);
    }

    @Override
    public String getOutputImageName(String imageParamName) {
        return internalDialog.getOutputImageName(imageParamName);
    }

    @Override
    public boolean isActionComplete() {
        return internalDialog.isActionComplete();
    }

}
