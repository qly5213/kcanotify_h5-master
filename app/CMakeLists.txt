cmake_minimum_required(VERSION 3.4.1)
set(libs_DIR src/main/jnilib/${ANDROID_ABI})
link_directories(${libs_DIR})

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        src/main/jni/native-lib.cpp )

add_library( netguard
             SHARED
             src/main/jni/netguard/netguard.c
             src/main/jni/netguard/session.c
             src/main/jni/netguard/ip.c
             src/main/jni/netguard/tcp.c
             src/main/jni/netguard/udp.c
             src/main/jni/netguard/icmp.c
             src/main/jni/netguard/dns.c
             src/main/jni/netguard/dhcp.c
             src/main/jni/netguard/pcap.c
             src/main/jni/netguard/util.c )

include_directories( src/main/jni/netguard/ )

find_library( log-lib
              log )

target_link_libraries( netguard
                       ${log-lib} )

target_link_libraries( # Specifies the target library.
        native-lib
        xhook
        AK
        )

find_library( # Sets the name of the path variable.
        log-lib
        log )


target_link_libraries( # Specifies the target library.
        native-lib
        ${log-lib}
        )