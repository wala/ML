
def test1():
  def f1():
    pass

  def f2():
    pass

  def f3():
    pass
  
  try:
    f1()
    print("Hello")
  except:
    f2()
    print("Something went wrong")
  else:
    f3()
    print("Nothing went wrong")


def test2():
  def f1():
    pass

  def f2():
    pass

  def f3():
    pass
  
  try:
    print(x)
    f1()
  except:
    f2()
    print("Something went wrong")
  finally:
    f3()
    print("The 'try except' is finished")

    
def test3():
  def f1():
    pass

  def f2():
    pass

  def f3():
    pass
  
  try:
    f = open("demofile.txt")
    try:
      f.write("Lorum Ipsum")
    except:
      f1()
      print("Something went wrong when writing to the file")
    finally:
      f2()
      f.close()
  except:
    f3()
    print("Something went wrong when opening the file")

    
test1()
test2()
test3()
