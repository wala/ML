
def ctwo(x, y):
    if (x < y):
        return 1
    else:
        return 0

def cthree(x, y, z):
    if (x < y < z):
        return 1
    else:
        return 0

def cfour(w, x, y, z):
    if (w < x < y < z):
        return 1
    else:
        return 0

ctwo(0, 1)
cthree(0, 1, 2)
cfour(0, 1, 3, 2)
