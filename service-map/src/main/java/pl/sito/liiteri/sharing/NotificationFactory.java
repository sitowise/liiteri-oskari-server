package pl.sito.liiteri.sharing;

import java.util.HashMap;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.PropertyUtil;

public class NotificationFactory implements INotificationFactory
{
    static {
        // populate properties before initializing logger since logger implementation is
        // configured in properties
        PropertyUtil.loadProperties("/oskari.properties");
        PropertyUtil.loadProperties("/oskari-ext.properties");
    }
	
	private static final Logger log = LogFactory.getLogger(NotificationFactory.class);
	private HashMap<SharingItem.ResourceType, NotificationConfigItem> _config = new HashMap<SharingItem.ResourceType, NotificationConfigItem>();

	public NotificationFactory() {
		Configure();
	}
	
	public void Configure() {
		NotificationConfigItem workspaceConfig = new NotificationConfigItem();
		workspaceConfig.setServerName(PropertyUtil.get("workspaces.sharing.serverName"));
		workspaceConfig.setTitle(PropertyUtil.get("workspaces.sharing.email.title"));
		workspaceConfig.setContentTextFormat(PropertyUtil.get("workspaces.sharing.email.contentText"));
		workspaceConfig.setPermissionId("workspace_sharing_id");
		_config.put(SharingItem.ResourceType.WORKSPACE, workspaceConfig);
		NotificationConfigItem layerConfig = new NotificationConfigItem();
		layerConfig.setServerName(PropertyUtil.get("userGisData.sharing.serverName"));
		layerConfig.setTitle(PropertyUtil.get("userGisData.sharing.email.title"));
		layerConfig.setContentTextFormat(PropertyUtil.get("userGisData.sharing.email.contentText"));
		layerConfig.setPermissionId("user_gis_data_sharing_id");
		_config.put(SharingItem.ResourceType.LAYER, layerConfig);
		NotificationConfigItem themeConfig = new NotificationConfigItem();
		themeConfig.setServerName(PropertyUtil.get("groupings.sharing.serverName"));
		themeConfig.setTitle(PropertyUtil.get("groupings.sharing.email.title"));
		themeConfig.setContentTextFormat(PropertyUtil.get("groupings.sharing.email.contentText"));
		themeConfig.setPermissionId("permission_id");
		_config.put(SharingItem.ResourceType.THEME, themeConfig);
		_config.put(SharingItem.ResourceType.SERVICE_PACKAGE, themeConfig);
	}
	
	@Override
	public NotificationItem Create(SharingItem item)
	{
		NotificationConfigItem configItem = _config.get(item.getResourceType());
		
		if (configItem == null)
			return null;
		
		NotificationItem result = new NotificationItem();
		result.setTitle(configItem.getTitle());
		result.setRecipient(item.getEmail());
		
		String linkText = configItem.getServerName() + "?" + configItem.getPermissionId() + "=" + item.getPermissionId() + "&email=" + item.getEmail() + "&token=" + item.getToken();
		String contentText = String.format(configItem.getContentTextFormat(), item.getSender().getScreenname(), linkText);
		result.setContent(contentText);		
				
		return result;
	}
	
	public class NotificationConfigItem {
		public String serverName;
		public String title;
		public String contentTextFormat;
		public String permissionId;

		public String getServerName()
		{
			return serverName;
		}
		public void setServerName(String serverName)
		{
			this.serverName = serverName;
		}
		public String getTitle()
		{
			return title;
		}
		public void setTitle(String title)
		{
			this.title = title;
		}
		public String getContentTextFormat()
		{
			return contentTextFormat;
		}
		public void setContentTextFormat(String contentTextFormat)
		{
			this.contentTextFormat = contentTextFormat;
		}
		public String getPermissionId()
		{
			return permissionId;
		}
		public void setPermissionId(String permissionId)
		{
			this.permissionId = permissionId;
		}
	}

}
