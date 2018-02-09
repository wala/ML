base_init = 10

def id(x):
    return x

def call(x, y):
    return x(y)

def foo(a,b):
    return call(id, a+b)

print(foo)
print(foo(1,2))

def nothing():
    return 0

z = id(nothing)
z()
