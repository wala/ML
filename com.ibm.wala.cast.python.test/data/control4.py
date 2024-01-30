import sys

a = sys.argv[1]
b = sys.argv[2]


class c1:
    a


x = c1()
x.a = "foo"
# reveal_type(x.a)

y = c1()
y.a = 7
# reveal_type(y.a)

if a == b:
    y = x

y.a = 7

print(x.a)
reveal_type(x.a)
