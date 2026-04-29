#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void print_i32(int value) {
    printf("%d\n", value);
}

void print_bool(int value) {
    puts(value ? "true" : "false");
}

void print_str(char *value) {
    puts(value ? value : "nil");
}

int read_i32(void) {
    int value = 0;
#ifdef _MSC_VER // Windows (otherwise gives warning)
    if (scanf_s("%d", &value) != 1) {
#else // Linux and Mac
    if (scanf("%d", &value) != 1) {
#endif
        return 0;
    }
    return value;
}

char *pas_str_concat(const char *left, const char *right) {
    const char *a = left ? left : "";
    const char *b = right ? right : "";
    size_t lenA = strlen(a);
    size_t lenB = strlen(b);
    char *out = (char *)malloc(lenA + lenB + 1);
    if (!out) {
        fprintf(stderr, "out of memory\n");
        exit(1);
    }
    memcpy(out, a, lenA);
    memcpy(out + lenA, b, lenB + 1);
    return out;
}
