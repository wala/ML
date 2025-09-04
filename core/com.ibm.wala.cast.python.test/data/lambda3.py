class Foo:
    def foo(self, a, b):
        for i in b:
            print(a(i))


ids = [lambda x: x, lambda y: 5 * y]


m = Foo()
m.foo(a=lambda x: x(1), b=ids)
