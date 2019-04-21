import numpy as np

x = { 'tolist' : lambda: 15 }

df_ann = { 'out' : x }

yv_ann1 = np.mat().T

yv_ann2 = np.mat( 5 ).T

yv_ann3 = np.mat( df_ann['out'].tolist()).T

yv_ann4 = np.mat( df_ann['out'].tolist())
