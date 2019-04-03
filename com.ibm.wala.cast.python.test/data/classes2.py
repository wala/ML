def fa(x):
    return x+1

def fb(x):
    return x+2

def fc(x):
    return x+3

class Ctor():
    def __init__(self, a, b, c):
        self.a = a
        self.b = b
        self.c = c
        
    def get(self, x):
        return self.a(x) + self.b(x) + self.c(x)
    
if fa(3) > fb(3):
    x = fa
    fb = fa
    fa = x
    
x = Ctor(fa, fb, fc)
print( x.get(3) )
