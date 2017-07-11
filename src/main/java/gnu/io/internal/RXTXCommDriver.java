/*-------------------------------------------------------------------------
|   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
|   RXTX is a native interface to serial ports in java.
|   Copyright 1998 Kevin Hester, kevinh@acm.org
|   Copyright 2000-2008 Trent Jarvi tjarvi@qbang.org and others who
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
/* Martin Pool <mbp@linuxcare.com> added support for explicitly-specified
 * lists of ports, October 2000. */
/* Joseph Goldstone <joseph@lp.com> reorganized to support registered ports,
 * known ports, and scanned ports, July 2001 */

package gnu.io.internal;

import gnu.io.CommDriver;
import gnu.io.CommPort;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.osgi.framework.BundleContext;

/**
   This is the JavaComm for Linux driver.
*/
public class RXTXCommDriver implements CommDriver
{
	private final static boolean debug = false;
	private final static boolean devel = false;
	
	private final static boolean noVersionOutput = "true".equals( 
			System.getProperty("gnu.io.rxtx.NoVersionOutput" ));

	// for rxtx prior to 2.1.7
	private static native String nativeGetVersion();
    private native static void observe();    
    protected native static void unobserve();    
	
	static
	{	
		try 
		{
		    System.loadLibrary( "NRJavaSerial" );
		
		} catch ( Exception e )
		{
			e.printStackTrace();
			
		} catch ( Error e )
		{
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 * 
	 * @throws UnsatisfiedLinkError
	 */
	public static String nativeGetVersionWrapper() 
			throws UnsatisfiedLinkError 
	{
		return nativeGetVersion();
	}

	/**
	 * Called by native code when the observation thread is stopped
	 */
	public static void observationThreadStopped()
	{
		RXTXCommDriver.observing = false;
	}
	
	/**
	 * Underlying native observation thread start point 
	 */
	static class Observer implements Runnable
	{
		@Override
		public void run() 
		{
			RXTXCommDriver.observing = true;
			RXTXCommDriver.observe();
            System.out.println("Static observer is stopped");
		}
	}	
	
	/**
	 * Returns true if the native observation thread 
	 * is running; otherwise returns false
	 * 
	 * @return
	 * 		<ul>
	 * 			<li>true if the underlying native observation 
	 * 				thread is running;
	 *			</li>
	 * 			<li>false othewise</li>
	 * 		</ul>
	 */
	public static boolean isObserving()
	{
		return RXTXCommDriver.observing;
	}
	
	private static boolean observing;
	
	private native boolean isPortPrefixValid(String dev);
	private native String getDeviceDirectory();
	private native String[] registerKnownPorts(int PortType);

    private native void registerObserver();
    private native void unregisterObserver();	
    
	protected native boolean testRead(String dev, int type);
	protected native boolean testExists(String dev);
	
	
	/** Get the Serial port prefixes for the running OS */
	private String deviceDirectory;
	private String osName;

	private String[] serialPrefixes;	
	private String[] parallelPrefixes;
	private String[] candidates;
	
	private boolean started;
	private final CommPortIdentifiers commPortIdentifiers;

	public RXTXCommDriver(BundleContext context, CommPortIdentifiers commPortIdentifiers)
	{
		this.commPortIdentifiers = commPortIdentifiers;
		this.osName = System.getProperty("os.name");
		this.deviceDirectory = getDeviceDirectory();
		
		if(context != null)
		{
			String udevProp = context.getProperty("udev.enabled");
			
			boolean udev = udevProp==null?false:Boolean.parseBoolean(udevProp);
			if(udev)
			{ 
			    String udevList = context.getProperty("udev.list");
				System.setProperty("gnu.io.SerialPorts", udevList);
			}
		}
		this.candidates = this.systemDefinedCandidates();
		
		if(this.candidates == null || this.candidates.length == 0)
		{
			this.candidates = this.alreadyKnownCandidates(
					this.commPortIdentifiers.getPortType());	
			
			if(this.candidates == null || this.candidates.length == 0)
			{
				if(osName.toLowerCase().indexOf("windows") != -1 )
				{
					if(osName.equals("Windows CE"))
					{
						this.candidates = new String[]{ 
								"COM1:".intern(), 
								"COM2:".intern(),
								"COM3:".intern(),
								"COM4:".intern(),
								"COM5:".intern(), 
								"COM6:".intern(), 
								"COM7:".intern(), 
								"COM8:".intern() 
								};
					} else
					{
						this.candidates = new String[259];
						for( int i = 0; i < 256; i++ )
						{
							this.candidates[i - 1] = ("COM" + i).intern();
						}
						for( int i = 1; i <= 3; i++ )
						{
							this.candidates[i + 255] = ("LPT" + i).intern();
						}
					}					
				} else  if( osName.equals("Solaris") || osName.equals("SunOS"))
				{
					String cua ="cua";
					String term ="term";
					
					this.candidates = new String[((123-97)+(58-48))*2];
					int index = 0;
				
					/** handle solaris/sunOs /dev/cua/a convention */
					char d[] =  { 91 };
					for( d[0] = 97 ;d[0] < 123; d[0]++ )
					{		
						this.candidates[index++] = (cua.concat(new String(d))).intern();
						this.candidates[index++] = (term.concat(new String(d))).intern();
					}
					/** check for 0-9 in case we have them (Solaris USB) */
					for( d[0] = 48 ;d[0] <= 57; d[0]++ )
					{
						this.candidates[index++] = (cua.concat(new String(d))).intern();
						this.candidates[index++] = (term.concat(new String(d))).intern();
					}					
				} else
				{
					this.portPrefixes();
				}
			}
		}
	}

    
	/**
	*  @param commPortIdentifier the CommPortIdentifier associated to the port
	*  @param PortType CommPortIdentifier.PORT_SERIAL or PORT_PARALLEL
	*  @param slip 	is the port configured to use serial line IP protocol ?
	*  @return CommPort
	*  getCommPort() will be called by CommPortIdentifier from its
	*  openPort() method. PortName is a string that was registered earlier
	*  using the CommPortIdentifier.addPortName() method. getCommPort()
	*  returns an object that extends either SerialPort or ParallelPort.
	*/
	public CommPort getCommPort(CommPortIdentifier commPortIdentifier, boolean slip )
	{
		try 
		{
			switch (this.commPortIdentifiers.getPortType()) 
			{
				case CommPortIdentifiers.PORT_SERIAL:					
					return new RXTXPort( commPortIdentifier , slip );					
				default:
					System.out.println("unknown PortType  "+ 
							this.commPortIdentifiers.getPortType() 
								+" passed to RXTXCommDriver.getCommPort()");
					
			}
		} catch( PortInUseException e )
		{
			System.out.println("Port " + commPortIdentifier.getName() + 
					" in use by another application");
			
		} catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	*  @param commPortIdentifier the CommPortIdentifier associated to the port
	*  @param PortType CommPortIdentifier.PORT_SERIAL or PORT_PARALLEL
	*  @param slip 	is the port configured to use serial line IP protocol ?
	*  @return CommPort
	*  getCommPort() will be called by CommPortIdentifier from its
	*  openPort() method. PortName is a string that was registered earlier
	*  using the CommPortIdentifier.addPortName() method. getCommPort()
	*  returns an object that extends either SerialPort or ParallelPort.
	*/
	@Override
	public CommPort getCommPort(String portName, boolean slip)
	{
		try 
		{
			return this.getCommPort(
				this.commPortIdentifiers.getPortIdentifier(
					portName), slip);
			
		} catch (NoSuchPortException e) 
		{
			e.printStackTrace();
		}
		return null;
	}

	public void Report( String arg )
	{
		System.out.println(arg);
	}	
    
    /**
     * @param ports
     * @return
     */
    private List<String> checkObservedPrefixed(String[] ports)
    {    	
    	String[] observed = null;
    	switch(this.commPortIdentifiers.getPortType())
    	{
	    	case CommPortIdentifiers.PORT_SERIAL:
	    		observed = this.serialPrefixes;
	    		break;
	    	case CommPortIdentifiers.PORT_PARALLEL:
	    		observed = this.parallelPrefixes;
	    		break;
	    	default:;
    	}
    	if(observed == null)
    	{
            observed = this.candidates;
    	}
    	int index = 0;
    	int length = ports==null?0:ports.length;
    	
		int observedIndex = 0;
		int observedLength = observed==null?0:observed.length;
		
		List<String> candidates = new ArrayList<String>();
		
		for(;index<length;index++) 
		{
			char[] candidateValue = ports[index].toCharArray();
			int candidateCount = candidateValue.length;

	        observedIndex = 0;
	        observedLength = observed==null?0:observed.length;
	        
			for(;observedIndex< observedLength; observedIndex++ ) 
			{
				char[] prefixValue = observed[observedIndex].toCharArray();
				int prefixCount = prefixValue.length;				
				if(candidateCount < prefixCount)
				{
					continue;
				}				
				int k = 0;
				for(;k < prefixCount && candidateValue[k]==prefixValue[k];k++);				
				if(k < prefixCount)
				{
					continue;
				}
				for(;k < candidateCount && Character.toUpperCase(
						candidateValue[k])==Character.toLowerCase(
								candidateValue[k]);k++);
				if(k < candidateCount)
				{
					continue;
				}
				String PortName;
				if(osName.toLowerCase().indexOf("windows") == -1 )
				{
					PortName = (deviceDirectory + ports[index]);
				}
				else
				{
					PortName =  ports[index];
				}
				candidates.add(PortName);
				break;
			}
		}
		return candidates;
    }    
    
    /**
	 * Defines serial and parallel ports known
	 * prefixes according to the operating 
	 * system
	 */
	private void portPrefixes()
	{
		if(osName.equals("Linux"))
		{
			serialPrefixes = new String[]{
			"ttyS".intern(), // linux Serial Ports
			"ttySA".intern(), // for the IPAQs
			"ttyUSB".intern(), // for USB frobs
			"rfcomm".intern(),       // bluetooth serial device
			"ttyircomm".intern(), // linux IrCommdevices (IrDA serial emu)
			"ttyACM".intern(),// linux CDC ACM devices
			"DyIO".intern(),// NRDyIO
			"Bootloader".intern(),// NRDyIO bootloader
			"BowlerDevice".intern(),// Generic Bowler Device
			"DeltaDoodle".intern(),// DeltaDoodle Printer
			"dyio".intern()// linux CDC ACM devices
			};

			// linux printer port
			parallelPrefixes = new String[]{ "lp".intern() };
		}
		else if(osName.equals("Linux-all-ports"))
		{
			/* if you want to enumerate all ports ~5000
		   		possible, then replace the above with this
			*/
			serialPrefixes = new String[]
			{
			"comx".intern(),      // linux COMMX synchronous serial card
			"holter".intern(),    // custom card for heart monitoring
			"modem".intern(),     // linux symbolic link to modem.
			"rfcomm".intern(),       // bluetooth serial device
			"ttyircomm".intern(), // linux IrCommdevices (IrDA serial emu)
			"ttycosa0c".intern(), // linux COSA/SRP synchronous serial card
			"ttycosa1c".intern(), // linux COSA/SRP synchronous serial card
			"ttyACM".intern(),// linux CDC ACM devices
			"DyIO".intern(),// linux CDC ACM devices
			"Bootloader".intern(),// linux CDC ACM devices
			"BowlerDevice".intern(),// Generic Bowler Device
			"DeltaDoodle".intern(),// DeltaDoodle Printer
			"dyio".intern(),// linux CDC ACM devices
			"ttyC".intern(), // linux cyclades cards
			"ttyCH".intern(),// linux Chase Research AT/PCI-Fast serial card
			"ttyD".intern(), // linux Digiboard serial card
			"ttyE".intern(), // linux Stallion serial card
			"ttyF".intern(), // linux Computone IntelliPort serial card
			"ttyH".intern(), // linux Chase serial card
			"ttyI".intern(), // linux virtual modems
			"ttyL".intern(), // linux SDL RISCom serial card
			"ttyM".intern(), // linux PAM Software's multimodem boards
				// linux ISI serial card
			"ttyMX".intern(),// linux Moxa Smart IO cards
			"ttyP".intern(), // linux Hayes ESP serial card
			"ttyR".intern(), // linux comtrol cards
				// linux Specialix RIO serial card
			"ttyS".intern(), // linux Serial Ports
			"ttySI".intern(),// linux SmartIO serial card
			"ttySR".intern(),// linux Specialix RIO serial card 257+
			"ttyT".intern(), // linux Technology Concepts serial card
			"ttyUSB".intern(),//linux USB serial converters
			"ttyV".intern(), // linux Comtrol VS-1000 serial controller
			"ttyW".intern(), // linux specialix cards
			"ttyX".intern()  // linux SpecialX serial card
			};
			// linux printer port
			parallelPrefixes = new String[]{ 
					"lp".intern() 
			};
		}
		else if(osName.toLowerCase().indexOf("qnx") != -1 )
		{
			serialPrefixes = new String[] {
				"ser".intern()
			};
			parallelPrefixes= new String[]{
				"par".intern()
			};			
		}
		else if(osName.equals("Irix"))
		{
			serialPrefixes = new String[]{
				"ttyc".intern(), // irix raw character devices
				"ttyd".intern(), // irix basic serial ports
				"ttyf".intern(), // irix serial ports with hardware flow
				"ttym".intern(), // irix modems
				"ttyq".intern(), // irix pseudo ttys
				"tty4d".intern(),// irix RS422
				"tty4f".intern(),// irix RS422 with HSKo/HSki
				"midi".intern(), // irix serial midi
				"us".intern()    // irix mapped interface
			};
			parallelPrefixes= new String[]{
					"lp".intern()
			};						
		}
		else if(osName.equals("FreeBSD")) //FIXME this is probably wrong
		{
			serialPrefixes = new String[]
			{
				"ttyd".intern(),    //general purpose serial ports
				"cuaa".intern(),    //dialout serial ports
				"ttyA".intern(),    //Specialix SI/XIO dialin ports
				"cuaA".intern(),    //Specialix SI/XIO dialout ports
				"ttyD".intern(),    //Digiboard - 16 dialin ports
				"cuaD".intern(),    //Digiboard - 16 dialout ports
				"ttyE".intern(),    //Stallion EasyIO (stl) dialin ports
				"cuaE".intern(),    //Stallion EasyIO (stl) dialout ports
				"ttyF".intern(),    //Stallion Brumby (stli) dialin ports
				"cuaF".intern(),    //Stallion Brumby (stli) dialout ports
				"ttyR".intern(),    //Rocketport dialin ports
				"cuaR".intern(),    //Rocketport dialout ports
				"stl".intern()      //Stallion EasyIO board or Brumby N 
			};
			parallelPrefixes = new String[]{"lpt".intern()};
		}
		else if(osName.equals("NetBSD")) // FIXME this is probably wrong
		{
			serialPrefixes = new String[]{
				"tty0".intern()  // netbsd serial ports
			};
			parallelPrefixes= new String[]{
					"lpt".intern()
			};		
		}
		else if(osName.equals("HP-UX"))
		{
			serialPrefixes = new String[] {
				"tty0p".intern(),// HP-UX serial ports
				"tty1p".intern() // HP-UX serial ports
			};
			parallelPrefixes= new String[]{};		
		}
		else if(osName.equals("UnixWare") || osName.equals("OpenUNIX"))
		{
			serialPrefixes = new String[]{
				"tty00s".intern(), // UW7/OU8 serial ports
				"tty01s".intern(),
				"tty02s".intern(),
				"tty03s".intern()
			};
			parallelPrefixes= new String[]{};		
		}
		else if	(osName.equals("OpenServer"))
		{
			serialPrefixes = new String[]{
				"tty1A".intern(),  // OSR5 serial ports
				"tty2A".intern(),
				"tty3A".intern(),
				"tty4A".intern(),
				"tty5A".intern(),
				"tty6A".intern(),
				"tty7A".intern(),
				"tty8A".intern(),
				"tty9A".intern(),
				"tty10A".intern(),
				"tty11A".intern(),
				"tty12A".intern(),
				"tty13A".intern(),
				"tty14A".intern(),
				"tty15A".intern(),
				"tty16A".intern(),
				"ttyu1A".intern(), // OSR5 USB-serial ports
				"ttyu2A".intern(),
				"ttyu3A".intern(),
				"ttyu4A".intern(),
				"ttyu5A".intern(),
				"ttyu6A".intern(),
				"ttyu7A".intern(),
				"ttyu8A".intern(),
				"ttyu9A".intern(),
				"ttyu10A".intern(),
				"ttyu11A".intern(),
				"ttyu12A".intern(),
				"ttyu13A".intern(),
				"ttyu14A".intern(),
				"ttyu15A".intern(),
				"ttyu16A".intern()
			};
			parallelPrefixes = new String[]{ "lp".intern() };	
		}
		else if (osName.equals("Compaq's Digital UNIX") || osName.equals("OSF1"))
		{
			serialPrefixes = new String[]{
				"tty0".intern()  //  Digital Unix serial ports
			};
			parallelPrefixes= new String[]{};		
		}
		else if(osName.equals("BeOS"))
		{
			serialPrefixes = new String[] {
				"serial".intern() // BeOS serial ports
			};	
			parallelPrefixes= new String[]{};					
		}
		else if(osName.equals("Mac OS X"))
		{
			serialPrefixes = new String[] {
			// Keyspan USA-28X adapter, USB port 1
				"cu.KeyUSA28X191.".intern(),
			// Keyspan USA-28X adapter, USB port 1
				"tty.KeyUSA28X191.".intern(),
			// Keyspan USA-28X adapter, USB port 2
				"cu.KeyUSA28X181.".intern(),
			// Keyspan USA-28X adapter, USB port 2
				"tty.KeyUSA28X181.".intern(),
			// Keyspan USA-19 adapter
				"cu.KeyUSA19181.".intern(),
			// Keyspan USA-19 adapter
				"tty.KeyUSA19181.".intern()
			};
			parallelPrefixes= new String[]{};
		}
	}
	
	/**
	 * Simply returns the Strings array returned by the 
	 * registerKnownPorts native method
	 * 
	 * @param portType
	 * 		the int port type for which retrieve already known
	 * 		ports
	 * 
	 * @return
	 * 		the Strings array returned by the 
	 * 		registerKnownPorts native method
	 */
	private String[] alreadyKnownCandidates(int portType)
	{
		return this.registerKnownPorts(portType);
	}
	
   /**
    * Register ports specified in the file "gnu.io.rxtx.properties"
    * Key system properties:
    *                   gnu.io.rxtx.SerialPorts
    * 			gnu.io.rxtx.ParallelPorts
    *
    * Tested only with sun jdk1.3
    * The file gnu.io.rxtx.properties must reside in the java extension dir
    *
    * Example: /usr/local/java/jre/lib/ext/gnu.io.rxtx.properties
    *
    * The file contains the following key properties:
    *
    *  gnu.io.rxtx.SerialPorts=/dev/ttyS0:/dev/ttyS1:
    *  gnu.io.rxtx.ParallelPorts=/dev/lp0:
    *
    */
	private String[] systemDefinedCandidates()
	{
		Properties origp = System.getProperties();//save system properties
        String fileSep = System.getProperty("file.separator");
        String pathSep = System.getProperty("path.separator", ":");		
	    String ext_dirs = System.getProperty("java.ext.dirs");
	    
	    StringTokenizer tok = new StringTokenizer(ext_dirs, pathSep);

        while (tok.hasMoreElements())
        {
              String ext_dir = tok.nextToken();
              try
              {
                  FileInputStream rxtx_prop = new FileInputStream(
                     new StringBuilder().append(ext_dir).append(fileSep
                             ).append("gnu.io.rxtx.properties"
                                     ).toString());
              
                  Properties properties=new Properties();
                  properties.load(rxtx_prop);
              
                  for (Iterator it = properties.keySet().iterator(); it.hasNext();) 
                  {
                       String key = (String) it.next();
                       System.setProperty(key, properties.getProperty(key));
                  } 
              } catch(Exception e)
              {
                  System.out.println("The file: gnu.io.rxtx.properties doesn't exists.");
                  System.out.println(e.toString());
              }
        }
		System.setProperties(origp); 
		String prop = null;
		
    	switch(this.commPortIdentifiers.getPortType())
    	{
	    	case CommPortIdentifiers.PORT_SERIAL:
	    		prop = System.getProperty("gnu.io.SerialPorts");
	    		break;
	    	case CommPortIdentifiers.PORT_PARALLEL:
	    		prop = System.getProperty("gnu.io.rxtx.SerialPorts");
	    		break;
	    	default:;
    	}
		if (prop != null) 
		{
			tok = new StringTokenizer(prop, pathSep);
			String[] candidates = new String[tok.countTokens()];

			int index = 0;
			while (tok.hasMoreElements())
			{
				String PortName = tok.nextToken();
				candidates[index++]  = PortName;
			}
			System.out.println(Arrays.toString(candidates));
			return candidates;
		}
		return new String[0];
	}
    
    public void appearance(String[] appearing)
    {       
        List<String> validPorts = this.checkObservedPrefixed(
                appearing);
        //System.out.println("APPEARANCE : " + validPorts);
        while(!validPorts.isEmpty())
        {
            String name = validPorts.remove(0);
            this.commPortIdentifiers.addPortName(name);
        }
    }
    
    public void disappearance(String[] disappearing)
    {
        List<String> validPorts = this.checkObservedPrefixed(
                disappearing);
        //System.out.println("DISAPPEARANCE : " + validPorts);
        while(!validPorts.isEmpty())
        {           
            this.commPortIdentifiers.remove(validPorts.remove(0));
        }
    }
    
    /** 
     * @InheritDoc
     *
     * @see gnu.io.CommDriver#start()
     */
    public void start()
    {
        this.started = true;
        this.registerObserver();
    }
    
	/** 
	 * @InheritDoc
	 *
	 * @see gnu.io.CommDriver#stop()
	 */
	@Override
	public void stop()
	{
        this.unregisterObserver();
	    this.started = false;
	}

    /** 
     * @InheritDoc
     *
     * @see gnu.io.CommDriver#started()
     */
    @Override
    public boolean started()
    {
        return this.started;
    }
}
