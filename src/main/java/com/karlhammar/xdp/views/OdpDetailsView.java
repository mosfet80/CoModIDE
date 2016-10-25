package com.karlhammar.xdp.views;

import java.awt.BorderLayout;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdpDetailsView extends AbstractOWLViewComponent {

	private static final long serialVersionUID = 6258186472581035105L;
	private static final Logger log = LoggerFactory.getLogger(OdpDetailsView.class);

    private Metrics metricsComponent;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());
        metricsComponent = new Metrics(getOWLModelManager());
        add(metricsComponent, BorderLayout.CENTER);
        log.info("ODP Details View initialized");
    }

	@Override
	protected void disposeOWLView() {
		metricsComponent.dispose();
	}
	
	public void setText(String inputText) {
		metricsComponent.setValue(inputText);
	}
}