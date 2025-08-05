class Foo:
    def foo(self, a, b):
        return a, b


def bad(x):
    return x + 1


def id(x):
    return x


m = Foo()
n, o = m.foo(id, b=bad)

print(n(2))
print(o(2))
