package fi.nls.oskari.printout.printing.page;

import fi.nls.oskari.printout.printing.PDFProducer.Options;
import fi.nls.oskari.printout.printing.PDFProducer.Page;
import fi.nls.oskari.printout.printing.PDFProducer.PageCounter;
import fi.nls.oskari.printout.printing.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentProperties;
import org.apache.pdfbox.pdmodel.markedcontent.PDPropertyList;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

//import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
/*import org.apache.pdfbox.pdmodel.font.PDType1Font;*/

/**
 * this class adds map legend page. W-i-P as some map legend images span
 * multiple pages in length.
 * 
 */
public class PDFContentPage extends PDFAbstractPage implements PDFPage {

	public PDFContentPage(Page page, Options opts, PDFont font) {
		super(page, opts, font);

	}

	public void createPages(PDDocument targetDoc, PageCounter pageCounter)
			throws IOException, TransformException {
		PDDocumentCatalog catalog = targetDoc.getDocumentCatalog();

		PDPage targetPage = page.createNewPage(targetDoc,
				opts.getPageTemplate() != null, pageCounter);

		PDResources resources = targetPage.findResources();
		if (resources == null) {
			resources = new PDResources();
			targetPage.setResources(resources);
		}

		PDOptionalContentProperties ocprops = catalog.getOCProperties();

		if (ocprops == null) {
			ocprops = new PDOptionalContentProperties();
			catalog.setOCProperties(ocprops);
		}

		PDPropertyList props = new PDPropertyList();
		resources.setProperties(props);

		PDPageContentStream contentStream = page.createContentStreamTo(
				targetDoc, targetPage, opts.getPageTemplate() != null);

		createContentOverlay(targetDoc, contentStream, ocprops, props,
				opts.getContent(), pageCounter);

		contentStream.close();

	}

}
