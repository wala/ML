import numpy as np
def get_punctuation_mark(sentence):
    cc=0
    for word in sentence:
        cc = np.sum(word)

    return cc