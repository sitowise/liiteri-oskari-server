package fi.nls.oskari.printout.ws.jaxrs.format;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import fi.nls.oskari.printout.imaging.ScaleOps;
import fi.nls.oskari.printout.input.layers.LayerDefinition;
import fi.nls.oskari.printout.input.maplink.MapLink;
import fi.nls.oskari.printout.output.layer.AsyncLayerProcessor;
import fi.nls.oskari.printout.output.map.MapProducer;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.renderer.lite.RendererUtilities;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.filter.request.RequestFilterException;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.TransformException;

import javax.imageio.ImageIO;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import fi.nls.oskari.printout.config.CopyrightTitleProvider;
import fi.nls.oskari.printout.imaging.ScaleOps;
import fi.nls.oskari.printout.input.layers.LayerDefinition;
import fi.nls.oskari.printout.input.maplink.MapLink;
import fi.nls.oskari.printout.output.layer.AsyncLayerProcessor;
import fi.nls.oskari.printout.output.map.MapProducer;
import fi.nls.oskari.printout.printing.PDFProducer.Options;

/**
 *  Outputs and creates a PNG image as a result for JAX-RS map image request.
 */
public class StreamingPNGImpl implements StreamingOutput {
	private static Log log = LogFactory.getLog(StreamingPNGImpl.class);

	MapProducer producer;
	MapLink mapLink;
	Options opts;
	BufferedImage image;
	ScaleOps scaleOps = new ScaleOps();
	private CopyrightTitleProvider _copyrightTitleProvider;

	public StreamingPNGImpl(MapProducer producer, MapLink mapLink, Options opts)
			throws NoSuchAuthorityCodeException, IOException,
			GeoWebCacheException, FactoryException {
		this.mapLink = mapLink;
		this.producer = producer;
		this.opts = opts;
		this.image = null;
		_copyrightTitleProvider = new CopyrightTitleProvider();

	}

	
	public void write(OutputStream outputStream) throws IOException,
			WebApplicationException {

		try {
			log.info("WRITING PNG ======================  ");
			ImageIO.write(image, "png", outputStream);
		} finally {
			image.flush();
		}

	}

	public void underflow() throws IOException, ParseException,
			GeoWebCacheException, XMLStreamException,
			FactoryConfigurationError, RequestFilterException,
			TransformException, URISyntaxException {

		final List<LayerDefinition> selectedLayers = new ArrayList<LayerDefinition>();

		for (LayerDefinition ldef : mapLink.getMapLinkLayers()) {
			LayerDefinition inScale = mapLink
					.selectLayerDefinitionForScale(ldef);
			if (inScale == null) {
				continue;
			}

			selectedLayers.add(inScale);

		}

		Point centre = mapLink.getCentre();
		int zoom = mapLink.getZoom();		
		int footerHeight = 40;		
		int width = mapLink.getWidth();
		int height = mapLink.getHeight();

		Envelope env = producer.getProcessor().getEnvFromPointZoomAndExtent(
				centre, zoom, width, height);

		AsyncLayerProcessor asyncProc = new AsyncLayerProcessor();
		asyncProc.start();
		try {

            BufferedImage producedImage = producer.getMap(asyncProc, env, zoom,
                    width, height, selectedLayers, MapProducer.ImageType.ARGB);

            int x = 0;

            BufferedImage img = new BufferedImage(width, height + footerHeight,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            g2d.drawImage(producedImage, 0, 0, null);
            g2d.setPaint(Color.black);

            if (opts.isPageScale()) {
                // copypaste code from
                // fi.nls.oskari.printout.printing.page.PDFLayeredImagesPage
                final ReferencedEnvelope bounds = new ReferencedEnvelope(
                        env.getMinX(), env.getMaxX(), env.getMinY(),
                        env.getMaxY(), producer.getCrs());

                final Rectangle rect = new Rectangle(0, 0, width, height);

                final AffineTransform transform = RendererUtilities
                        .worldToScreenTransform(bounds, rect, producer.getCrs());

                /* krhm... to be fixed with some algoriddim */
                /* time restricted coding... */
                long widthInMeters = Double.valueOf(env.getWidth()).longValue();
                long scaleLenSelector100m = widthInMeters / 100;
                long scaleLenSelector1Km = widthInMeters / 1000;
                long scaleLenSelector5Km = widthInMeters / 5000;
                long scaleLenSelector10Km = widthInMeters / 10000;
                long scaleLenSelector50Km = widthInMeters / 50000;
                long scaleLenSelector100Km = widthInMeters / 100000;
                long scaleLenSelector500Km = widthInMeters / 500000;
                long scaleLenSelector1000Km = widthInMeters / 1000000;

                String scaleText = "";
                long scaleLength = 0;
                if (scaleLenSelector100m == 0) {
                    /* m */
                    scaleLength = 1;
                    scaleText = "1m";
                } else if (scaleLenSelector1Km == 0) {
                    /* 10m */
                    scaleLength = 10;
                    scaleText = "10m";
                } else if (scaleLenSelector5Km == 0) {
                    /* 10m */
                    scaleLength = 100;
                    scaleText = "100m";
                } else if (scaleLenSelector10Km == 0) {
                    /* 100m */
                    scaleLength = 100;
                    scaleText = "100m";
                } else if (scaleLenSelector50Km == 0) {
                    /* 10km */
                    scaleLength = 1000;
                    scaleText = "1km";
                } else if (scaleLenSelector100Km == 0) {
                    /* 10km */
                    scaleLength = 10000;
                    scaleText = "10km";
                } else if (scaleLenSelector500Km == 0) {
                    /* 100km */
                    scaleLength = 10000;
                    scaleText = "10km";
                } else if (scaleLenSelector1000Km == 0) {
                    /* 100km */
                    scaleLength = 100000;
                    scaleText = "100km";
                } else {
                    /* 1000km */
                    scaleLength = 100000;
                    scaleText = "100km";
                }

                double[] srcPts = new double[] { env.getMinX(), env.getMaxY(),
                        env.getMaxX(), env.getMinY(),
                        env.getMinX() + scaleLength, env.getMinY() };
                double[] dstPts = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
                transform.transform(srcPts, 0, dstPts, 0, 3);

                g2d.setFont(new Font("Serif", Font.PLAIN, 12));

                FontMetrics fm = g2d.getFontMetrics();
                float y = height + footerHeight - 5;
                x += 5;
                g2d.drawString(scaleText, x, y);

                x += fm.stringWidth(scaleText) + 5;

                g2d.draw(new Line2D.Double(x, y, x, y - fm.getHeight()));
                g2d.draw(new Line2D.Double(x, y, x += Double.valueOf(dstPts[4])
                        .floatValue(), y));
                g2d.draw(new Line2D.Double(x, y, x, y - fm.getHeight()));
            }

            FontMetrics fm = g2d.getFontMetrics();
            x += 5;
            float y = height + footerHeight - 5;
            float maxWidth = 0;
            String copyTitle = _copyrightTitleProvider.GetCopyrightTitle();
            float titleWidth = fm.stringWidth(copyTitle);
            g2d.setFont(new Font("Serif", Font.PLAIN, 12));
            if (opts.getCopyrightText() != null) {
                String[] lines = opts.getCopyrightText().split("\\|");
                List<String> escapedLines = new ArrayList<String>();
                for (String line : lines) {
                    String escapedLine = StringEscapeUtils.unescapeHtml4(Jsoup
                            .clean(line, Whitelist.simpleText()));
                    if(escapedLine.length() > 0)
                        escapedLines.add(escapedLine);
                    float lineWidth = fm.stringWidth(escapedLine);
                    if (lineWidth > maxWidth)
                        maxWidth = lineWidth;
                }

                escapedLines.add(copyTitle);
                if (titleWidth > maxWidth)
                    maxWidth = titleWidth;

                for (int i = 0; i < escapedLines.size(); i++) {
                    g2d.drawString(escapedLines.get(i), width - maxWidth - 5, y
                            - i * fm.getHeight());
                }
            } else {
                g2d.drawString(copyTitle, width - titleWidth - 5, y);
            }

            g2d.dispose();

            producedImage = img;
            
			String scaledWidth = mapLink.getValues().get("SCALEDWIDTH");
			String scaledHeight = mapLink.getValues().get("SCALEDHEIGHT");

			if (scaledWidth != null && scaledHeight != null) {
				image = scaleOps.doScaleWithFilters(producedImage,
						Integer.valueOf(scaledWidth, 10),
						Integer.valueOf(scaledHeight, 10));
			} else {
				image = producedImage;
			}
		} finally {
			try {
				asyncProc.shutdown();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				throw new IOException(e);
			}
		}

	}

}
