def silly(x):
    def inner(y):
        return x+y;
    return inner(1)

x = 7
