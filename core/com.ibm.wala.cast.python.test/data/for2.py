def f1(a):
    return a + 1


def f2(a):
    return a + 2


def f3(a):
    return a + 3


def doit(fs):
    for f in fs:
        x = f(0)
        if x < 3:
            print(f(0))
        else:
            break


fs = [f1, f2, f3]
doit(fs)
