def makeGenerator(fs):
    return (f(3) for f in fs)


def f1(a):
    return lambda x: a + x


def f2(a):
    return lambda x: a * x


def f3(a):
    return lambda x: a - x


fs = [f1, f2, f3]

gs = makeGenerator(fs)

for f in gs:
    print(f(1))
