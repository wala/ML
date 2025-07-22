def MAGIC_EQ(x, y):
    return x == y


def Banana():
    c = 5
    b = c + 7
    d = 10
    a = 0
    if MAGIC_EQ(c, b):
        a = 2 * c
    else:
        a = c + 6

    a = b + c
    if MAGIC_EQ(a, d):
        raise ("Oh no")


Apple()
Banana()
