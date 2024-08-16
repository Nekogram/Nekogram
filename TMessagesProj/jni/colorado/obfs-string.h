#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wgnu-string-literal-operator-template"

#include <utility>
#include <string_view>

#if __cplusplus >= 202002L
#define CONSTEVAL consteval
#else
#define CONSTEVAL constexpr
#endif

CONSTEVAL bool isPrime(std::size_t x) {
    if (x == 2 || x == 3)
        return true;

    if (x % 2 == 0 || x % 3 == 0)
        return false;

    std::size_t divisor = 6;
    while (divisor * divisor - 2 * divisor + 1 <= x) {

        if (x % (divisor - 1) == 0)
            return false;

        if (x % (divisor + 1) == 0)
            return false;

        divisor += 6;
    }
    return true;
}

CONSTEVAL std::size_t nextPrime(std::size_t x) {
    while (!isPrime(x)) ++x;
    return x;
}

template<char... cs>
class InlineObfs {
private:
    char inner[sizeof...(cs) + 1];

public:
    inline InlineObfs() : inner{cs..., '\0'} {
        for (std::size_t i = 0; i < sizeof...(cs); ++i) {
            inner[i] ^= (i + sizeof...(cs)) % nextPrime(sizeof...(cs));
        }
    }

    constexpr const char *c_str() const { return inner; }

    constexpr std::size_t size() const { return sizeof...(cs); }

    operator std::string_view() const { return {inner}; }
};

template<char... is, std::size_t... I>
constexpr inline auto MakeInlineObfs(std::index_sequence<I...>) {
    return InlineObfs<(is ^ ((I + sizeof...(is)) % nextPrime(sizeof...(is))))...>();
}

template<char... cs>
class StaticObfs {
public:
    CONSTEVAL StaticObfs() = default;

    constexpr auto obtain() const {
        return MakeInlineObfs<cs...>(std::make_index_sequence<sizeof...(cs)>());
    }
};

template<typename T, T... cs>
constexpr inline auto operator ""_iobfs() {
    return MakeInlineObfs<cs...>(std::make_index_sequence<sizeof...(cs)>());
}

template<typename T, T... cs>
CONSTEVAL inline auto operator ""_sobfs() {
    return StaticObfs<cs...>{};
}

template<char... as, char... bs>
CONSTEVAL inline auto operator+(const StaticObfs<as...> &, const StaticObfs<bs...> &) {
    return StaticObfs<as..., bs...>{};
}

#pragma clang diagnostic pop
