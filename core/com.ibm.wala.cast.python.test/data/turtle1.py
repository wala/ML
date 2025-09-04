import numpy as np

a = np.arange(6).reshape(3, 2)

b = np.ravel(a, order="F")

if a != b:
    c = a
else:
    c = b

d = np.reshape(c, (2, 3), order="F")

print(d)
