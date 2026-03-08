import os
import json
import firebase_admin
from firebase_admin import credentials


def initialize_firebase():
    if not firebase_admin._apps:
        firebase_creds_json = os.getenv("FIREBASE_CREDENTIALS")
        if not firebase_creds_json:
            raise ValueError("FIREBASE_CREDENTIALS is not set in environment")
        cred = credentials.Certificate(json.loads(firebase_creds_json))
        firebase_admin.initialize_app(cred)
