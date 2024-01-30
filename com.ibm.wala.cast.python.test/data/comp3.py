def f1(a):
    return lambda: a + 1


def f2(a):
    return lambda: a + 2


def f3(a):
    return lambda: a + 3


fs = [f1, f2, None, f3]


def g1(f):
    return f


def g2(f):
    return lambda x: f(x + 1)


gs = [g1, g2]

vs = [g(f)(0) for g in gs for f in fs if f is not None]

print([f() for f in vs])
