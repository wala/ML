class Foo:
	def foo(self, a, b):
		return a, b


id = lambda x: x


m = Foo()
n, o = m.foo(lambda x: id(x), b=lambda x: id(x+1))

print(n(2))
print(o(2))
