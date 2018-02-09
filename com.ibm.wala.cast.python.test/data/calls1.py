base_init = 10

def id(x):
    return x

def call(x, y):
    return x(y)

class Foo(object):
    base = base_init
    
    def foo(self, a, b):
        self.contents = call(id, a+b+self.base)
        return self.contents

print(Foo)

print(Foo.foo)
print(Foo.base)

instance = Foo()
print(Foo.foo(instance, 2,3))
print(instance.foo(2,3))

f = instance.foo
print(f);
print(f(3,4))

x = Foo
print(x)
y = x()
print(y)
print(y.foo(7,8))
