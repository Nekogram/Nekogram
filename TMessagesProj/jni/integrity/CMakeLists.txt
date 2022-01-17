find_package(xposeddetector REQUIRED CONFIG)

add_library(genuine STATIC
        integrity/am-proxy.c
        integrity/apk-sign-v2.c
        integrity/common.c
        integrity/genuine.c
        integrity/inline.c
        integrity/openat.c
        integrity/path.c
        integrity/plt.c
        integrity/pm.c)
target_compile_options(genuine PUBLIC
        -Oz -Wall -Wextra -Wshadow -Werror -fvisibility=hidden)
target_include_directories(genuine PUBLIC
        integrity)
target_link_libraries(genuine -Wl,--no-whole-archive xposeddetector::xposed_detector)
