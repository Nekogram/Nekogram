#include <string>

std::string read_certificate(int fd);

ssize_t xxread(int fd, void *buf, size_t count);

bool ends_with(std::string_view str, std::string_view suffix);

bool starts_with(std::string_view str, std::string_view prefix);

bool contains(std::string_view str, std::string_view substr);