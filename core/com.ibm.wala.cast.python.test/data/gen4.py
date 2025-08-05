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
    
i = 0
for f in gen():
    g = f(i)
    print(g(i))
    i = i + 1
