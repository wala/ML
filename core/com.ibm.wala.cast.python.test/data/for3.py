def f1(a):
    return a + 1


def f2(a):
    return a + 2


def f3(a):
    return a + 3


fs = [f1, f2, f3]


def g1(f, a):
    return f(a)


def g2(f, a):
    return f(a + 1)


def g3(f, a):
    return f(a + 2)


gs = [g1, g2, g3]


for f in fs:
    for g in gs:
        print(g(f, 0))
