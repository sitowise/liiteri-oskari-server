package fi.nls.oskari.domain.map;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.PropertyUtil;
import org.json.JSONObject;

import java.util.*;

public class OskariLayer extends JSONLocalizedNameAndTitle implements Comparable<OskariLayer> {

    private static Logger log = LogFactory.getLogger(OskariLayer.class);

    private static final String TYPE_COLLECTION = "collection";
    public static final String TYPE_WMS = "wmslayer";
    public static final String TYPE_WFS = "wfslayer";
    public static final String TYPE_WMTS = "wmtslayer";
    public static final String TYPE_STATS = "statslayer";
    public static final String TYPE_ANALYSIS = "analysislayer";
    public static final String TYPE_USERLAYER = "userlayer";
    public static final String TYPE_ARCGISLAYER = "arcgislayer";

    private int id = -1;
    private int parentId = -1;
    private String externalId;
	private String type;

    private boolean isBaseMap = false;
    private int groupId;

    private String name;
    private String url;

    // simplied url is just for caching so we don't need to create it but once
    private final static String secureBaseUrl = PropertyUtil.get("maplayer.wmsurl.secure");
    private String simplifiedUrl;

    // defaults
	private Integer opacity = 100;
    private String style;
	private Double minScale = -1d;
	private Double maxScale = -1d;

    private String legendImage;
    private String metadataId;

    private String tileMatrixSetId;
    private String tileMatrixSetData;

    private JSONObject params = new JSONObject();
    private JSONObject options = new JSONObject();

    private boolean realtime = false;
    private int refreshRate;

    private String gfiXslt;
    private String gfiType;
    private String gfiContent;
    private String geometry;

    private Date created = null;
    private Date updated = null;

    private Set<InspireTheme> inspireThemes = new HashSet<InspireTheme>();
    private Set<LayerGroup> groups = new HashSet<LayerGroup>();
    private List<OskariLayer> sublayers = new ArrayList<OskariLayer>();
    
    private String downloadServiceUrl;
    private String copyrightInfo;


    public boolean isCollection() {
        return TYPE_COLLECTION.equals(type);
    }

    // we only link one theme at the moment so get the first one
	public InspireTheme getInspireTheme() {
        if(inspireThemes == null || inspireThemes.isEmpty()) {
            return null;
        }
        if(inspireThemes.size() > 1) {
            // TODO: remove this when we support more than one theme
            log.warn("More than one inspire theme, this shouldn't happen!! layerId:", getId(), "- Themes:" , inspireThemes);
        }
		return inspireThemes.iterator().next();
	}
    public Set<InspireTheme> getInspireThemes() {
        return inspireThemes;
    }
    public void addInspireThemes(final List<InspireTheme> themes) {
        if(themes != null && !themes.isEmpty()) {
            addInspireTheme(themes.iterator().next());
            // TODO: use addAll when we support more than one theme
            //inspireThemes.addAll(themes);
        }
    }
    public void addInspireTheme(final InspireTheme theme) {
        if(theme != null) {
            // TODO: remove the clearing when we support more than one theme
            inspireThemes.clear();
            inspireThemes.add(theme);
        }
    }

    // we only link one group at the moment so get the first one
    public LayerGroup getGroup() {
        if(groups == null || groups.isEmpty()) {
            return null;
        }
        return groups.iterator().next();
    }

    public void addGroup(final LayerGroup group) {
        if(group != null) {
            groups.add(group);
            setGroupId(group.getId());
        }
    }

    public int compareTo(OskariLayer l) {
        String defaultLang = PropertyUtil.getDefaultLanguage();
        return this.getName(defaultLang).compareTo(l.getName(defaultLang));
    }
    /**
     * Returns a simplified version of the wms url. Splits it with comma-character, takes the first one and
     * returns it after removing possible protocol.
     * @return simplified version of wms url or an empty string if there is any problems creating it.
     */
    public String getSimplifiedUrl() {
        if(simplifiedUrl != null) {
            return simplifiedUrl;
        }
        if(url == null) {
            return "";
        }
        final String[] splitted = url.split("\\s*,\\s*");
        if(splitted == null || splitted.length == 0 ) {
            return "";
        }

        final String protocolSeparator = "://";
        final int protocolIndex = splitted[0].indexOf(protocolSeparator);
        if(protocolIndex == -1) {
            // there was no protocol - weird but possible case
            simplifiedUrl = splitted[0].trim();
        }
        else {
            simplifiedUrl = splitted[0].substring(protocolIndex + protocolSeparator.length()).trim();
        }
        return simplifiedUrl;
    }

    public void addSublayer(final OskariLayer layer) {
        if(layer != null) {
            sublayers.add(layer);
        }
    }

    public void addSublayers(List<OskariLayer> layers) {
        sublayers.addAll(layers);
    }

    public List<OskariLayer> getSublayers() {
        return sublayers;
    }

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getOpacity() {
		return opacity;
	}
	public void setOpacity(Integer opacity) {
		this.opacity = opacity;
	}
	public Double getMinScale() {
		return minScale;
	}
	public void setMinScale(Double minScale) {
        if(minScale != null && !minScale.isNaN()) {
		    this.minScale = minScale;
        }
	}
	public Double getMaxScale() {
		return maxScale;
	}
	public void setMaxScale(Double maxScale) {
        if(maxScale != null && !maxScale.isNaN()) {
            this.maxScale = maxScale;
        }
	}

	public String getStyle() {
		return style;
	}
	public void setStyle(String style) {
		this.style = style;
	}
	public String getLegendImage() {
		return legendImage;
	}
	public void setLegendImage(String legendImage) {
		this.legendImage = legendImage;
	}
	public String getTileMatrixSetId() {
		return tileMatrixSetId;
	}

	public void setTileMatrixSetId(String value) {
		tileMatrixSetId = value;
	}

	public String getTileMatrixSetData() {
		return tileMatrixSetData;
	}

	public void setTileMatrixSetData(String value) {
		tileMatrixSetData = value;
	}

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public boolean isBaseMap() {
        return isBaseMap;
    }

    public void setBaseMap(boolean baseMap) {
        isBaseMap = baseMap;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return getUrl(false);
    }

    public String getUrl(final boolean isSecure) {
        if(isSecure) {
            return secureBaseUrl + getSimplifiedUrl();
        }
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        // reset cached simplified url
        this.simplifiedUrl = null;
    }

    public String getMetadataId() {
        return metadataId;
    }

    public void setMetadataId(String metadataId) {
        this.metadataId = metadataId;
    }

    public JSONObject getParams() {
        return params;
    }

    public void setParams(JSONObject params) {
        this.params = params;
    }

    public JSONObject getOptions() {
        return options;
    }

    public void setOptions(JSONObject options) {
        this.options = options;
    }

    public String getGfiType() {
        return gfiType;
    }

    public void setGfiType(String gfiType) {
        this.gfiType = gfiType;
    }

    public String getGfiXslt() {
        return gfiXslt;
    }

    public void setGfiXslt(String gfiXslt) {
        this.gfiXslt = gfiXslt;
    }

    public String getGfiContent() {
        return gfiContent;
    }

    public void setGfiContent(String gfiContent) {
        this.gfiContent = gfiContent;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public boolean getRealtime() { return realtime; }

    public void setRealtime(boolean realtime) { this.realtime = realtime; }

    public int getRefreshRate() { return refreshRate; }

    public void setRefreshRate(int refreshRate) {
        if (refreshRate < 0) {
            this.refreshRate = 0;
        } else {
            this.refreshRate = refreshRate;
        }
    }

	public String getDownloadServiceUrl() {
		return downloadServiceUrl;
	}

	public void setDownloadServiceUrl(String downloadServiceUrl) {
		this.downloadServiceUrl = downloadServiceUrl;
	}
	
	public String getCopyrightInfo() {
		return this.copyrightInfo;
	}

	public void setCopyrightInfo(String copyrightInfo) {
		this.copyrightInfo = copyrightInfo;
	}
}
