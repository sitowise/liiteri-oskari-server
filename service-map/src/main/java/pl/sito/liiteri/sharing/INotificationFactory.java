package pl.sito.liiteri.sharing;

import fi.nls.oskari.domain.User;

public interface INotificationFactory
{
	public void Configure();
	public NotificationItem Create(SharingItem item);
}
