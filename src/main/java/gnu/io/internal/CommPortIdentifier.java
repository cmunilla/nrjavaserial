/*-------------------------------------------------------------------------
|   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
|   RXTX is a native interface to serial ports in java.
|   Copyright 1997-2007 by Trent Jarvi tjarvi@qbang.org and others who
|   actually wrote it.  See individual source files for more information.
|
|   A copy of the LGPL v 2.1 may be found at
|   http://www.gnu.org/licenses/lgpl.txt on March 4th 2007.  A copy is
|   here for your convenience.
|
|   This library is free software; you can redistribute it and/or
|   modify it under the terms of the GNU Lesser General Public
|   License as published by the Free Software Foundation; either
|   version 2.1 of the License, or (at your option) any later version.
|
|   This library is distributed in the hope that it will be useful,
|   but WITHOUT ANY WARRANTY; without even the implied warranty of
|   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
|   Lesser General Public License for more details.
|
|   An executable that contains no derivative of any portion of RXTX, but
|   is designed to work with RXTX by being dynamically linked with it,
|   is considered a "work that uses the Library" subject to the terms and
|   conditions of the GNU Lesser General Public License.
|
|   The following has been added to the RXTX License to remove
|   any confusion about linking to RXTX.   We want to allow in part what
|   section 5, paragraph 2 of the LGPL does not permit in the special
|   case of linking over a controlled interface.  The intent is to add a
|   Java Specification Request or standards body defined interface in the 
|   future as another exception but one is not currently available.
|
|   http://www.fsf.org/licenses/gpl-faq.html#LinkingOverControlledInterface
|
|   As a special exception, the copyright holders of RXTX give you
|   permission to link RXTX with independent modules that communicate with
|   RXTX solely through the Sun Microsytems CommAPI interface version 2,
|   regardless of the license terms of these independent modules, and to copy
|   and distribute the resulting combined work under terms of your choice,
|   provided that every copy of the combined work is accompanied by a complete
|   copy of the source code of RXTX (the version of RXTX used to produce the
|   combined work), being distributed under the terms of the GNU Lesser General
|   Public License plus this exception.  An independent module is a
|   module which is not derived from or based on RXTX.
|
|   Note that people who make modified versions of RXTX are not obligated
|   to grant this special exception for their modified versions; it is
|   their choice whether to do so.  The GNU Lesser General Public License
|   gives permission to release a modified version without this exception; this
|   exception also makes it possible to release a modified version which
|   carries forward this exception.
|
|   You should have received a copy of the GNU Lesser General Public
|   License along with this library; if not, write to the Free
|   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
|   All trademarks belong to their respective owners.
--------------------------------------------------------------------------*/
package  gnu.io.internal;

import gnu.io.CommPort;
import gnu.io.CommPortOwnershipListener;

import java.util.Enumeration;
import java.util.Vector;

/**
* @author Trent Jarvi
* @version %I%, %G%
* @since JDK1.0
*/

public class CommPortIdentifier
{	
	native String native_psmisc_report_owner(String PortName);
	
	private String PortName;
	private int PortType;
	
	private boolean Available = true;	
	private String Owner;    
	
	private CommPort commport;	
	private CommPortIdentifier next;
	
	@SuppressWarnings("rawtypes")
	Vector ownershipListener;



/*------------------------------------------------------------------------------
	static {}   aka initialization
	accept:       -
	perform:      load the rxtx driver
	return:       -
	exceptions:   Throwable
	comments:     static block to initialize the class
------------------------------------------------------------------------------*/
	
	CommPortIdentifier ( String pn, CommPort cp, int pt) 
	{
		setPortName(pn);
		setCommport(cp);
		PortType        = pt;
		setNext(null);
	}


/*------------------------------------------------------------------------------
	addPortOwnershipListener()
	accept:
	perform:
	return:
	exceptions:
	comments:   
------------------------------------------------------------------------------*/
	@SuppressWarnings("unchecked")
	public void addPortOwnershipListener(CommPortOwnershipListener c) 
	{ 
		/*  is the Vector instantiated? */

		if( ownershipListener == null )
		{
			ownershipListener = new Vector();
		}

		/* is the ownership listener already in the list? */

		if ( ownershipListener.contains(c) == false)
		{
			ownershipListener.addElement(c);
		}
	}
/*------------------------------------------------------------------------------
	getCurrentOwner()
	accept:
	perform:
	return:
	exceptions:
	comments:    
------------------------------------------------------------------------------*/
	public String getCurrentOwner() 
	{ 
		return( getOwner() );
	}
/*------------------------------------------------------------------------------
	getName()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	public String getName() 
	{ 
		return( getPortName() );
	}
	
/*------------------------------------------------------------------------------
	getPortType()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	public int getPortType() 
	{ 
		return( PortType );
	}
/*------------------------------------------------------------------------------
	isCurrentlyOwned()
	accept:
	perform:
	return:
	exceptions:
	comments:    
------------------------------------------------------------------------------*/
	public synchronized boolean isCurrentlyOwned() 
	{ 
		return(!isAvailable());
	}

/*------------------------------------------------------------------------------
	removePortOwnership()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	public void removePortOwnershipListener(CommPortOwnershipListener c) 
	{ 
		/* why is this called twice? */
		if(ownershipListener != null)
		{
			ownershipListener.removeElement(c);
		}
	}

/*------------------------------------------------------------------------------
	internalClosePort()
	accept:       None
	perform:      clean up the Ownership information and send the event
	return:       None
	exceptions:   None
	comments:     None
------------------------------------------------------------------------------*/
	void internalClosePort() 
	{
		synchronized(this) 
		{	
			setOwner(null);
			setAvailable(true);
			setCommport(null);
			
			/*  this tosses null pointer?? */
			notifyAll();
		}
		fireOwnershipEvent(CommPortOwnershipListener.PORT_UNOWNED);
	}
/*------------------------------------------------------------------------------
	fireOwnershipEvent()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	void fireOwnershipEvent(int eventType)
	{
		if (ownershipListener != null)
		{
			CommPortOwnershipListener c;
			for ( Enumeration e = ownershipListener.elements();
				e.hasMoreElements(); 
				c.ownershipChange(eventType))
				c = (CommPortOwnershipListener) e.nextElement();
		}
	}

	/**
	 * @return the portName
	 */
	public String getPortName() {
		return PortName;
	}

	/**
	 * @param portName the portName to set
	 */
	void setPortName(String portName) {
		PortName = portName;
	}

	/**
	 * @return the commport
	 */
	public CommPort getCommport() {
		return commport;
	}

	/**
	 * @param commport the commport to set
	 */
	void setCommport(CommPort commport) {
		this.commport = commport;
	}


	/**
	 * @return the available
	 */
	public boolean isAvailable() {
		return Available;
	}


	/**
	 * @param available the available to set
	 */
	void setAvailable(boolean available) {
		Available = available;
	}


	/**
	 * @return the owner
	 */
	public String getOwner() {
		return Owner;
	}


	/**
	 * @param owner the owner to set
	 */
	void setOwner(String owner) {
		Owner = owner;
	}


	/**
	 * @return the next
	 */
	CommPortIdentifier getNext() {
		return next;
	}


	/**
	 * @param next the next to set
	 */
	void setNext(CommPortIdentifier next) {
		this.next = next;
	}
}

