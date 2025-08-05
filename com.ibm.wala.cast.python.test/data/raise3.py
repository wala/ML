class e1(Exception):
    def f(self):
        return "e1"


class e2(Exception):
    def f(self):
        return "e2"


class e3(Exception):
    def f(self):
        return "e3"


try:
    raise ExceptionGroup("g", [e1(), e2()]) from e3()
except* e1 as e:
    print({s.f() for s in e.exceptions})
    print(e.__cause__.f())
except* e2 as e:
    print({s.f() for s in e.exceptions})
    print(e.__cause__.f())



