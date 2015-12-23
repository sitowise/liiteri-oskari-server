package fi.nls.oskari.control;

import fi.nls.oskari.util.PropertyUtil;

public abstract class AuthorizedActionHandler extends ActionHandler
{
	private String[] permittedRoles = new String[0];
	
	@Override
    public void init() {
		super.init();
		
		String propertyKey = "actionhandler." + getName() + ".roles";
		permittedRoles = PropertyUtil.getCommaSeparatedList(propertyKey);
    }

	@Override
	public final void handleAction(ActionParameters params) throws ActionException
	{				
		if (permittedRoles.length > 0 && !params.getUser().hasAnyRoleIn(permittedRoles)) {
    	  	throw new ActionDeniedException("Unauthorized user tried access following route: " + getName());
      	}
		
      	handleAuthorizedAction(params);
	}

	public abstract void handleAuthorizedAction(ActionParameters params) throws ActionException;
}
