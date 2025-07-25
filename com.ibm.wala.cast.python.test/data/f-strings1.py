
def f(x):
    return 3*x

def g(x):
    return f(x+1)

x = f(0) + g(0)

print(f"this is {g(x+1)} or {f(x)}")
