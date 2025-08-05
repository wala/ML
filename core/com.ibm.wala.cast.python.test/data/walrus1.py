class Foo:
    def __init__(self, a):
        self.a = a

    def f(self, a):
        return a < self.a

    def h(self, x):
        print(x + self.a)


def g(x):
    return Foo(x)


if (a := g(3)).f(2):
    a.h(0)
