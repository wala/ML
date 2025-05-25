class e1(Exception):
    def f(self):
        print("e1")


class e2(Exception):
    def f(self):
        print("e2")


class e3(Exception):
    def f(self):
        print("e3")


def f1(x):
  if x > 5:
    raise e1()


def f2(x):
  if x > 3:
    raise e2() from e3()


def f3(e):
    e.f()
    e.__cause__.f()


try:
    f1(10)
except Exception as e:
    e.f()

try:
    f2(10)
except Exception as e:
    f3(e)
