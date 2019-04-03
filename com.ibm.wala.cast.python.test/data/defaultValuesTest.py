
def defValTest(a, l=lambda x: x+1):
	print l(a)
	
defValTest(1)
defValTest(2)
b = lambda x: x+3
defValTest(3,b)
defValTest(4)


