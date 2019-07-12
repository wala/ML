
def listTest():
	l = [1,2,3,4]
	print(l[2])
	l[2] = 5
	print(l)

def tupleTest():
	t = (1,2,3,4)
	print(t[2])
	#t[2] = 5 		#Executing this on a tuple will cause an error
	print(t)
	
def setTest():
	s = {1,2,3,4}
	print(s)
	
def dictTest():
	d = {1:2, 3:"90", "hey" : "hi", "3":90} #right now different kinds of field instructions are used for strings and numbers 
	print(d)
	print(d["hey"])
	d["hey"] = "hey"
	#d.hey = "hey"                #This line would cause the program to crash, it would however produce the same SSAInstruction as the line above 
	var = d["hey"]
	print(var)
	

listTest()
tupleTest()
setTest()
dictTest()
