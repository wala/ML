import pandas as pd

dfqol = pd.read_excel("Master_Data_Sheet_06252018.xlsx", sheetname="EQ5D")
dfdemog = pd.read_excel(
    "Master_Data_Sheet_06252018.xlsx", sheetname="Patient_Info_Demographics", skiprows=1
)

# format participant IDs uniformly across each dataframe
dfqol["PID"] = dfqol["PID"].str.replace("'", "")
dfdemog["PID"] = dfdemog["Patient_ID"].str.replace("-", "")
