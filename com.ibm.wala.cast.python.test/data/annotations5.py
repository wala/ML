a : int = 3
b : int = 4

class foof:
    foo: int = 42
    bar: 17 = 42
    baz: a + b = 42
    bang: lambda: 17 = lambda self, x: x + self.foo_f()
    def foo_f(self):
        return self.foo

print(foof.__annotations__)
print(foof.__annotations__['bang']())

x = foof()
print(x.foo)
x.foo = 10
print(foof.foo)
print(x.foo)
print(foof().bang(5))
print(foof().foo_f())

