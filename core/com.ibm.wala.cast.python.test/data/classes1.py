top = 5


class Outer:
    fromOuter = top + 1
    local = 17

    def foo(self, x):
        return self.local + x

    class Inner:
        def foo(self, x):
            return x - 1


x = Outer()
v = x.foo(6)
print(v)

y = x.Inner()
w = y.foo(3)
print(w)
