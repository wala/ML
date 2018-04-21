def bad():
    return 7

def id(x):
	return x
	
class Foo(object):
	def foo(self, a, b):
		return a(b)
		
m = Foo()
n = m.foo(id, b=bad)

print(n())
