base_init = 10


def id(x):
    return x


def call(x, y):
    return x(y)


def foo(a, b):
    return call(id, a + b)


class Foo(object):
    base = base_init

    def foo(self, a, b):
        self.contents = a + b + self.base
        return self.contents


print(Foo)

print(Foo.foo)
print(Foo.base)

instance = Foo()
print(Foo.foo(instance, 2, 3))
print(instance.foo(b=2, a=3))

instance.foo = foo
print(instance.foo(5, 6))
print(instance.foo)
