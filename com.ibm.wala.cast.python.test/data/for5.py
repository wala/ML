
def doit(gs):
    for g in gs:
        gs[g]("a")

fs = ["a", "b", "c"];

gs = {f: lambda x: x + f for f in fs}

doit(gs)

