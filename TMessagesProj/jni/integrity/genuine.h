#include <stdbool.h>

#ifdef NDEBUG
#define GENUINE_NAME {0x74, 0x76, 0x2c, 0x6d, 0x61, 0x6e, 0x69, 0x6a, 0x61, 0x64, 0x63, 0x25, 0x62, 0x68, 0x65, 0x60, 0x77, 0x63, 0x73, 0x7e, 0x0}
#else
#define GENUINE_NAME {0x71, 0x71, 0x29, 0x66, 0x6c, 0x61, 0x64, 0x61, 0x64, 0x63, 0x66, 0x3e, 0x7f, 0x77, 0x78, 0x6f, 0x66, 0x70, 0x62, 0x69, 0x2b, 0x64, 0x62, 0x7c, 0x68, 0x0}
#endif
#define GENUINE_SIZE 0x02d7
#define GENUINE_HASH 0x35d0f48e

bool checkGenuine(JNIEnv *env);