package fi.nls.oskari.control.data;

import fi.mml.map.mapwindow.util.OskariLayerWorker;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.userlayer.UserLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.userlayer.domain.GPXGeoJsonCollection;
import fi.nls.oskari.map.userlayer.domain.KMLGeoJsonCollection;
import fi.nls.oskari.map.userlayer.domain.MIFGeoJsonCollection;
import fi.nls.oskari.map.userlayer.domain.SHPGeoJsonCollection;
import fi.nls.oskari.map.userlayer.service.GeoJsonWorker;
import fi.nls.oskari.map.userlayer.service.UserLayerDataService;
import fi.nls.oskari.util.FileHelper;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONObject;

import fi.nls.oskari.map.userowndata.GisDataRoleSettingsDbService;
import fi.nls.oskari.map.userowndata.GisDataRoleSettingsDbServiceImpl;
import fi.nls.oskari.map.userowndata.GisDataService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONObject;

/**
 * Store zipped file set to oskari_user_store database
 */
@OskariActionRoute("CreateUserLayer")
public class CreateUserLayerHandler extends ActionHandler {

    private static final Logger log = LogFactory
            .getLogger(CreateUserLayerHandler.class);
    private final GisDataService gisService = GisDataService.getInstance();
    
    private static final List<String> ACCEPTED_FORMATS = Arrays.asList("SHP", "KML", "GPX", "MIF");
    private static final String IMPORT_SHP = ".SHP";
    private static final String IMPORT_GPX = ".GPX";
    private static final String IMPORT_MIF = ".MIF";
    private static final String IMPORT_KML = ".KML";
    private static final String PARAM_EPSG_KEY = "epsg";
    private static final String PARAM_SOURCE_EPSG_KEY = "sourceEpsg";
    private static final String USERLAYER_MAX_FILE_SIZE_MB = "userlayer.max.filesize.mb";
    private static final String USERLAYER_DEFAULT_SOURCE_EPSG = "userlayer.default.source.epsg";
    private static final int MAX_FILES_IN_ZIP = 100;
    private static final int MAX_BASE_FILENAME_LENGHT = 24;
    final long userlayerMaxFileSizeMb = PropertyUtil.getOptional(USERLAYER_MAX_FILE_SIZE_MB, 10);
    final String defaultSourceEpsg = PropertyUtil.get(USERLAYER_DEFAULT_SOURCE_EPSG, null);
    private final UserLayerDataService userlayerService = new UserLayerDataService();

    /**
     * @param zipFile
     * @return
     * @throws Exception
     */

    private static File unZip(FileItem zipFile) throws Exception {

        FileHelper mainFile = null;

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())){
            ZipEntry ze = zis.getNextEntry();
            String filesBaseName = null;
            int fileCount = 0;
            while (ze != null) {
                fileCount++;
                if (fileCount > MAX_FILES_IN_ZIP) {
                    // safeguard against infinite loop, userlayers shouldn't have this many files in any case
                    break;
                }

                if (ze.isDirectory()) {
                    zis.closeEntry();
                    ze = zis.getNextEntry();
                    continue;
                }
                FileHelper file = handleZipEntry(ze, zis, filesBaseName);
                if (file == null) {
                    zis.closeEntry();
                    ze = zis.getNextEntry();
                    continue;
                }

                // use the same base name for all files in zip
                int i = file.getSavedTo().lastIndexOf(".");
                filesBaseName = file.getSavedTo().substring(0, i);
                //Cut too long basename

                if (mainFile == null && file.isOfType(ACCEPTED_FORMATS)) {
                    mainFile = file;
                }
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
        }

        if (mainFile == null) {
            return null;
        }
        return mainFile.getFile();
    }

    private static FileHelper handleZipEntry(ZipEntry ze, ZipInputStream zis, String tempFileBaseName) {

        FileHelper file = new FileHelper(ze.getName());
        if (!file.hasNameAndExtension()) {
            return null;
        }

        File newFile = null;
        FileOutputStream fos = null;
        try {
            String ftmp = file.getBaseName();
            if (ftmp.length() > MAX_BASE_FILENAME_LENGHT) {
                //Cut too long filename in the middle
                ftmp = file.getBaseName().substring(0, 9) + "_" + file.getBaseName().substring(file.getBaseName().length() - 10);
            } else if(ftmp.length() < 3) {
                // File.createTempFile() "The prefix string must be at least three characters long"
                ftmp = "00" + ftmp;
            }

            newFile = File.createTempFile(ftmp, "." + file.getExtension());
            log.debug("file unzip : " + newFile.getAbsoluteFile());
            fos = new FileOutputStream(newFile);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        } catch (IOException ex) {
            log.warn(ex, "Error unzipping file:", file);
        } finally {
            if (newFile != null) {
                file.setSavedTo(newFile.getPath());
                newFile.deleteOnExit();
            }
            IOHelper.close(fos);
        }

        if (tempFileBaseName != null) {
            // rename tempfile if we have an existing basename (shapefiles need to have same basename)
            final String newName = tempFileBaseName + "." + file.getExtension();
            File renameLocation = new File(newName);
            boolean success = newFile.renameTo(renameLocation);
            if (!success) {
                //TODO: if shapefile contains more than one layer -> warn user that zip should contain only one layer (shp + dbf + shx)
                log.warn("Rename failed in temp directory", newFile.getName(), " ->  ", newName);
            } else {
                // update path
                file.setSavedTo(renameLocation.getPath());
                log.debug("File renamed ", newFile.getName(), " ->  ", newName);
            }
        }
        return file;
    }

    @Override
    public void handleAction(ActionParameters params) throws ActionException {

        // stop here if user isn't logged in
        params.requireLoggedInUser();

        final String target_epsg = params.getHttpParam(PARAM_EPSG_KEY, "EPSG:3067");
        final String source_epsg = params.getHttpParam(PARAM_SOURCE_EPSG_KEY, defaultSourceEpsg);
        try {
        	User user = params.getUser();

            // Only 1st file item is handled
            RawUpLoadItem loadItem = getZipFiles(params);
            

            // Checks file size

            File file = unZip(loadItem.getFileitem());

            if (file == null) {
                log.error("Couldn't find valid import file in zip file");
                throw new ActionParamsException("invalid_file");
            }
            
            //check limitations
        	if (!gisService.canUserAddNewDataset(user, ((float)loadItem.getFileitem().getSize() / (1024 * 1024)))) {
        		throw new ActionException("server_error_key_limit");
        	}

            // import format
            GeoJsonWorker geojsonWorker = null;

            if (file.getName().toUpperCase().indexOf(IMPORT_SHP) > -1) {
                geojsonWorker = new SHPGeoJsonCollection();
            } else if (file.getName().toUpperCase().indexOf(IMPORT_KML) > -1) {
                geojsonWorker = new KMLGeoJsonCollection();
            } else if (file.getName().toUpperCase().indexOf(IMPORT_GPX) > -1) {
                geojsonWorker = new GPXGeoJsonCollection();
            } else if (file.getName().toUpperCase().indexOf(IMPORT_MIF) > -1) {
                geojsonWorker = new MIFGeoJsonCollection();
            }
            // Parse import data to geojson
            String status = geojsonWorker.parseGeoJSON(file, source_epsg, target_epsg);
            if (status != null) {
                throw new ActionException(status);
            }


            // Store geojson via Mybatis
            UserLayer ulayer = userlayerService.storeUserData(geojsonWorker, user, loadItem.getFparams());
            
            // Store failed
            if (ulayer == null) {
                log.error("Couldn't store user data into database or no features in the input data");
                throw new ActionException("unable_to_store_data");
            }

            //create reference to user layer in oskari_user_gis_data table
            String dataId = "userlayer_" + ulayer.getId();
            try {
            	gisService.insertUserGisData(params.getUser(), dataId, "IMPORTED_PLACES", null);            	
    		} catch (Exception e) {
    			//TODO: should we suppress it?
    			log.warn(e, "Cannot save user gis data");
    		}
            
            // workaround because of IE iframe submit json download functionality
            //params.getResponse().setContentType("application/json;charset=utf-8");
            //ResponseHelper.writeResponse(params, userlayerService.parseUserLayer2JSON(ulayer));
            params.getResponse().setContentType("text/plain;charset=utf-8");
            params.getResponse().setCharacterEncoding("UTF-8");

            // workaround because of IE iframe submit json download functionality
            //params.getResponse().setContentType("application/json;charset=utf-8");
            //ResponseHelper.writeResponse(params, userlayerService.parseUserLayer2JSON(ulayer));
            final HttpServletResponse response = params.getResponse();
            response.setContentType("text/plain;charset=utf-8");
            response.setCharacterEncoding("UTF-8");

            JSONObject userLayer = userlayerService.parseUserLayer2JSON(ulayer);
            JSONHelper.putValue(userLayer, "featuresCount", ulayer.getFeatures_count());
            JSONObject permissions = OskariLayerWorker.getAllowedPermissions();
            JSONHelper.putValue(userLayer, "permissions", permissions);
            //add warning if features were skipped
            if (ulayer.getFeatures_skipped() > 0) {
                JSONObject featuresSkipped = new JSONObject();
                JSONHelper.putValue(featuresSkipped, "featuresSkipped", ulayer.getFeatures_skipped());
                JSONHelper.putValue(userLayer, "warning", featuresSkipped);
            }
            response.getWriter().print(userLayer);
        } catch (Exception e) {
            if (e instanceof ActionException) {
                throw (ActionException) e;
            }
            throw new ActionException(e.getMessage(), e);
        }

    }

    /**
     * unzip and write shp files as temp filesz
     *
     * @param params
     * @return File of import master file of  file set  (.shp, .kml, mif)
     */
    private RawUpLoadItem getZipFiles(ActionParameters params) throws ActionException {

        FileItem impFileItem = null;
        HttpServletRequest request = params.getRequest();

        try {
            // Incoming strings are in UTF-8 but they're not read as such unless we force it...
            request.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.warn("Couldn't setup UTF-8 encoding");
        }

        Map<String, String> fparams = new HashMap();
        RawUpLoadItem loadItem = new RawUpLoadItem();

        try {
            request.setCharacterEncoding("UTF-8");

            if (request.getContentType().indexOf("multipart") > -1) {

                DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();

                /*
                * Set the file size limit in bytes. This should be set as an
                * initialization parameter
                */
                // diskFileItemFactory.setSizeThreshold(1024 * 1024 * 10);

                // Create a new file upload handler
                ServletFileUpload upload = new ServletFileUpload(diskFileItemFactory);

                // Set size limit
                upload.setSizeMax(userlayerMaxFileSizeMb * 1024 * 1024);

                List items = null;

                try {
                    items = upload.parseRequest(request);
                } catch (FileUploadException ex) {
                    log.error("Could not parse request", ex); //file_over_size
                }
                try {
                    ListIterator li = items.listIterator();

                    while (li.hasNext()) {
                        FileItem fileItem = (FileItem) li.next();
                        if (!fileItem.isFormField()) {
                            // Take only 1st one
                            if (impFileItem == null) impFileItem = fileItem;

                        } else {
                            fparams.put(fileItem.getFieldName(), fileItem.getString("UTF-8"));
                        }
                    }
                } catch (Exception ex) {
                    log.error("Could not parse request", ex);
                }
            }
        } catch (Exception ex) {
            log.error("Could not parse request", ex);
        }
        loadItem.setFileitem(impFileItem);
        loadItem.setFparams(fparams);

        return loadItem;
    }

    class RawUpLoadItem {
        FileItem fileitem;
        Map<String, String> fparams;
        String imp_extension;

        FileItem getFileitem() {
            return fileitem;
        }

        void setFileitem(FileItem fileitem) {
            this.fileitem = fileitem;
        }

        Map<String, String> getFparams() {
            return fparams;
        }

        void setFparams(Map<String, String> fparams) {
            this.fparams = fparams;
        }

        String getImp_extension() {
            return imp_extension;
        }

        void setImp_extension(String imp_extension) {
            this.imp_extension = imp_extension;
        }
    }
}
