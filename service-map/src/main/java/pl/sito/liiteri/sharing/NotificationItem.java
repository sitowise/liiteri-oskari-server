package pl.sito.liiteri.sharing;

public class NotificationItem
{	
	private String content;
	private String title;	
	private String recipient;
	
	public String getContent()
	{
		return content;
	}
	public void setContent(String content)
	{
		this.content = content;
	}
	public String getTitle()
	{
		return title;
	}
	public void setTitle(String title)
	{
		this.title = title;
	}
	public void setRecipient(String recipient)
	{
		this.recipient = recipient;
	}	
	public String getRecipient() {
		return this.recipient;
	}
	@Override
	public String toString()
	{
		return "NotificationItem [content=" + content + ", title=" + title
				+ ", recipient=" + recipient + "]";
	}
	
	
}
