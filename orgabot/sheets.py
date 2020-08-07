import gspread
from gspread.auth import READONLY_SCOPES


class SheetsInterface:
    READ_SCOPE = 'https://www.googleapis.com/auth/spreadsheets.readonly'

    def __init__(self, credentials_file: str):
        self.credentials_file = credentials_file
        self.service = gspread.service_account(filename=self.credentials_file, scopes=READONLY_SCOPES)

    def read_col(self, spreadsheet_title: str, locator: str):
        sheet = self.service.open(spreadsheet_title)
        return sheet.sheet1.get(locator)



