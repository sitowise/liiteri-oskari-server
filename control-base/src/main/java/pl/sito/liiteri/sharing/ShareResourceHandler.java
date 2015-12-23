package pl.sito.liiteri.sharing;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("ShareResource")
public class ShareResourceHandler extends ActionHandler {
	
	private static final Logger log = LogFactory.getLogger(ShareResourceHandler.class);
	private static final SharingService _sharingService = SharingService.getInstance();
	
	private static final String PERMISSION_ID_PARAM = "permissionId";
	private static final String TOKEN_PARAM = "token";
	
	@Override
	public void handleAction(ActionParameters params) throws ActionException
	{
		User user = params.getUser();
		if (user.isGuest())
			throw new ActionDeniedException("Operation cannot be conducted by guest user");
		
		String permissionIdString = params.getRequiredParam(PERMISSION_ID_PARAM);
		String token = params.getRequiredParam(TOKEN_PARAM);
		
		long permissionId = 0;
		
		try {
			permissionId = Long.parseLong(permissionIdString);	
		} catch(NumberFormatException e) {
			throw new ActionParamsException(PERMISSION_ID_PARAM);
		}
						
		try {
			_sharingService.ShareToUser(permissionId, token, user);	
		} catch (ServiceException e) {
			throw new ActionException(e.getMessage(), e);
		}
		
	}
}
