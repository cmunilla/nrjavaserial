package gnu.io;

import java.util.EventObject;

/**
 * {@link EventObject} dedicated to appearance and disappearance
 * port events
 */
public class PortLifecycleEvent extends EventObject {

	public static final int PORT_DISCOVERED = 0;
	public static final int PORT_DISAPPEARED = 1;
	
	private final int eventType;
	
	/**
	 * Constructor
	 * 
	 * @param commPortIdentifier	
	 * 		the source object
	 * @param eventType
	 * 		the integer identifier of the type of event
	 */
	public PortLifecycleEvent(String name, int eventType) 
	{
		super(name);
		this.eventType = eventType;
	}
	
	/**
	 * Returns the integer identifier of the type of event
	 * 
	 * @return
	 * 		the integer identifier of the type of event
	 */
	public int getEventType()
	{
		return this.eventType;
	}

}
