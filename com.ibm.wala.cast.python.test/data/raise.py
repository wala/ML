class e1(Exception):
    def f(self):
        print("e1")


class e2(Exception):
    def f(self):
        print("e2")


class e3(Exception):
    def f(self):
        print("e3")


try:
    raise e1()
except Exception as e:
    e.f()

try:
    raise e2() from e3()
except Exception as e:
    e.f()
    e.__cause__.f()
