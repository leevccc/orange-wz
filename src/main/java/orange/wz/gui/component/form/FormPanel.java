package orange.wz.gui.component.form;

import orange.wz.gui.component.form.data.NodeFormData;

import javax.swing.*;

public interface FormPanel {
    JPanel getPanel();

    NodeFormData getData();
}

