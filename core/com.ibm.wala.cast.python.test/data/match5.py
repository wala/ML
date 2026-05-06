class Nothing:
    def act(self):
        print("Nothing")


def id(x):
    return x


class Something:
    a = 0
    b = 0

    def __init__(self, a, b):
        self.a = id(a)
        self.b = id(b)

    def act(self):
        print(self.a + self.b)


def doit(x):
    match x:
        case Nothing():
            x.act()
        case Something(a=5, b=7):
            x.act()
        case Something():
            print("unexpected something")
        case _:
            print("unexpected value")


doit(Nothing())
doit(Something(5, 7))
doit(Something(3, 4))
doit(10)
