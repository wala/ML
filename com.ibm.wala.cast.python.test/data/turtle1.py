import numpy as np

a = np.arange(6).reshape(3, 2)

b = np.reshape(np.ravel(a, order='F'), (2, 3), order='F')

print(b)
