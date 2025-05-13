fs = [1, 2, 3]

def mf(a):
    return lambda x: x + a

for f in fs:
   x = mf(f)
   for g in fs:
      print(x(g))
