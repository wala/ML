
fs = ["a", "b", "c"];

def mf(a):
    return lambda x: a + x

gs = {f: mf(f) for f in fs}

for g in gs:
    gs[g]("a")

  
