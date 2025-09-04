from sklearn.decomposition import PCA
from sklearn.ensemble import RandomForestRegressor
from sklearn.linear_model import RidgeCV
from sklearn.pipeline import make_pipeline

for reg in [RidgeCV(), RandomForestRegressor(random_state=0)]:
    pipeline = make_pipeline(PCA(n_components=100), reg)
    y_pred = pipeline.fit(0, 1).predict(2)
