func int fib (int n):
    int result = 0
    if n < 2:
        result = n
    else:
        result = fib(n-1) + fib(n-2) .
    return result .

proc main ():
    int i = 0
    while i < 10:
        write(fib(i))
        i = i + 1 .
.