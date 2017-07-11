#JARCH=amd64
JARCH=aarch32
#LJDK=/usr/java/jdk1.7.0_79
LJDK=/usr/share/ezdk-1.8.0_102

LLOCALINCLUDE=-I"include" -I"include/header"
LININCLUDE=$(LLOCALINCLUDE) -I$(LJDK)/include -I$(LJDK)/include/linux

LINOBJ=build/fixup.o build/fuserImp.o build/SerialImp.o

OSXJAVAINCLUDE1 = -I/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Headers 
OSXJAVAINCLUDE2 = -I/Developer/SDKs/MacOSX10.6.sdk/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers 
OSXJAVAINCLUDE4 = -I/Developer/SDKs/MacOSX10.6.sdk/usr/include 
OSXJAVAINCLUDE5 = -I./include/OSX/

OSXJAVAINCLUDE = $(OSXJAVAINCLUDE1) $(OSXJAVAINCLUDE2) $(OSXJAVAINCLUDE3) $(OSXJAVAINCLUDE4) $(OSXJAVAINCLUDE5)		
OSXINCLUDE=$(LLOCALINCLUDE) $(OSXJAVAINCLUDE)

CCOSX=cc $(OSXINCLUDE)  -g -O2 -D_BSD_SOURCE -c
LINKOSX=cc  -dynamiclib -framework JavaVM -framework IOKit -framework CoreFoundation

CCLIN32=gcc $(LININCLUDE) -O3 -Wall -c -fmessage-length=0 -fPIC -m32 -MMD
LINKLIN32=g++ -m32 -shared 

CCLIN64=gcc $(LININCLUDE) -O3 -Wall -c -fmessage-length=0 -fPIC -m64 -MMD
LINKLIN64=g++ -m64 -shared 

CCLINARM=arm-linux-gnueabi-gcc $(LININCLUDE) -O3 -Wall -c -fmessage-length=0 -fPIC -MMD -MMD -MP
LINKLINARM=arm-linux-gnueabi-g++ -shared

CCRASPI=gcc $(LININCLUDE) -O3 -Wall -c -fmessage-length=0 -fPIC -MMD -MMD -MP
LINKRASPI=gcc -fPIC -g -fno-omit-frame-pointer -DDEBUG  -shared

CCELIOBX=gcc $(LININCLUDE) -O3 -Wall -c -fmessage-length=0 -fPIC -MMD -MMD -MP
LINKELIOBX=gcc -fPIC -g -fno-omit-frame-pointer -DDEBUG  -shared

CCLINARM_HF=arm-linux-gnueabihf-gcc $(LININCLUDE) -O3 -Wall -c -fmessage-length=0 -fPIC -MMD -MMD -MP
LINKLINARM_HF=arm-linux-gnueabihf-g++ -shared

CCLINANDROID=/home/opt/android-ndk-r5b/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/arm-linux-androideabi/bin/gcc $(LININCLUDE) -O3 -Wall -c -fmessage-length=0 -fPIC -MMD -MMD -MP
LINKLINANDROID=/home/opt/android-ndk-r5b/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/arm-linux-androideabi/bin/g++ -shared

CCLINPPC=/usr/local/powerpc-none-linux-gnuspe/bin/powerpc-none-linux-gnuspe-gcc $(LININCLUDE) -O3 -Wall -c -fmessage-length=0 -fPIC -MMD -MMD -MP
LINKLINPPC=/usr/local/powerpc-none-linux-gnuspe/bin/powerpc-none-linux-gnuspe-g++ -shared

WINLINKOPT = -shared -Wl,--add-stdcall-alias -DBUILD_DLL -pthread
WINCCOPT = -O3 -Wall -DBUILD_DLL
JDKDIR = C:\glassfish3\jdk\include

WININCLUDE32=-I".\include" -I".\include\header" -I".\include\windows" -I"C:\MinGW\include" -I"$(JDKDIR)" -I"$(JDKDIR)\win32"
CCWIN32 = wine "C:\MinGW\bin\gcc.exe" $(WININCLUDE32) -m32 $(WINCCOPT)
LINKWIN32 =wine "C:\MinGW\bin\gcc.exe" $(WINLINKOPT)

WININCLUDE64=-I".\include" -I".\include\header" -I".\include\windows" -I"C:\MinGW64\include" -I"$(JDKDIR)" -I"$(JDKDIR)\win32"
CCWIN64 = wine "C:\MinGW64\bin\gcc.exe" $(WININCLUDE64) -m64 $(WINCCOPT)
LINKWIN64 =wine "C:\MinGW64\bin\gcc.exe" $(WINLINKOPT)

MINGW32 = C:\\Users\\cm239742\\_Bin\\MinGW\\msys\\1.0
MINGW64 = C:\\Users\\cm239742\\_Bin\\MinGW\\mingw64\\mingw

WININCLUDE32N=-I".\\include" -I".\\include\\header" -I".\\include\\windows" -I"$(MINGW32)\\include" -I"$(JDKDIR)" -I"$(JDKDIR)\win32"
CCWIN32N = "$(MINGW32)\\bin\\gcc.exe" $(WININCLUDE32N) -m32 $(WINCCOPT)
LINKWIN32N = "$(MINGW32)\\bin\\gcc.exe" $(WINLINKOPT)

WININCLUDE64N=-I".\\include" -I".\\include\\header" -I".\\include\\windows" -I"$(MINGW64)\\include" -I"$(JDKDIR)" -I"$(JDKDIR)\win32"
CCWIN64N = "$(MINGW64)\\bin\\gcc.exe" $(WININCLUDE64N) -m64 $(WINCCOPT)
LINKWIN64N = "$(MINGW64)\\bin\\gcc.exe" $(WINLINKOPT)

all:
	echo "Specify a system: make linux, make windows, make osx"
dirs:
	if (test -d build); then rm -rf build;mkdir build;else mkdir build; fi
	if (test -d resources/native); then echo 'resources/native exists';else mkdir resources/native; fi
	if (test -d resources/native/linux);then echo 'resources/native/linux exists';else mkdir resources/native/linux; fi
	if (test -d resources/native/windows);then echo 'resources/native/windows exists';else mkdir resources/native/windows; fi
	if (test -d resources/native/linux/x86_32);then echo 'resources/native/linux/x86_32 exists';else mkdir -p resources/native/linux/x86_32; fi	 
	if (test -d resources/native/linux/x86_64); then echo 'resources/native/linux/x86_64 exists';else mkdir -p resources/native/linux/x86_64; fi	 
	if (test -d resources/native/linux/arm); then echo 'resources/native/linux/arm exists';else mkdir -p resources/native/linux/arm; fi
	if (test -d resources/native/linux/armv6); then echo 'resources/native/linux/armv6 exists';else mkdir -p resources/native/linux/armv6; fi
	if (test -d resources/native/linux/armv6/hf);then echo 'resources/native/linux/armv6/hf exists';else mkdir -p resources/native/linux/armv6/hf; fi
	if (test -d resources/native/linux/armv7/hf);then echo 'resources/native/linux/armv7/hf exists';else mkdir -p resources/native/linux/armv7/hf; fi
	if (test -d resources/native/linux/raspberry);then echo 'resources/native/linux/raspberry exists';else mkdir -p resources/native/linux/raspberry; fi
	if (test -d resources/native/linux/aarch32);then echo 'resources/native/linux/aarch32 exists';else mkdir -p resources/native/linux/aarch32; fi
	if (test -d resources/native/linux/PPC);then echo 'resources/native/linux/PPC exists';else mkdir -p resources/native/linux/PPC; fi
	if (test -d resources/native/windows/x86_32); then echo 'resources/native/windows/x86_32 exists';else mkdir -p resources/native/windows/x86_32; fi
	if (test -d resources/native/windows/x86_64); then echo 'resources/native/windows/x86_64 exists';else mkdir -p resources/native/windows/x86_64; fi
	if (test -d resources/native/osx); then echo 'resources/native/osx exists';else mkdir -p resources/native/osx;  fi
	 
windowsLocal:dirs
	$(CCWIN64N) -c src/windows/init.c -o build/init.o
	$(CCWIN64N) -c src/windows/termios.c -o build/termios.o
	$(CCWIN64N) -c src/fixup.c -o build/fixup.o
	$(CCWIN64N) -c src/fuserImp.c -o build/fuserImp.o
	$(CCWIN64N) -c src/SerialImp.c -o build/SerialImp.o
	$(LINKWIN64N) -o"resources/native/windows/x86_64/libNRJavaSerial.dll" build/SerialImp.o build/fuserImp.o build/fixup.o build/termios.o build/init.o
	
	#$(CCWIN32N) -c src/windows/init.c -o build/init.o
	#$(CCWIN32N) -c src/windows/termios.c -o build/termios.o
	#$(CCWIN32N) -c src/fixup.c -o build/fixup.o
	#$(CCWIN32N) -c src/fuserImp.c -o build/fuserImp.o
	#$(CCWIN32N) -c src/SerialImp.c -o build/SerialImp.o
	#$(LINKWIN32N) -o"resources/native/windows/x86_32/libNRJavaSerial.dll" build/SerialImp.o build/fuserImp.o build/fixup.o build/termios.o build/init.o
	rm build/*
	
windows:dirs
	export WINEPREFIX=/home/hephaestus/.wine/
	$(CCWIN32) src/windows/init.c -o build/init.o
	$(CCWIN32) src/windows/termios.c -o build/termios.o
	$(CCWIN32) src/fixup.c -o build/fixup.o
	$(CCWIN32) src/fuserImp.c -o build/fuserImp.o
	$(CCWIN32) src/SerialImp.c -o build/SerialImp.o
	$(LINKWIN32) -o"resources/native/windows/x86_32/libNRJavaSerial.dll" build/SerialImp.o build/fuserImp.o build/fixup.o build/termios.o build/init.o
	rm build/*
	
	#$(CCWIN64) src/windows/init.c -o build/init.o
	#$(CCWIN64) src/windows/termios.c -o build/termios.o
	#$(CCWIN64) src/fixup.c -o build/fixup.o
	#$(CCWIN64) src/fuserImp.c -o build/fuserImp.o
	#$(CCWIN64) src/SerialImp.c -o build/SerialImp.o
	#$(LINKWIN64) -o"resources/native/windows/x86_64/libNRJavaSerial.dll" build/SerialImp.o build/fuserImp.o build/fixup.o build/termios.o build/init.o
	rm -rf build

linux:linux32 linux64 arm armHF ppc
	echo all Linux ok!

linux32:dirs
	$(CCLIN32) src/fixup.c -o build/fixup.o
	$(CCLIN32) src/fuserImp.c -o build/fuserImp.o
	$(CCLIN32) src/SerialImp.c -o build/SerialImp.o
	$(LINKLIN32) -o"resources/native/linux/x86_32/libNRJavaSerial.so" $(LINOBJ)
	rm -rf build

linux64:dirs	
	$(CCLIN64) src/fixup.c -o build/fixup.o
	$(CCLIN64) src/fuserImp.c -o build/fuserImp.o
	$(CCLIN64) src/SerialImp.c -o build/SerialImp.o
	$(LINKLIN64) -o"resources/native/linux/x86_64/libNRJavaSerial.so" $(LINOBJ)
	rm -rf build

arm7:	dirs
	$(CCLINARM) --with-arch=armv7  src/fixup.c -o build/fixup.o
	$(CCLINARM) --with-arch=armv7  src/fuserImp.c -o build/fuserImp.o
	$(CCLINARM) --with-arch=armv7  src/SerialImp.c -o build/SerialImp.o
	$(LINKLINARM) --with-arch=armv7  -o"resources/native/linux/armv7/libNRJavaSerial.so" $(LINOBJ)
	rm -rf build

arm7HF:dirs
	$(CCLINARM_HF) --with-arch=armv7  src/fixup.c -o build/fixup.o
	$(CCLINARM_HF) --with-arch=armv7  src/fuserImp.c -o build/fuserImp.o
	$(CCLINARM_HF) --with-arch=armv7  src/SerialImp.c -o build/SerialImp.o
	$(LINKLINARM_HF) --with-arch=armv7 -o"resources/native/linux/armv7/hf/libNRJavaSerial.so" $(LINOBJ)
	rm -rf build	
	
arm6:	dirs
	$(CCLINARM) --with-arch=armv6  src/fixup.c -o build/fixup.o
	$(CCLINARM) --with-arch=armv6  src/fuserImp.c -o build/fuserImp.o
	$(CCLINARM) --with-arch=armv6  src/SerialImp.c -o build/SerialImp.o
	$(LINKLINARM) --with-arch=armv6  -o"resources/native/linux/armv6/libNRJavaSerial.so" $(LINOBJ)
	rm -rf build

arm6HF:dirs
	$(CCLINARM_HF) --with-arch=armv6  src/fixup.c -o build/fixup.o
	$(CCLINARM_HF) --with-arch=armv6 src/fuserImp.c -o build/fuserImp.o
	$(CCLINARM_HF) --with-arch=armv6 src/SerialImp.c -o build/SerialImp.o
	$(LINKLINARM_HF) --with-arch=armv6 -o"resources/native/linux/armv6/hf/libNRJavaSerial.so" $(LINOBJ)
	rm -rf build

raspberry:dirs
	$(CCRASPI)  src/fixup.c -o build/fixup.o
	$(CCRASPI)  src/fuserImp.c -o build/fuserImp.o
	$(CCRASPI)  src/SerialImp.c -o build/SerialImp.o
	$(LINKRASPI)  -o"resources/native/linux/raspberry/libNRJavaSerial.so" $(LINOBJ)
	rm -rf build	

aarch32:dirs
	$(CCELIOBX)  src/fixup.c -o build/fixup.o
	$(CCELIOBX)  src/fuserImp.c -o build/fuserImp.o
	$(CCELIOBX)  src/SerialImp.c -o build/SerialImp.o
	$(LINKELIOBX)  -o"resources/native/linux/aarch32/libNRJavaSerial.so" $(LINOBJ)
	rm -rf build	

ppc:   dirs
	$(CCLINPPC) src/fixup.c -o build/fixup.o
	$(CCLINPPC) src/fuserImp.c -o build/fuserImp.o
	$(CCLINPPC) src/SerialImp.c -o build/SerialImp.o
	$(LINKLINPPC) -o"resources/native/linux/PPC/libNRJavaSerial.so" $(LINOBJ)
	rm -rf build
	
osx:dirs
	if (test -d include/OSX/CoreFoundation/);\
	then\
		echo Dirs OK!;\
	else\
		rm include/OSX/CoreFoundation;\
		sudo ln -s /Developer/SDKs/MacOSX10.6.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/ include/OSX/CoreFoundation; \
	fi
	
	if (test -d include/OSX/CoreServices/);\
	then\
		echo Dirs OK!;\
	else\
		rm include/OSX/CoreServices;\
		sudo ln -s /Developer/SDKs/MacOSX10.6.sdk/System/Library/Frameworks/CoreServices.framework/Headers/ include/OSX/CoreServices; \
	fi
	
	if (test -d include/OSX/CarbonCore/);\
	then\
		echo Dirs OK!;\
	else\
		rm include/OSX/CarbonCore;\
		sudo ln -s /Developer/SDKs/MacOSX10.6.sdk/System/Library/Frameworks/CoreServices.framework/Versions/A/Frameworks/CarbonCore.framework/Versions/A/Headers include/OSX/CarbonCore; \
	fi
	
	if (test -d include/OSX/IOKit/);\
	then\
		echo Dirs OK!;\
	else\
		rm include/OSX/CarbonCore;\
		sudo ln -s /Developer/SDKs/MacOSX10.6.sdk/System/Library/Frameworks/IOKit.framework/Headers include/OSX/IOKit; \
	fi
	
	$(CCOSX) src/fuserImp.c -o build/fuserImp.o
	$(CCOSX) src/SerialImp.c -o build/SerialImp.o
	$(LINKOSX) -o "./resources/native/osx/libNRJavaSerial.jnilib" build/SerialImp.o build/fuserImp.o
	rm -rf build	
	
