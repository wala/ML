
fs = ["a", "b", "c"];

gs = {f: lambda x: x + f for f in fs}

for g in gs:
    gs[g]("a")

  
