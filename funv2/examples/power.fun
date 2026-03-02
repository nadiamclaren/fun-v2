# Tests func with two args, multiplication accumulation.

func int power (int base, int exp):
    int result = 1
    int i = 0
    while i < exp:
        result = result * base
        i = i + 1 .
    return result .

proc main ():
    write(power(2, 0))
    write(power(2, 1))
    write(power(2, 4))
    write(power(2, 8))
    write(power(3, 3))
    write(power(5, 4))
.