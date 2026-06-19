def monday():
    print("Monday")


def tuesday():
    print("Tuesday")


def wednesday():
    print("Wednesday")


def thursday():
    print("Thursday")


def friday():
    print("Friday")


def saturday():
    print("Saturday")


def sunday():
    print("Sunday")


def otherDay():
    print("other day??")


for day in [1, 2, 3, 4, 5, 6, 7, otherDay]:
    match day:
        case 1:
            monday()
        case 2:
            tuesday()
        case 3:
            wednesday()
        case 4:
            thursday()
        case 5:
            friday()
        case 6:
            saturday()
        case 7:
            sunday()
        case x:
            x()
