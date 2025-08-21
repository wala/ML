
def f(x):
    return 3*x

def g(x):
    if x > 0:
        return f"this is {g(x-1)} or {f(x)}"
    else:
        return f"this is {f(x+1)} or {f(x)}"

print(g(3))
