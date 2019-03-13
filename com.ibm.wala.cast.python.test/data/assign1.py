def a(x, y):
    return x(y)

p = q = a

def f(ff):
    return ff(3)
    
def g(fff):
    return fff(4)
    
f(p)
g(q)
