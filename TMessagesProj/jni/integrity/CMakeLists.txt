add_library(genuine STATIC
        integrity/apk-sign-v2.c
        integrity/common.c
        integrity/genuine.c
        integrity/openat.c
        integrity/pm.c)
target_compile_options(genuine PUBLIC
        -Oz -fvisibility=hidden)
target_include_directories(genuine PUBLIC
        integrity)
