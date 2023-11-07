def g(p1, p2):
    assert p1 == 5 and p2 == 2


def f():
    g(5, p2=2)


f()
