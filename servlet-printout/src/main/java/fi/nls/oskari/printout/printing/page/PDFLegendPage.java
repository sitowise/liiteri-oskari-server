package fi.nls.oskari.printout.printing.page;

import fi.nls.oskari.printout.printing.PDFProducer.Options;
import fi.nls.oskari.printout.printing.PDFProducer.Page;
import fi.nls.oskari.printout.printing.PDFProducer.PageCounter;
import fi.nls.oskari.printout.printing.PDPageContentStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentGroup;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentProperties;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDPixelMap;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.pdfbox.pdmodel.markedcontent.PDPropertyList;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

//import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
/*import org.apache.pdfbox.pdmodel.font.PDType1Font;*/

/**
 * this class adds map legend page. W-i-P as some map legend images span
 * multiple pages in length.
 * 
 */
public class PDFLegendPage extends PDFAbstractPage implements PDFPage {

    private static Log log = LogFactory.getLog(PDFLegendPage.class);
	public final float PT_TO_PX_FACTOR = 72f/2.54f; 
	
	class LegendImage {
		PDXObjectImage ximage;
		int w;
		int h;
	}
	
	private List<URL> imageUrls;
	private Map<String, String> xClientInfo;

	public PDFLegendPage(Page page, Options opts, PDFont font, List<URL> imageUrls, Map<String, String> xClientInfo) {
		super(page, opts, font);
		this.imageUrls = imageUrls;
		this.xClientInfo = xClientInfo;
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

		Vector<LegendImage> legendImages = new Vector<LegendImage>();		
		createLegendImages(targetDoc, legendImages);
		
		createMapLayersOverlay(targetDoc, targetPage, contentStream, ocprops,
				props, legendImages);
		
		createTextLayerOverlay(targetDoc, contentStream, ocprops, props);				
		
		contentStream.close();

	}
	
	protected void createLegendImages(PDDocument targetDoc, List<LegendImage> legendImages)
			throws IOException {

		List<BufferedImage> images = new Vector<BufferedImage>();
		
		for (URL url : this.imageUrls)
		{
			final HttpURLConnection con = (HttpURLConnection) new URL(url.toString()).openConnection();
			try {
				con.setUseCaches(false);
				
				if (this.xClientInfo != null) {
					for (Map.Entry<String,String> entry : this.xClientInfo.entrySet()) {
						con.setRequestProperty(entry.getKey(), entry.getValue());
					}
				}
				
				InputStream response = con.getInputStream();
				
				BufferedImage image = ImageIO.read(response);
				if(image != null) {
					images.add(image);
				} else {
					log.warn("No legend graphic found " + url);
				}
			} catch (Exception e) {
				log.warn("No legend graphic found " + url);
			} finally {
				con.disconnect();
			}
		}				
		
		legendImages.addAll(fitToPageHeight(targetDoc, images));
	}
	
	protected void createMapLayersOverlay(PDDocument targetDoc,
			PDPage targetPage, PDPageContentStream contentStream,
			PDOptionalContentProperties ocprops, PDPropertyList props,
			Vector<LegendImage> legendImages) throws IOException {

		float f[] = { 1.0f, 1.5f };

		if (opts.getPageMapRect() != null) {
			f[0] = opts.getPageMapRect()[0];
			f[1] = opts.getPageMapRect()[1];
		}
//		int width = page.getMapWidthTargetInPoints(opts);
//		int height = page.getMapHeightTargetInPoints(opts);

		page.getTransform().transform(f, 0, f, 0, 1);
		
		PDOptionalContentGroup layerGroup = new PDOptionalContentGroup("legend");
		ocprops.addGroup(layerGroup);				

		COSName mc0 = COSName.getPDFName("MClegend");
		props.putMapping(mc0, layerGroup);
		/* PDFont font = PDType1Font.HELVETICA_BOLD; */
		contentStream.beginMarkedContentSequence(COSName.OC, mc0);

		for (LegendImage legendImage : legendImages) {
			
			PDXObjectImage ximage = legendImage.ximage;
			contentStream.drawXObject(ximage, f[0], f[1], legendImage.w, legendImage.h);			
			f[1] += legendImage.h;
		}
		
		contentStream.endMarkedContentSequence();
	}


	private List<LegendImage> fitToPageHeight(PDDocument targetDoc, List<BufferedImage> images) throws IOException
	{
		Vector<LegendImage> legendImages = new Vector<LegendImage>();
		float pageHeight = page.getHeight() * PT_TO_PX_FACTOR;
		float totalLegendHeight = 0;
		for (BufferedImage image : images) {
			totalLegendHeight += image.getHeight();
		}	
		
		for (BufferedImage image : images) {
			LegendImage legendImage = new LegendImage();
			int width = image.getWidth();
	    	int height = image.getHeight();
			
			if (totalLegendHeight > pageHeight) {
				float factor = pageHeight / totalLegendHeight;
				int newWidth = (int) (width * factor);
				int newHeight = (int) (height * factor);
				BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, image.getType());
				Graphics2D g = resizedImage.createGraphics();
				g.drawImage(image, 0, 0, newWidth, newHeight, null);				
				g.setComposite(AlphaComposite.Src);
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
				g.dispose();
				
				legendImage.ximage = new PDPixelMap(targetDoc, resizedImage);
	
				legendImage.w = newWidth;
		    	legendImage.h = newHeight;
			} else {
				legendImage.ximage = new PDPixelMap(targetDoc, image);
				legendImage.w = width;
		    	legendImage.h = height;
			}
			legendImages.add(legendImage);
		}																      	    	
    			
		return legendImages;
	}

	void createTextLayerOverlay(PDDocument targetDoc,
			PDPageContentStream contentStream,
			PDOptionalContentProperties ocprops, PDPropertyList props)
			throws IOException, TransformException {

		float logoWidth = 24;
		float logoHeight = 24;

		PDXObjectImage xlogo = null;

		if (opts.isPageLogo()) {
			/* MUST create before optiona content group is created */
			/*
			 * - this is a googled fix to not being able to show images in
			 * overlays
			 */
			InputStream inp = getClass().getResourceAsStream("logo.png");
			try {
				BufferedImage imageBuf = ImageIO.read(inp);
				xlogo = new PDPixelMap(targetDoc, imageBuf);
			} finally {
				inp.close();
			}
		}	

		/* BEGIN overlay content */

		/* title */

		if (opts.getPageTitle() != null) {
			String pageTitle = StringEscapeUtils.unescapeHtml4(Jsoup.clean(
					opts.getPageTitle(), Whitelist.simpleText()));

			createTextAt(contentStream, pageTitle, 9.0f, page.getHeight() - 1f,
					opts.getFontSize(), 0, 0, 0);

		}

		/* pvm */
		if (opts.isPageDate()) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Locale l = new Locale("fi");
			Date dte = Calendar.getInstance(l).getTime();

			String dateStr = sdf.format(dte);

			createTextAt(contentStream, dateStr, page.getWidth() - 4f,
					page.getHeight() - 1f, opts.getFontSize(), 0, 0, 0);

		}

		/* logo */
		if (opts.isPageLogo()) {
			contentStream.setNonStrokingColor(255, 255, 255);
			contentStream.setStrokingColor(255, 255, 255);

			contentStream.drawXObject(xlogo, 1.0f / 2.54f * 72f, 16, logoWidth,
					logoHeight);

		}

		/* END overlay content */

		contentStream.endMarkedContentSequence();

		contentStream.close();

	}

}
