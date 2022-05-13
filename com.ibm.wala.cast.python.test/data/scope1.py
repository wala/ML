var_one = 5

def outer_one(x):

    def inner_one(x):

        def inner_two():
            print(var_one)

        inner_two()

        if x>5:
            global var_one
            print("a" + str(var_one))
        else:
            print("b" + str(var_one))

        def inner_three():
            print(var_one)
            
        inner_three()

    var_one = x
    print(var_one)
    inner_one(x)
    print(var_one)

outer_one(3)
