# Tests multiple local variables and temp swapping logic.

func int fib (int n):
    int a = 0
    int b = 1
    int temp = 0
    int i = 0
    while i < n:
        temp = b
        b = a + b
        a = temp
        i = i + 1 .
    return a .

proc main ():
    int i = 0
    while i < 10:
        write(fib(i))
        i = i + 1 .
.