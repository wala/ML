def f1(a):
    return lambda x: a + x


def f2(a):
    return lambda x: a * x


def f3(a):
    return lambda x: a - x


def gen():
    yield f1
    yield from gen1()


def gen1():
    yield f2
    yield f3


def p1(f, i):
    return f(i)


def p2(f, i):
    print(f(i))


i = 0
for f in gen():
    g = p1(f, i)
    p2(g, i)
    i = i + 1
