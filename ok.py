import os
import shutil
import subprocess

#rm db.sqlite3 before running this script

# Name of your Django app
APP_NAME = "TumaGo_Server"
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MIGRATIONS_PATH = os.path.join(BASE_DIR, APP_NAME, "migrations")

def delete_migration_files():
    print(f"🔄 Cleaning migration files in: {MIGRATIONS_PATH}")

    if not os.path.exists(MIGRATIONS_PATH):
        print("❌ Migrations directory not found.")
        return

    for filename in os.listdir(MIGRATIONS_PATH):
        file_path = os.path.join(MIGRATIONS_PATH, filename)

        if filename == "__init__.py":
            continue
        elif filename.endswith(".py") or filename.endswith(".pyc"):
            os.remove(file_path)
            print(f"🗑️ Deleted file: {file_path}")
        elif os.path.isdir(file_path) and filename == "__pycache__":
            shutil.rmtree(file_path)
            print(f"🧹 Deleted cache directory: {file_path}")

def make_and_apply_migrations():
    print("\n📦 Running makemigrations...")
    subprocess.run(["python", "manage.py", "makemigrations", APP_NAME])

    print("\n⚙️ Running migrate...")
    subprocess.run(["python", "manage.py", "migrate"])

if __name__ == "__main__":
    delete_migration_files()
    make_and_apply_migrations()
