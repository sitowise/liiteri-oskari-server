package fi.nls.oskari.control.groupings;

import fi.nls.oskari.domain.map.MaplayerGroup;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.map.layer.group.link.OskariLayerGroupLink;
import fi.nls.oskari.util.PropertyUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.domain.groupings.*;
import fi.nls.oskari.groupings.utils.DataType;
import fi.nls.oskari.groupings.utils.GroupingCollectionHelper;
import fi.nls.oskari.groupings.utils.GroupingStatus;
import fi.nls.oskari.groupings.utils.ThemeType;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONHelper;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class JSONGroupingsHelper {

	private static final Logger log = LogFactory
			.getLogger(JSONGroupingsHelper.class);

	public static final JSONObject createGroupingsJSONObject(
			List<Grouping> groupings, List<GroupingTheme> groupingThemes,
			List<GroupingThemeData> data, List<GroupingPermission> allUsers,
			List<GroupingPermission> allRoles,
			HashMap<Long, String> indicatorNames) throws JSONException {
		final JSONObject main = new JSONObject();
		JSONArray JSONgroupings = new JSONArray();
		PropertyUtil.loadProperties("/oskari-ext.properties");
		String groupingUrl = PropertyUtil.get("grouping.url", "");

		for (Grouping g : groupings) {
			JSONObject grMain = new JSONObject();
			JSONHelper.putValue(grMain, "id", g.getId());
			JSONHelper.putValue(grMain, "name", g.getName());
			JSONHelper.putValue(grMain, "label", g.getLabel());
			JSONHelper.putValue(grMain, "state", GroupingStatus
					.getInstanceFromCodeValue(g.getStatus()).getName());
			JSONHelper.putValue(grMain, "mainType", "package");
			JSONHelper.putNullAwareValue(grMain, "description", g.getDescription());
			JSONHelper.putValue(grMain, "userGroup", g.getUserGroup());
			if (g.getCreated() != null) {
				JSONHelper.putValue(grMain, "created", new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss").format(g.getCreated()));
			}
			if (g.getUpdated() != null) {
				JSONHelper.putValue(grMain, "updated", new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss").format(g.getUpdated()));
			}
			if(g.getMapState()!= null)
				JSONHelper.putValue(grMain, "mapState", g.getMapState());

			if ((groupingUrl != null)&&(groupingUrl.length() > 0))
				JSONHelper.putValue(grMain, "url", groupingUrl);

			List<GroupingTheme> mainThemesForGrouping = GroupingCollectionHelper
					.findMainThemesForGrouping(g.getId(), groupingThemes);

			JSONArray JSONThemes = new JSONArray();
			for (GroupingTheme gt : mainThemesForGrouping) {
				JSONThemes
						.put(createThemeJSONObject(gt, groupingThemes, data, indicatorNames));
			}
			JSONHelper.putValue(grMain, "themes", JSONThemes);
			addJSONGroupingPermissions(grMain, allUsers, allRoles, g.getId());
			JSONgroupings.put(grMain);

		}

		for (GroupingTheme t : GroupingCollectionHelper
				.findUnbindedMainThemes(groupingThemes)) {
			/*
			 * JSONObject thMain = new JSONObject(); JSONHelper.putValue(thMain,
			 * "name", t.getName()); JSONHelper.putValue(thMain, "type",
			 * ThemeType .getInstanceFromCodeValue(t.getThemeType()).getName());
			 * JSONHelper.putValue(thMain, "mainType", "theme"); JSONArray
			 * JSONThemes = new JSONArray();
			 *
			 * JSONThemes.put(crerateThemeJSONObject(t, groupingThemes, data));
			 * List<GroupingThemeData> grData = GroupingCollectionHelper
			 * .findGroupingThemeData(t.getId(), data); if (grData.size() > 0) {
			 * JSONArray JSONgroupingsData = new JSONArray(); for
			 * (GroupingThemeData d : grData) { JSONObject obData = new
			 * JSONObject(); JSONHelper.putValue(obData, "id", d.getDataId());
			 * JSONHelper.putValue(obData, "type", DataType
			 * .getInstanceFromCodeValue(d.getDataType()) .getName());
			 * JSONgroupingsData.put(obData); } JSONHelper.putValue(thMain,
			 * "elements", JSONgroupingsData); } JSONHelper.putValue(thMain,
			 * "themes", JSONThemes);
			 */
			JSONObject thMain = createThemeJSONObject(t, groupingThemes, data, indicatorNames);
			JSONHelper.putValue(thMain, "mainType", "theme");
			JSONHelper.putValue(thMain, "id", t.getId());
			JSONHelper.putValue(thMain, "state", GroupingStatus.getInstanceFromCodeValue(t.getStatus()).getName());

			addJSONGroupingPermissions(thMain, allUsers, allRoles, t.getId());

			JSONgroupings.put(thMain);

		}

		JSONHelper.putValue(main, "groupings", JSONgroupings);

		return main;
	}

	public static final JSONObject createThemeGroupingsJSONObject(
			List<Grouping> groupings, List<GroupingTheme> groupingThemes,
			List<GroupingThemeData> data, List<GroupingPermission> allUsers,
			List<GroupingPermission> allRoles, HashMap<Long, String> indicatorNames,
			List<OskariLayer> layers, Map<Integer, List<MaplayerGroup>> groupsByParentId,
			Map<Integer, List<OskariLayerGroupLink>>linksByGroupId) throws Exception {
		final JSONObject main = new JSONObject();
		JSONArray JSONgroupings = new JSONArray();

		for (Grouping g : groupings) {
			JSONObject grMain = new JSONObject();
			JSONHelper.putValue(grMain, "mainType", "package");
			JSONHelper.putValue(grMain, "id", g.getId());
			JSONHelper.putValue(grMain, "name", g.getName());
			JSONHelper.putValue(grMain, "label", g.getLabel());

			//addJSONGroupingPermissions(grMain, allUsers, allRoles, g.getId());
			JSONgroupings.put(grMain);
		}

		for (GroupingTheme t : GroupingCollectionHelper
				.findUnbindedMainThemes(groupingThemes)) {
			JSONObject thMain = createThemeJSONObject(t, groupingThemes, data, indicatorNames);
			JSONHelper.putValue(thMain, "mainType", "theme");
			JSONHelper.putValue(thMain, "id", t.getId());
			JSONHelper.putValue(thMain, "state", GroupingStatus.getInstanceFromCodeValue(t.getStatus()).getName());
			
			//addJSONGroupingPermissions(thMain, allUsers, allRoles, t.getId());
			
			JSONgroupings.put(thMain);
		}

		int[] sortedLayerIds = layers.stream().mapToInt(OskariLayer::getId).toArray();
		Arrays.sort(sortedLayerIds);
		JSONArray jsonArray = getGroupJSON(groupsByParentId, linksByGroupId, sortedLayerIds, -1, layers);

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONgroupings.put(jsonArray.getJSONObject(i));
		}
		JSONHelper.putValue(main, "groupings", JSONgroupings);

		return main;
	}

	public static JSONArray getGroupJSON(final Map<Integer, List<MaplayerGroup>> groupsByParentId,
								   final Map<Integer, List<OskariLayerGroupLink>> linksByGroupId,
								   final int[] sortedLayerIds,
								   final int parentGroupId,
										 final List<OskariLayer> layers) throws Exception {
		List<MaplayerGroup> groups = groupsByParentId.get(parentGroupId);
		if (groups == null || groups.isEmpty()) {
			return null;
		}

		JSONArray json = new JSONArray();
		groups.sort(Comparator.comparing(MaplayerGroup::getId));
		for (MaplayerGroup group : groups) {
			int groupId = group.getId();

			JSONObject groupAsJson = new JSONObject();
			groupAsJson.put("name", group.getName("fi"));
			groupAsJson.put("type", "map_layers");
			groupAsJson.put("mainType", "theme");
			groupAsJson.put("id", groupId);
			//groupAsJson.put("permissions", new int[]{});
			//groupAsJson.put("isPublic", true);

			JSONArray subGroups = getGroupJSON(groupsByParentId, linksByGroupId, sortedLayerIds, groupId, layers);
			if (subGroups != null) {
				groupAsJson.put("themes", subGroups);
			}

			List<OskariLayerGroupLink> groupLinks = linksByGroupId.get(groupId);
			if (groupLinks != null && !groupLinks.isEmpty()) {
				List<OskariLayerGroupLink> groupLayers = groupLinks.stream()
						.filter(l -> contains(sortedLayerIds, l.getLayerId()))
						.sorted(Comparator.comparingInt(OskariLayerGroupLink::getGroupId))
						.collect(Collectors.toList());
				if (!groupLayers.isEmpty()) {
					groupAsJson.put("elements", getLayersJSON(groupLayers, layers));
				}
			}

			json.put(groupAsJson);
		}
		return json;
	}

	private static JSONArray getLayersJSON(List<OskariLayerGroupLink> groupLayers, List<OskariLayer> layers) throws Exception {
		JSONArray groupLayersJSON = new JSONArray();

		for(OskariLayerGroupLink groupLayer: groupLayers) {
			JSONObject groupLayerJSON = new JSONObject();
			groupLayerJSON.put("id", groupLayer.getLayerId());
			groupLayerJSON.put("type", "map_layer");
			groupLayerJSON.put("name", getLayerName(layers, groupLayer.getLayerId()));
			groupLayersJSON.put(groupLayerJSON);
		}

		return groupLayersJSON;
	}

	private static JSONObject getLayerName(List<OskariLayer> layers, int layerId) throws Exception {

		OskariLayer layer = layers.stream()
				.filter(l -> l.getId() == layerId)
				.findAny()
				.orElse(null);
		if (layer == null)
			throw new Exception("Layer not found");

		return layer.getLocale();
	}

	private static boolean contains(int[] sortedLayerIds, int layerId) {
		return Arrays.binarySearch(sortedLayerIds, layerId) >= 0;
	}

	public static final JSONObject createUnbindedThemesJSONObject(
			List<GroupingTheme> themes, List<GroupingThemeData> data,
			HashMap<Long, String> indicatorNames) {
		final JSONObject main = new JSONObject();
		JSONArray JSONThemes = new JSONArray();
		for (GroupingTheme t : GroupingCollectionHelper
				.findUnbindedMainThemes(themes)) {

			JSONObject thMain = createThemeJSONObject(t, themes, data, indicatorNames);
			JSONHelper.putValue(thMain, "mainType", "theme");
			JSONHelper.putValue(thMain, "id", t.getId());
			JSONThemes.put(thMain);
		}
		JSONHelper.putValue(main, "themes", JSONThemes);
		return main;
	}

	private static void addJSONGroupingPermissions(JSONObject grouping,
			List<GroupingPermission> allUsers,
			List<GroupingPermission> allRoles, long oskariGroupingId) {
		JSONArray JSONPermissions = new JSONArray();
		List<GroupingPermission> users = GroupingCollectionHelper
				.findGroupingPermissions(allUsers, oskariGroupingId);
		List<GroupingPermission> roles = GroupingCollectionHelper
				.findGroupingPermissions(allRoles, oskariGroupingId);
		for (GroupingPermission u : users) {
			JSONPermissions.put(buildJSONGroupingPermission(u));
		}
		for (GroupingPermission r : roles) {
			JSONPermissions.put(buildJSONGroupingPermission(r));
		}

		JSONHelper.putValue(grouping, "permissions", JSONPermissions);

	}

	private static JSONObject buildJSONGroupingPermission(
			GroupingPermission groupingPermission) {
		JSONObject ob = new JSONObject();
		JSONHelper.putValue(ob, "permissionId", groupingPermission.getId());
		JSONHelper.putValue(ob, "id", groupingPermission.getExternalId());
		JSONHelper.putValue(ob, "type", groupingPermission.getExternalType()
				.toLowerCase());
		JSONHelper.putValue(ob, "name", groupingPermission.getName());
		JSONHelper.putValue(ob, "email", groupingPermission.getEmail());
		return ob;
	}

	private static JSONObject createThemeJSONObject(GroupingTheme theme,
			List<GroupingTheme> groupingThemes, List<GroupingThemeData> data,
			HashMap<Long, String> indicatorNames) {
		JSONObject ob = new JSONObject();
		JSONHelper.putValue(ob, "name", theme.getName());
		JSONHelper.putValue(ob, "type",
				ThemeType.getInstanceFromCodeValue(theme.getThemeType())
						.getName());
		if (theme.isPublic() != null) {
			JSONHelper.putValue(ob, "isPublic", theme.isPublic());
		}
		List<GroupingTheme> subthemes = theme.getOskariGroupingId() != null ? GroupingCollectionHelper
				.findSubthemes(theme.getId(), theme.getOskariGroupingId(),
						groupingThemes) : GroupingCollectionHelper
				.findUnbindedMainThemeSubthemes(theme.getId(), theme
						.getMainThemeId() != null ? theme.getMainThemeId()
						: theme.getId(), groupingThemes);
				
		if(theme.getThemeType() == ThemeType.STAT.getCode()) {
			subthemes = GroupingCollectionHelper.findSubthemes(theme.getId(), groupingThemes);
		}
				
		if (subthemes.size() > 0) {
			JSONArray JSONgroupings = new JSONArray();
			for (GroupingTheme gt : subthemes) {
				JSONgroupings.put(createThemeJSONObject(gt, groupingThemes,
						data, indicatorNames));
			}

			JSONHelper.putValue(ob, "themes", JSONgroupings);
		} else {
			List<GroupingThemeData> grData = GroupingCollectionHelper
					.findGroupingThemeData(theme.getId(), data);
			if (grData.size() > 0) {
				JSONArray JSONgroupingsData = new JSONArray();
				for (GroupingThemeData d : grData) {
					JSONObject obData = new JSONObject();
					JSONHelper.putValue(obData, "id", d.getDataId());
					JSONHelper.putValue(obData, "type", DataType
							.getInstanceFromCodeValue(d.getDataType())
							.getName());
					JSONHelper.putValue(obData, "status", d.getStatus());
					if(d.getDataType() == DataType.MAP.getCode() && d.getName() != null) { 
						JSONHelper.putValue(obData, "name", JSONHelper.createJSONObject(d.getName()));
					} else if(d.getDataType() == DataType.STAT.getCode() && indicatorNames != null) {
						String name = indicatorNames.get((Long)d.getDataId());
						if(name != null) {
							JSONHelper.putValue(obData, "name", JSONHelper.createJSONObject(name));
						}
					}
					JSONgroupingsData.put(obData);
				}
				JSONHelper.putValue(ob, "elements", JSONgroupingsData);
			}
		}
		
		

		return ob;
	}

	public static final Grouping buildServicePackageObject(String json)
			throws JSONException {
		Grouping grouping = new Grouping();
		JSONObject mainJSON = JSONHelper.createJSONObject(json);
		grouping.setName(mainJSON.getString("name"));
		grouping.setLabel(mainJSON.getString("label"));
		//grouping.setStatus(GroupingStatus.getInstanceFromName(
		//		mainJSON.getString("state")).getCode());
		grouping.setId(mainJSON.optLong("id"));
		
		if (mainJSON.has("userGroup")) {
			grouping.setUserGroup(mainJSON.getString("userGroup"));
		}

		if (mainJSON.has("description")) {
			grouping.setDescription(mainJSON.optString("description"));
		}
		JSONArray themes = mainJSON.getJSONArray("themes");

		JSONArray permissions = mainJSON.optJSONArray("permissions");
		if (permissions != null) {
			for (int i = 0; i < permissions.length(); i++) {
				grouping.getPermissions().add(
						buildGroupingPermission(permissions.getJSONObject(i), false));
			}
		}
		
		int status = 2;
		
		for (GroupingPermission gp : grouping.getPermissions()) {
			if (gp.getExternalType().equals("ROLE")) {
				status = 3;
				break;
			}
		}
		
		grouping.setStatus(status);

		PropertyUtil.loadProperties("/oskari-ext.properties");
		String groupingUrl = PropertyUtil.get("grouping.url", "");
		if ((groupingUrl != null)&&(groupingUrl.length() > 0)){
			grouping.setUrl(groupingUrl);
		}

		for (int i = 0; i < themes.length(); i++) {
			JSONObject JSONDataObj = themes.getJSONObject(i);
			grouping.getThemes().add(buildThemeObject(JSONDataObj));
		}
		return grouping;
	}

	public static final GroupingTheme buildUnbindedMainThemeObject(String json)
			throws JSONException {
		GroupingTheme theme = new GroupingTheme();
		JSONObject mainJSON = JSONHelper.createJSONObject(json);
		theme.setName(mainJSON.getString("name"));
		theme.setThemeType(ThemeType.getInstanceFromName(
				mainJSON.getString("type")).getCode());
		theme.setId(mainJSON.optLong("id"));
		theme.setPublic(mainJSON.optBoolean("isPublic"));
		
		JSONArray permissions = mainJSON.optJSONArray("permissions");
		if (permissions != null) {
			for (int i = 0; i < permissions.length(); i++) {
				theme.getPermissions().add(
						buildGroupingPermission(permissions.getJSONObject(i), true));
			}
		}
		
		int status = 2;
		if (theme.isPublic()) {
			status = 3;
		}
		theme.setStatus(status);

		JSONArray themes = mainJSON.optJSONArray("themes");
		if (themes != null) {
			for (int i = 0; i < themes.length(); i++) {
				JSONObject JSONDataObj = themes.getJSONObject(i);
				theme.getSubThemes().add(buildThemeObject(JSONDataObj));
			}
		}
		JSONArray data = mainJSON.optJSONArray("elements");

		if (data != null && data.length() > 0) {
			for (int i = 0; i < data.length(); i++) {
				GroupingThemeData dataObj = new GroupingThemeData();
				JSONObject JSONDataObj = data.getJSONObject(i);

				dataObj.setDataId(JSONDataObj.getLong("id"));
				dataObj.setDataType(DataType.getInstanceFromName(
						JSONDataObj.getString("type")).getCode());
				// dataObj.setName(JSONDataObj.getString("name"));
				theme.getThemeData().add(dataObj);
			}

		}
		return theme;
	}

	private static GroupingTheme buildThemeObject(JSONObject JSONTheme)
			throws JSONException {
		GroupingTheme theme = new GroupingTheme();
		theme.setName(JSONTheme.getString("name"));
		theme.setThemeType(ThemeType.getInstanceFromName(
				JSONTheme.getString("type")).getCode());
		JSONArray subThemes = JSONTheme.optJSONArray("themes");
		JSONArray data = JSONTheme.optJSONArray("elements");

		if (subThemes != null && subThemes.length() > 0) {
			for (int i = 0; i < subThemes.length(); i++) {
				JSONObject JSONDataObj = subThemes.getJSONObject(i);
				theme.getSubThemes().add(buildThemeObject(JSONDataObj));
			}
		}

		if (data != null && data.length() > 0) {
			for (int i = 0; i < data.length(); i++) {
				GroupingThemeData dataObj = new GroupingThemeData();
				JSONObject JSONDataObj = data.getJSONObject(i);

				dataObj.setDataId(JSONDataObj.getLong("id"));
				dataObj.setDataType(DataType.getInstanceFromName(
						JSONDataObj.getString("type")).getCode());
				if(JSONDataObj.has("status"))
				{
					dataObj.setStatus(JSONDataObj.getString("status"));
				}
				// dataObj.setName(JSONDataObj.getString("name"));
				theme.getThemeData().add(dataObj);
			}

		}
		return theme;

	}

	private static GroupingPermission buildGroupingPermission(
			JSONObject JSONGroupingPermission, Boolean isTheme) throws JSONException {
		GroupingPermission ob = new GroupingPermission();
		
		if (JSONGroupingPermission.has("permissionId"))
			ob.setId(JSONGroupingPermission.getLong("permissionId"));
		
		long id = JSONGroupingPermission.optLong("id", 0);
		
		if (id != 0) {			
			ob.setExternalId(id);
		} else {
			//in this case there is given only email address (user ID is unknown yet)
			ob.setExternalId(0);
			ob.setEmail(JSONGroupingPermission.getString("email"));
		}
		
		ob.setExternalType(JSONGroupingPermission.getString("type").toUpperCase());
		ob.setTheme(isTheme);
		
		return ob;
	}

}
