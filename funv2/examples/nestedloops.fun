# Tests nested while, multiple variables, write.

proc main ():
    int i = 1
    int j = 1
    int product = 0
    while i < 6:
        j = 1
        while j < 6:
            product = i * j
            write(product)
            j = j + 1 .
        i = i + 1 .
.