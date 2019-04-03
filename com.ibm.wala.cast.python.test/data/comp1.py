
def f1(a):
    return lambda: a+1

def f2(a):
    return lambda: a+2

def f3(a):
    return lambda: a+3

fs = [f1, f2, None, f3]
vs = [f(0) for f in fs if f is not None]

print([f() for f in vs])
