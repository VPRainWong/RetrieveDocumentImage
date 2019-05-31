package com.vp.plugin.sample.retrievedocumentimage;

import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import com.vp.plugin.ApplicationManager;
import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPActionController;
import com.vp.plugin.diagram.IDiagramElement;
public class RetrieveDocumentImageActionControl implements VPActionController {

	@Override
	public void performAction(VPAction arg0) {
		// Create a collection to store the images
		Collection<Image> lImages = new ArrayList<Image>();

		// Retrieve the selected element in diagram
		IDiagramElement[] selectedElements = ApplicationManager.instance().getDiagramManager().getActiveDiagram().getSelectedDiagramElement();
		
		if (selectedElements.length > 0) {
			// Process the selected diagram one by one to retrieve its HTML description
			// After that pass to retrieveImageFromElement method for processing,
			// and append the images to collection 
			for (IDiagramElement element : selectedElements) {
				String lDescription = element.getModelElement().getHTMLDescription();
				Collection<Image> descImage = retrieveImageFromElement(lDescription);
				if (descImage != null) {
					lImages.addAll(descImage);
				}
			}
			
			// Create a panel for display the images as ImageIcon
			// and show it with a Message Dialog
			JPanel lPanel = new JPanel(new FlowLayout());
			for (Image lImage : lImages) {
				JLabel lLabel = new JLabel(new ImageIcon(lImage));
				lPanel.add(lLabel);
			}
			ApplicationManager.instance().getViewManager().showMessageDialog(null, lPanel);
		}
	}
	
	// Retrieve the HTML node form description text
	private Collection<Image> retrieveImageFromElement(String lDescription) {
		Collection<Image> lImages = new ArrayList<Image>();
		// Create JEditorPane to host the HTML description
		JEditorPane p = new JEditorPane();
		p.setContentType("text/html");
		p.setText(lDescription);
		
		// Walk through the description and pass the HTML node 
		// to recursive function parseImage to find out the image element
		HTMLDocument d = (HTMLDocument) p.getDocument();
		Element lRoot = d.getRootElements()[0];
		int lCount = lRoot.getElementCount();
		for (int i = 0; i < lCount; i++) {
			Element lElement = lRoot.getElement(i);				
			Object lElementNameAttribute = lElement.getAttributes().getAttribute(StyleConstants.NameAttribute);
			if (HTML.Tag.BODY == lElementNameAttribute) {
				parseImage(lElement, lImages);
			}
		}
		return lImages;
	}
	
	// Retrieve image elements from HTML node
	private void parseImage(Element aParentElement, Collection<Image> aImages) {
		int lElementCount = aParentElement.getElementCount();
		for (int i = 0; i < lElementCount; i++) {
			Element lElement = aParentElement.getElement(i);
			Object lElementNameAttribute = lElement.getAttributes().getAttribute(StyleConstants.NameAttribute);
			
			// Local the image element
			if (HTML.Tag.IMG == lElementNameAttribute) {
				// Retrieve the source of the image
				Object lValue = lElement.getAttributes().getAttribute(HTML.Attribute.SRC);
				if (lValue instanceof String) {
					String lSrc = (String) lValue;
					if (lSrc.startsWith("Documentation/Clipboard/Images/")) {
						lSrc = lSrc.substring("Documentation/Clipboard/Images/".length());
					}
					
					// Read the image from file system and store it into Image object
					try {
						InputStream lIs = ApplicationManager.instance().getViewManager().getDocumentationImageInputStream(lSrc);
						if (lIs != null) {
							try {
								Image lImage = ImageIO.read(lIs);
								
								int lImageWidth = lImage.getWidth(null);
								int lImageHeight = lImage.getHeight(null);
								int lAvailableWidth = 100;
								int lAvailableHeight = 100;
								
								float lScale;
								if (lImageWidth > lAvailableWidth || lImageHeight > lAvailableHeight) {
									lScale = Math.min((lAvailableWidth/(float)lImageWidth), (lAvailableHeight/(float)lImageHeight));
									
									lImageWidth = (int) (lImageWidth*lScale);
									lImageHeight = (int) (lImageHeight*lScale);
																	
								} else {
									lScale = 1f;
								}
								
								if (lScale != 1f) {
									BufferedImage lImg = new BufferedImage(lImageWidth, lImageHeight, BufferedImage.TYPE_4BYTE_ABGR);
									Graphics2D lG = lImg.createGraphics();
									lG.drawImage(lImage, 0, 0, lImageWidth, lImageHeight, null);
									lG.dispose();
									lImage = lImg;
								}				
								// Add the image into collection
								aImages.add(lImage);
							} finally {
								lIs.close();
							}
						}						
					} catch (Exception lE) {
						lE.printStackTrace();
					}
				}
			}
			
			// children
			parseImage(lElement, aImages);
		}
	}
					
	@Override
	public void update(VPAction arg0) {
		// TODO Auto-generated method stub
		
	}

}
