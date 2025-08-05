def f(v):
    return v + 1


def ff(f, v):
    return f(v), f(v + 1)


def fff(ff, f, v):
    return ff(f, v), ff(f, v + 1)


a = f(0)
b, c = ff(f, 0)
d, e = fff(ff, f, 0)

print(a)
print(b)
print(c)
print(d)
print(e)
