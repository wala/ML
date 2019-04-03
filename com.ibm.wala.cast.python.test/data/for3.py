
def f1(a):
    return a + 1

def f2(a):
    return a + 2

def f3(a):
    return a + 3

fs = [f1, f2, f3]

for f in fs:
	for g in fs:
		print(f(0)+g(0))

