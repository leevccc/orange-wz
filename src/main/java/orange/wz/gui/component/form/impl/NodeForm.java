package orange.wz.gui.component.form.impl;

import orange.wz.gui.component.form.data.NodeFormData;

public class NodeForm extends AbstractValueForm {
    @Override
    public void setData(String name, String type) {
        super.setData(name, type);
    }

    @Override
    public NodeFormData getData() {
        return new NodeFormData(nameInput.getText(), typeInput.getText());
    }
}
