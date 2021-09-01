import sys
a = sys.argv[1]
b = sys.argv[2]
#i = 0
for i in range(0,5): #i < 5:
    if a == b:
        x = "foo"
        break
    x = 7
    #i = i + 1
print(x)
reveal_type(x)
