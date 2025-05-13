
g = lambda x: x+1

f = lambda x, y=g: y(x) 

print(f(7))
print(f(7, lambda x: x-1))

