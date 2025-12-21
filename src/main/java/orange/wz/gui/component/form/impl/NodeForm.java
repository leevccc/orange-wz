package orange.wz.gui.component.form.impl;

import orange.wz.gui.component.form.data.NodeFormData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.provider.WzObject;

public class NodeForm extends AbstractValueForm {
    public void setData(String name, String type, WzObject wzObject, EditPane editPane) {
        super.setData(name, type, wzObject, editPane);
    }

    @Override
    public NodeFormData getData() {
        return new NodeFormData(nameInput.getText(), typeInput.getText());
    }
}
