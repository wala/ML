def bad():
    return 7


def id(x):
    return x


def foo(a,b):
    return id(a+b)


foo.x = foo;
print(foo.x(b=7, a=6));
bd = id(x=bad)
bd()

