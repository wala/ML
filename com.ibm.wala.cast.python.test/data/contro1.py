import sys

class e1(Exception):
   def __str__(self):
       return "exception"

a = sys.argv[1]
b = sys.argv[2]

try:
    if a == b:
        raise e1

    a = 7
    
except e1 as e:
    a = e

print(a)
reveal_type(a)
