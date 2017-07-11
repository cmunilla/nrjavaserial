package gnu.io;

import gnu.io.internal.CommPortIdentifier;

/**
 * Signature service listening for {@link PortLifecycleEvent}s
 */
public interface PortLifecycleListener 
{
	/**
	 * the PortLifecycleListener is informed about appearance
	 * or disappearance of the port whose linked {@link CommPortIdentifier}
	 * is defined as the source object of the {@link PortLifecycleEvent}
	 * passed as parameter
	 *  
	 * @param event
	 * 		the the {@link PortLifecycleEvent}
	 */
	void lifecycleEvent(PortLifecycleEvent event);
}
