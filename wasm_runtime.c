typedef unsigned int u32;
typedef unsigned long long u64;

extern unsigned char __heap_base;

static u32 heap_ptr = 0u;

static u32 align8(u32 value) {
    return (value + 7u) & ~7u;
}

void *malloc(u64 size) {
    if (heap_ptr == 0u) {
        heap_ptr = align8((u32)&__heap_base);
    }

    u32 old = heap_ptr;
    heap_ptr = align8(heap_ptr + (u32)size);
    return (void *)old;
}

int strcmp(char *left, char *right) {
    unsigned char *a = (unsigned char *)(left != 0 ? left : "");
    unsigned char *b = (unsigned char *)(right != 0 ? right : "");

    while (*a != 0 && *a == *b) {
        a++;
        b++;
    }

    return (int)(*a) - (int)(*b);
}

static u32 strlen0(char *text) {
    u32 n = 0u;

    if (text == 0) {
        return 0u;
    }

    while (text[n] != 0) {
        n++;
    }

    return n;
}

char *pas_str_concat(char *left, char *right) {
    u32 len_left = strlen0(left);
    u32 len_right = strlen0(right);
    char *out = (char *)malloc((u64)len_left + (u64)len_right + 1u);

    for (u32 i = 0u; i < len_left; i++) {
        out[i] = left[i];
    }

    for (u32 j = 0u; j < len_right; j++) {
        out[len_left + j] = right[j];
    }

    out[len_left + len_right] = 0;
    return out;
}
