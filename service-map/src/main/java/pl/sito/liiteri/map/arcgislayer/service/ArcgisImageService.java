package pl.sito.liiteri.map.arcgislayer.service;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.imageio.ImageIO;

import com.esri.core.geometry.Envelope;

import pl.sito.liiteri.arcgis.domain.ArcgisMapLayerLegendConfiguration;
import pl.sito.liiteri.arcgis.domain.ArcgisMapLayerLegendItemConfiguration;
import pl.sito.liiteri.stats.domain.GridStatsResultItem;
import pl.sito.liiteri.stats.domain.GridStatsVisualization;
import pl.sito.liiteri.stats.domain.GridStatsVisualizationParams;
import pl.sito.liiteri.stats.domain.GridStatsVisualizationParams.GridStatsVisualizationType;
import fi.nls.oskari.domain.map.stats.StatsVisualization;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;

public class ArcgisImageService
{
	private static class ArcgisImageServiceHolder
	{
		static final ArcgisImageService INSTANCE = new ArcgisImageService();
	}

	public static ArcgisImageService getInstance()
	{
		return ArcgisImageServiceHolder.INSTANCE;
	}
	
	private static final Logger log = LogFactory.getLogger(ArcgisImageService.class);

	private final int LEGEND_DEFAULT_WIDTH = 350;
	private final int LEGEND_TEXT_MARGIN = 5;
	private final int LEGEND_VERTICAL_MARGIN = 5;
	private final int LEGEND_HORIZONTAL_MARGIN = 5;
	
	public byte[] createLegendImage(List<ArcgisMapLayerLegendConfiguration> configs) 
	{
		byte[] result = new byte[0];
		
		try {
			int height = getImageHeight(configs);
			BufferedImage image = new BufferedImage(LEGEND_DEFAULT_WIDTH, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = (Graphics2D) image.getGraphics();
			g.setBackground(Color.WHITE);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);		
			g.setColor(Color.WHITE);
			g.fillRect(0,  0, LEGEND_DEFAULT_WIDTH, height);
			
			int verticalOffset = 0;
			for (ArcgisMapLayerLegendConfiguration config : configs)
			{
				verticalOffset = loadConfigToImage(g, config, verticalOffset);
			}
			
			g.dispose();
			
			result = imageToBytes(image);
		}
		catch (Exception e) {
			log.error(e, "Error creating legend image");
		}
		
		return result;
	}
	
	public byte[] renderTile(GridStatsVisualizationParams params, GridStatsVisualization visualization) 
	{
		byte[] result = new byte[0];
		Envelope bbox = params.getBbox();
		int width = params.getWidth();
		int height = params.getHeight();
		int gridSize = params.getGridSize();
		
		double minX = bbox.getLowerLeft().getX();			
		double minY = bbox.getLowerLeft().getY();
		double maxX = bbox.getUpperRight().getX();
		double maxY = bbox.getUpperRight().getY();		
		
		double stepX = (maxX - minX) / (1.0 *width);
		double stepY = (maxY - minY) / (1.0 *height);		
		
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setBackground(Color.WHITE);
		
		if (visualization != null) {
			if (params.getVisualizationType() == GridStatsVisualizationType.Circle)
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			AffineTransform originalTransform = g.getTransform();
			
			double scaleX = (1.0 * width) / (maxX - minX);
			double scaleY = (1.0 * height) / (maxY - minY);
			
			AffineTransform tx2 = AffineTransform.getScaleInstance(scaleX, -scaleY);
			tx2.translate(-minX, -(maxY - minY) - minY);
					
//			Point2D.Double zero = new Point2D.Double(minX, minY);
//			Point2D.Double maxV = new Point2D.Double(maxX, maxY);
//			Point2D.Double firstY = new Point2D.Double(minX + (maxX-minX)/2, minY + (maxY-minY)/4);
//			Point2D.Double middle = new Point2D.Double(minX + (maxX-minX)/2, minY + (maxY-minY)/2);
//			Point2D.Double thirdY = new Point2D.Double(minX + (maxX-minX)/2, minY + 3*(maxY-minY)/4);
//			
//			Point2D transformedZero = tx2.transform(zero, null);
//			Point2D transformedMax = tx2.transform(maxV, null);
//			Point2D transformedfirstY = tx2.transform(firstY, null);
//			Point2D transformedMiddle = tx2.transform(middle, null);
//			Point2D transformedthirdY = tx2.transform(thirdY, null);			
			
			g.setTransform(tx2);				
			
			String[] groupDescriptions = visualization.getDescriptions();
			
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
			if (params.getVisualizationType() == GridStatsVisualizationType.Circle) {
				for (int i = 0; i < groupDescriptions.length; i++)
				{
					for (GridStatsResultItem item : visualization.getItems(groupDescriptions[i]))
					{
						double value = item.getValue();
						if (value < 0)
							value = -value;
						
						if (value > max)
							max = value;
						if (value < min)
							min = value;
					}
				}
			}
			
			for (int i = 0; i < groupDescriptions.length; i++)
			{
				String descriptionStr = groupDescriptions[i];			
				
				Color color = null;
				if (params.getVisualizationType() == GridStatsVisualizationType.Square) {
					color = stringToColor(descriptionStr);
				}						
				
				for (GridStatsResultItem item : visualization.getItems(descriptionStr))
				{
					if (params.getVisualizationType() == GridStatsVisualizationType.Circle) {
						if (item.getValue() >= 0)
							color = stringToColor(params.getColors()[0]);
						else
							color = stringToColor(params.getColors()[1]);
					}

					renderGridStatsResultItem(g, item, color, Color.black, params.getVisualizationType(), 
							gridSize, bbox, min, max);
				}
			}						
			
			g.setTransform(originalTransform);	
		}
		
		g.dispose();
		result = imageToBytes(image);
		
		return result;
	}
	
	private void renderGridStatsResultItem(Graphics g, GridStatsResultItem item, Color color, Color borderColor, 
			GridStatsVisualizationType visualizationType, 
			int gridSize, Envelope bbox, double min, double max) {
		
		double minX = bbox.getLowerLeft().getX();			
		double minY = bbox.getLowerLeft().getY();
		double maxX = bbox.getUpperRight().getX();
		double maxY = bbox.getUpperRight().getY();		
		
    	int northCenter = item.getNorthing();
    	int eastingCenter = item.getEasting();
    	int northMin = northCenter - gridSize/2;
    	int northMax = northCenter + gridSize/2;
    	int eastingMin = eastingCenter - gridSize/2;
    	int eastingMax = eastingCenter + gridSize/2;
    	
    	if (northMax > minY && northMin < maxY && eastingMax > minX && eastingMin < minY) {	        		    		    		
    		    		
    		g.setColor(color);
    		
    		switch (visualizationType)
    		{
    		case Square:
    			g.fillRect(eastingMin, northMin, gridSize, gridSize);  
//    			g.setColor(borderColor);
//    			g.drawRect(eastingMin, northMin, gridSize, gridSize);    			
    			break;
    		case Circle:    			
    			int gridBox = (int) Math.round(Math.sqrt((Math.abs(item.getValue()) - min) / (max - min)) * gridSize);
    			int ovalX = eastingMin + (gridSize - gridBox) /2;
        		int ovalY = northMin + (gridSize - gridBox) /2;
        		g.fillOval(ovalX, ovalY, gridBox, gridBox);
        		if (item.getValue() != 0) {
    				g.setColor(borderColor);
    				g.drawRect(eastingMin, northMin, gridSize, gridSize);	
    			}
    			break;
    		default:
    			break;
    		}	        			        			        			        		
    	}
	}	
	
	private int getImageHeight(List<ArcgisMapLayerLegendConfiguration> configs) {
		int height = LEGEND_VERTICAL_MARGIN;
		for (ArcgisMapLayerLegendConfiguration config : configs)
		{
			for (ArcgisMapLayerLegendItemConfiguration item : config.getItems())
				height += item.getHeight() + LEGEND_VERTICAL_MARGIN;
		}
		return height;
	}

	private int loadConfigToImage(Graphics g, ArcgisMapLayerLegendConfiguration conf, int verticalOffset)
	{
		int currentY = verticalOffset;
		for (ArcgisMapLayerLegendItemConfiguration item : conf.getItems())
		{
			currentY += LEGEND_VERTICAL_MARGIN;
			int currentX = LEGEND_HORIZONTAL_MARGIN;

			BufferedImage subImage = decodeImage(item.getImageData());

			if (subImage == null)
			{
				g.setColor(Color.BLACK);
				g.fillRect(currentX, currentY, item.getWidth(),
						item.getHeight());
			} else
			{
				g.drawImage(subImage, currentX, currentY,
						item.getWidth(), item.getHeight(), null);
			}

			currentX += item.getWidth() + LEGEND_TEXT_MARGIN;
			g.setColor(Color.BLACK);
			String text = item.getLabel();
			if (text == null || text.isEmpty())
				text = "";

			int lineHeight = g.getFontMetrics().getAscent();
			int bottomLineY = currentY + item.getHeight() / 2 + lineHeight / 2;

			g.drawString(text, currentX, bottomLineY);

			// g.setColor(Color.BLACK);
			// g.drawLine(0, bottomLineY, DEFAULT_WIDTH, bottomLineY);

			currentY += item.getHeight();
		}		

		return currentY;
	}
	
	private Color stringToColor(String descriptionStr) {
		return new Color(
	            Integer.valueOf(descriptionStr.substring( 0, 2 ), 16 ),
	            Integer.valueOf(descriptionStr.substring( 2, 4 ), 16 ),
	            Integer.valueOf(descriptionStr.substring( 4, 6 ), 16 ),
	            194);
	}

	private static BufferedImage decodeImage(String base64string)
	{				
		BufferedImage image = null;
		byte[] imageByte;
		try
		{
			imageByte = org.apache.commons.codec.binary.Base64.decodeBase64(base64string);
			ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
			image = ImageIO.read(bis);
			bis.close();
		} catch (Exception e)
		{
			log.error(e, "Error during encoding image");
		}
		return image;
	}

	private static byte[] imageToBytes(BufferedImage bufferedImage)
	{
		if (bufferedImage == null)
		{
			log.error("No image given");
			return null;
		}

		ByteArrayOutputStream byteaOutput = new ByteArrayOutputStream();
		try
		{
			ImageIO.write(bufferedImage, "png", byteaOutput);
			byteaOutput.flush();
			byteaOutput.close();
		} catch (Exception e)
		{
			log.error(e, "Image could not be written into stream");
		}
		return byteaOutput.toByteArray();
	}
	
	private static BufferedImage bytesToImage(byte[] bytes)
	{
		if (bytes == null)
		{
			log.error("No bytes given");
			return null;
		}

		ByteArrayInputStream byteInput = new ByteArrayInputStream(bytes);
		try
		{
			return ImageIO.read(byteInput);			
		} catch (Exception e)
		{
			log.error(e, "Image could not be written into stream");
			return null;
		}
	}
}
