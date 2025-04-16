from flask import Flask, request, jsonify  # type: ignore
from flask_cors import CORS  # type: ignore
from pymongo import MongoClient  # type: ignore
import os

app = Flask(__name__)
CORS(app)

client = MongoClient("mongodb+srv://jaysheth1304:jayashvi1310@cluster0.pwjbm18.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0")
db = client["IOUApp"]
users = db["users"]

@app.route("/register", methods=["POST"])
def register():
    data = request.get_json()
    username = data.get("username")
    email = data.get("email")
    password = data.get("password")

    if not username or not email or not password:
        return jsonify({"status": "error", "message": "Missing fields"}), 400

    if users.find_one({"username": username}):
        return jsonify({"status": "error", "message": "Username already taken"}), 409

    if users.find_one({"email": email}):
        return jsonify({"status": "error", "message": "Email already exists"}), 409

    users.insert_one({"username": username, "email": email, "password": password})
    print("User registered:", {"username": username, "email": email})
    return jsonify({"status": "success", "message": "User registered successfully"}), 201

@app.route("/login", methods=["POST"])
def login():
    data = request.get_json()
    email = data.get("email")
    password = data.get("password")

    user = users.find_one({"email": email})
    if user and user["password"] == password:
        return jsonify({"status": "success", "message": "Login successful"}), 200
    else:
        return jsonify({"status": "error", "message": "Invalid credentials"}), 401


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)

