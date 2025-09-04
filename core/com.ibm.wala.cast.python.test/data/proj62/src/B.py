#! usr/bin/env python3
# -*- coding:utf-8 -*-
"""
@Author:Kaiyin Zhou
"""
from tensorflow import Tensor


class C:

    def __init__(self):
        pass

    def __call__(self, a):
        assert isinstance(a, Tensor)
