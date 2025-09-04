a = 10


def f():
    global a
    a = a + 1


assert a == 10
f()
assert a == 11
