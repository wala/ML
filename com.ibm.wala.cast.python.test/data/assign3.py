def f(v):
    return lambda x: v + x


def ff(f, v):
    return lambda x: f(v + x), lambda x: f(v + x + 1)


def fff(ff, f, v):
    return lambda x: ff(f, v+x), lambda x: ff(f, v+x+1)


a = f(0)
v = a(1)
b, c = ff(f, 0)
w = b(2)(0)
x = c(3)(1)

d, e = fff(ff, f, 0)
y, m = d(4)
z, n = e(5)

print(v)
print(w)
print(x)
print(y(0)(0))
print(z(0)(0))
print(m(0)(0))
print(n(0)(0))
