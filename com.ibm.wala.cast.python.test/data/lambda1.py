x = lambda a: a+1
print(x(3))

class Foo:
    def foo(self, a, b):
        return a, b


m = Foo()
n, o = m.foo(lambda x: x, b=lambda x: x + 1)

print(n(2))
print(o(2))
