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

//import java.io.FileDescriptor;
import gnu.io.CommDriver;
import gnu.io.CommPort;
import gnu.io.CommPortOwnershipListener;
import gnu.io.NoSuchPortException;
import gnu.io.PortLifecycleEvent;
import gnu.io.PortLifecycleListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.osgi.framework.BundleContext;

/**
* @author Trent Jarvi
* @version %I%, %G%
* @since JDK1.0
*/

public class CommPortIdentifiers
{
    public static enum PORT_TYPE
    {
    	SERIAL(PORT_SERIAL),
    	PARALLEL(PORT_PARALLEL);
    
    	private int portType;
    	
    	PORT_TYPE(int portType)
    	{
    		this.portType = portType;
    	}
    	
    	public static PORT_TYPE valueOf(int portType) 
    	{
    		PORT_TYPE[] portTypes = PORT_TYPE.values();
    		int index = 0;
    		int length = portTypes==null?0:portTypes.length;
    		
    		for(;index < length; index++)
    		{
    			if(portTypes[index].getPortType()==portType)
    			{
    				return portTypes[index];
    			}
    		}
    		return null;
    	}
    
    	public int getPortType() 
    	{
    		return this.portType;
    	}
    }

    public static final int PORT_RAW      = 5;  // Raw Port
    public static final int PORT_RS485    = 4;  // rs485 Port
    public static final int PORT_I2C      = 3;  // i2c Port
    public static final int PORT_PARALLEL = 2;  // Parallel Port
    public static final int PORT_SERIAL   = 1;  // rs232 Port
    
    private static BundleContext context;
    
	private static Map<CommPortIdentifiers.PORT_TYPE, CommPortIdentifiers> instances = 
			new EnumMap<CommPortIdentifiers.PORT_TYPE,CommPortIdentifiers> (
			        CommPortIdentifiers.PORT_TYPE.class);
	
	public static final void setBundleContext(BundleContext bundleContext)
	{
		if(CommPortIdentifiers.context == null)
		{
			CommPortIdentifiers.context = bundleContext;
			new Thread(new RXTXCommDriver.Observer()).start();
		}
	}
	
	public static final CommPortIdentifiers getInstance(int portType)
	{
		CommPortIdentifiers.PORT_TYPE type = 
				CommPortIdentifiers.PORT_TYPE.valueOf(portType);
		if(type == null)
		{
			return null;
		}
		CommPortIdentifiers instance = null;
		synchronized(instances)
		{
    		if((instance = instances.get(
    				CommPortIdentifiers.PORT_TYPE.valueOf(portType)))==null)
    		{
    			instance = new CommPortIdentifiers(
    					CommPortIdentifiers.context , portType);
    			instances.put(type, instance);
    		}
    		instance.open();
		}
		return instance;
	}

    /**
     * 
     */
    public static void closeInstances()
    {
        synchronized(instances)
        {
            Iterator<Map.Entry<CommPortIdentifiers.PORT_TYPE,CommPortIdentifiers>> iterator = 
                    instances.entrySet().iterator();
            while(iterator.hasNext())
            {
                Map.Entry<CommPortIdentifiers.PORT_TYPE,CommPortIdentifiers> 
                entry = iterator.next();
                entry.getValue().close();
            }
            instances.clear();
        }
        RXTXCommDriver.unobserve();
        CommPortIdentifiers.context = null;
        System.out.println("Stop RXTXCommDriver static thread");
    }
    
	private Object Sync;
 	private CommPortIdentifier CommPortIndex;
 	private CommDriver rxtxCommDriver;
 	private int portType;

    private LinkedList<PortLifecycleListener> listeners;
    
	private CommPortIdentifiers (BundleContext context, int portType) 
	{
		this.Sync = new Object();
		this.portType = portType;		
		this.rxtxCommDriver = new RXTXCommDriver(context, this);
	}
/*------------------------------------------------------------------------------
	addPortName()
	accept:         Name of the port s, Port type, 
                        reverence to rxtxCommDriver.
	perform:        place a new CommPortIdentifier in the linked list
	return: 	none.
	exceptions:     none.
	comments:
------------------------------------------------------------------------------*/
	protected void addPortName(String s) 
	{ 
		try
		{
			this.getPortIdentifier(s);
			
		}catch(NoSuchPortException e)
		{
//			if(((RXTXCommDriver)rxtxCommDriver).testExists(s))
//			{
				CommPortIdentifier commPortIdentifier = new CommPortIdentifier(
				        s, null, this.portType);
				
				synchronized (Sync) 
		        {
		            if (CommPortIndex == null) 
		            {
		                CommPortIndex = commPortIdentifier;
		            }
		            else
		            { 
		                CommPortIdentifier index  = CommPortIndex; 
		                while (index.getNext() != null)
		                {
		                    index = index.getNext();
		                }
		                index.setNext(commPortIdentifier);
		            } 
		        }				
				this.send(commPortIdentifier,PortLifecycleEvent.PORT_DISCOVERED);
//			}
		}
	}

/*------------------------------------------------------------------------------
	getPortIdentifier()
	accept:
	perform:
	return:
	exceptions:
	comments:   
------------------------------------------------------------------------------*/
	CommPortIdentifier getPortIdentifier(String s) 
			throws NoSuchPortException 
	{		
		CommPortIdentifier index;
		synchronized (Sync) 
		{
		 	index = CommPortIndex;
		 	
			while (index != null && !index.getPortName().equals(s)) 
			{ 
				index = index.getNext();
			}
		}
		if (index != null) 
		{
			return index;
		}
		else
		{
			throw new NoSuchPortException(s);
		}
	}
/*------------------------------------------------------------------------------
	getPortIdentifier()
	accept:
	perform:
	return:
	exceptions:
	comments:    
------------------------------------------------------------------------------*/
	CommPortIdentifier getPortIdentifier(CommPort p) 
		throws NoSuchPortException 	
	{ 
		CommPortIdentifier c;
		synchronized( Sync )
		{
			c = CommPortIndex;
			while ( c != null && c.getCommport() != p )
			{
				c = c.getNext();
			}
		}
		if ( c != null )
		{
			return (c);	
		}
		throw new NoSuchPortException();
	}
	
	/*------------------------------------------------------------------------------
	getPortIdentifiers()
	accept:
	perform:
	return:
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	@SuppressWarnings({"rawtypes" })
	public Enumeration getPortIdentifiers() 
	{ 
		List<String> ports = new ArrayList<String>();
		synchronized (Sync) 
		{
			CommPortIdentifier identifier = CommPortIndex;
			while(identifier != null)
			{
				ports.add(identifier.getPortName());
				identifier = identifier.getNext();
			} 
		}
		return Collections.enumeration(ports);
		
	}

    /**
     * Adds the {@link PortLifecycleListener} passed as parameter
     * to the list of listeners
     * 
     * @param listener
     *      the {@link PortLifecycleListener} to add
     */
    public void addPortLifecycleListener(PortLifecycleListener listener)
    {
        if(listeners == null)
        {
            listeners = new LinkedList<PortLifecycleListener>();
        }
        this.listeners.offer(listener);
    }
    
    /**
     * Removes the {@link PortLifecycleListener} passed as 
     * parameter from the list of listeners
     * 
     * @param listener
     *      the {@link PortLifecycleListener} to remove
     */
    public void removePortLifecycleListener(PortLifecycleListener listener)
    {
        if(listeners == null)
        {
            return;
        }
        this.listeners.remove(listener);
    }

    /**
     * Sends a {@link PortLifecycleEvent} which type is defined by
     * the eventType integer past as parameter to each registered 
     * {@link PortLifecycleListener} 
     * 
     * @param commPortIdentifier
     *      the {@link CommPortIdentifier} event's source object
     * @param eventType
     *      the type of the {@link PortLifecycleEvent} to send
     */
    protected void send(CommPortIdentifier commPortIdentifier,int eventType)
    {
        if(listeners == null)
        {
            return;
        }
        PortLifecycleEvent event = new PortLifecycleEvent(
                commPortIdentifier.getName(),eventType);
        
        ListIterator<PortLifecycleListener> iterator = this.listeners.listIterator();
        while(iterator.hasNext())
        {
            PortLifecycleListener listener = (PortLifecycleListener) iterator.next();
            listener.lifecycleEvent(event);
        }
    }
    
	public boolean checkValid(String name)
	{
		return (((RXTXCommDriver)rxtxCommDriver).testExists(
		name) && ((RXTXCommDriver)rxtxCommDriver).testRead(
				name,this.portType));
	}


    /**
     * @param name
     */
    public boolean remove(String name)
    {
        try
        {
            CommPortIdentifier commPortIdentifier = 
                    this.getPortIdentifier(name);
            
            return this.remove(commPortIdentifier);
            
        } catch (NoSuchPortException e)
        {
            return false;
        }
    }
    
/*------------------------------------------------------------------------------
	remove()
	accept:CommPortIdentifier
	perform:
	return: boolean
	exceptions:
	comments:
------------------------------------------------------------------------------*/
	protected boolean remove(CommPortIdentifier comm)
	{
		if(comm == null)
		{
			return false;
		}		
		CommPortIdentifier index;
		CommPortIdentifier previous;
		
		synchronized(Sync)
		{			
			index = CommPortIndex;
			if(comm == index)
			{   
				CommPortIndex = index.getNext();
				if(index.getCommport() != null)
				{
					index.getCommport().close();
				}
				this.send(index, PortLifecycleEvent.PORT_DISAPPEARED);				
				//index.internalClosePort();
				return true;
			}
			previous = index; 
			index = index.getNext();
			
			while(index!=null)
			{				
				if(index == comm)
				{
					previous.setNext(index.getNext());
					if(index.getCommport() != null)
					{
						index.getCommport().close();
					}
					this.send(index,PortLifecycleEvent.PORT_DISAPPEARED);
					//index.internalClosePort();
					index.setNext(null);
					return true;
				}
				previous = index;
				index = index.getNext();
			}
			return false;
		}
	}
//	/*------------------------------------------------------------------------------
//	open()
//	accept:
//	perform:
//	return:
//	exceptions:
//	comments:
//------------------------------------------------------------------------------*/
//	public synchronized CommPort open(FileDescriptor f) throws UnsupportedCommOperationException 
//	{ 
//		throw new UnsupportedCommOperationException();
//	}
/*------------------------------------------------------------------------------
	open()
	accept:      application making the call and milliseconds to block
                     during open.
	perform:     open the port if possible
	return:      CommPort if successful
	exceptions:  PortInUseException if in use.
	comments:
------------------------------------------------------------------------------*/
	@SuppressWarnings("unused")
	private boolean HideOwnerEvents;

	public CommPort open(String name, String TheOwner, int timeout) 
		throws gnu.io.PortInUseException, NoSuchPortException 
	{ 
		return this.open(name, TheOwner, timeout, false);
	}
	
/*------------------------------------------------------------------------------
	open()
	accept:      application making the call and milliseconds to block
                     during open.
	perform:     open the port if possible
	return:      CommPort if successful
	exceptions:  PortInUseException if in use.
	comments:
------------------------------------------------------------------------------*/
	public CommPort open(String name, String TheOwner, int timeout, boolean slip) 
			throws gnu.io.PortInUseException, gnu.io.NoSuchPortException
	{ 
		CommPortIdentifier commPortIdentifier = this.getPortIdentifier(name);

		boolean isAvailable;
		synchronized(Sync) 
		{
			isAvailable = commPortIdentifier.isAvailable();
			if (isAvailable) 
			{
			    //assume ownership inside the synchronized block
				commPortIdentifier.setAvailable(false);
				commPortIdentifier.setOwner(TheOwner);
			}
		}
		if (!isAvailable)
		{
			long waitTimeEnd = System.currentTimeMillis() + timeout;
			//fire the ownership event outside the synchronized block
			commPortIdentifier.fireOwnershipEvent(
					CommPortOwnershipListener.PORT_OWNERSHIP_REQUESTED);
			long waitTimeCurr;
			
			synchronized(this)
			{
				while(!commPortIdentifier.isAvailable() 
						&& (waitTimeCurr=System.currentTimeMillis()) < waitTimeEnd)
				{
					try
					{
						wait(waitTimeEnd - waitTimeCurr);
					}
					catch ( InterruptedException e )
					{
						Thread.currentThread().interrupt();
						break;
					}
				}
				isAvailable = commPortIdentifier.isAvailable();
				if (isAvailable) 
				{
					//assume ownership inside the synchronized block
					commPortIdentifier.setAvailable(false);
					commPortIdentifier.setOwner(TheOwner);
				}
			}
		}
		if (!isAvailable)
		{
			throw new gnu.io.PortInUseException(
					commPortIdentifier.getCurrentOwner());
		}
		//At this point, the CommPortIdentifier is owned by us.
		try 
		{
			System.out.println("COMMPORT : "+ commPortIdentifier.getCommport());
			if(commPortIdentifier.getCommport() == null)
			{
				commPortIdentifier.setCommport((RXTXPort) 
					((RXTXCommDriver)rxtxCommDriver).getCommPort(
							commPortIdentifier, slip));
			}
			if(commPortIdentifier.getCommport() != null)
			{
				commPortIdentifier.fireOwnershipEvent(
						CommPortOwnershipListener.PORT_OWNED);
				
				return commPortIdentifier.getCommport();
			}
			else
			{
				String message = commPortIdentifier.native_psmisc_report_owner(name);
				System.out.println("ERROR MESSAGE = "+ message);
				throw new gnu.io.PortInUseException(message);
			}
		} finally 
		{
			if(commPortIdentifier.getCommport() == null)
			{
				//something went wrong reserving the commport -> unown the port
				synchronized(this) 
				{
					commPortIdentifier.setAvailable(true);
					commPortIdentifier.setOwner(null);
				}
			}
		}
	}

	public int getPortType() 
	{
		return this.portType;
	}
	
	void close()
	{
		synchronized(Sync)
		{
			CommPortIdentifier commPortIdentifier = 
					this.CommPortIndex;
			
			while(commPortIdentifier != null)
			{
				if(commPortIdentifier.getCommport()!=null)
				{
					commPortIdentifier.getCommport().close();
				}
				//commPortIdentifier.internalClosePort();
				commPortIdentifier = commPortIdentifier.getNext();
			}
		}
		this.CommPortIndex = null;
		this.rxtxCommDriver.stop();
	}

	void open()
	{
		if(!this.rxtxCommDriver.started())
		{			
			this.rxtxCommDriver.start();
		}
	}
	
	public static void main(String args[])
	{
		CommPortIdentifiers identifiers = CommPortIdentifiers.getInstance(
				CommPortIdentifiers.PORT_SERIAL);
		
		try {
			Thread.sleep(300000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		identifiers.close();
	}
}

