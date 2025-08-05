def fa(x):
    return x + 1


def fb(x):
    return x + 2


def fc(x):
    return x + 3


class Ctor:
    def __init__(self, a, b, c):
        self.a = a
        self.b = b
        self.c = c

    def get(self, x):
        return self.a(x) + self.b(x) + self.c(x)


x = Ctor(fa, fb, fc)
print(x.get(3))


class SubCtor(Ctor):
    def __init__(self, a):
        super(SubCtor, self).__init__(a, a, a)


x = SubCtor(fc)
print(x.get(4))


class OtherSubCtor(Ctor):
    def __init__(self, a):
        super().__init__(a, a, a)


x = OtherSubCtor(fc)
print(x.get(3))
