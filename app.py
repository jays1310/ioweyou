from flask import Flask, request, jsonify # type: ignore
from flask_cors import CORS # type: ignore
from pymongo import MongoClient # type: ignore
import os, uuid, traceback

app = Flask(__name__)
CORS(app)

# MongoDB setup
client = MongoClient(
    "mongodb+srv://jaysheth1304:jayashvi1310@cluster0.pwjbm18.mongodb.net/"
    "?retryWrites=true&w=majority&appName=Cluster0"
)
db = client["IOUApp"]
users = db["users"]
groups = db["groups"]

@app.route("/")
def home():
    return "Flask backend is running!"

# ------------------------- User Registration -------------------------
@app.route("/register", methods=["POST"])
def register():
    data = request.get_json()
    username = data.get("username")
    email    = data.get("email")
    password = data.get("password")
    contact  = data.get("contact")

    print("Received data:", data)
    print("👀 Username:", data.get("username"))
    print("📞 Contact:", data.get("contact"))

    if not username or not email or not password or not contact:
        return jsonify({"status": "error", "message": "Missing fields"}), 400

    if users.find_one({"username": username}):
        return jsonify({"status": "error", "message": "Username already taken"}), 409

    if users.find_one({"email": email}):
        return jsonify({"status": "error", "message": "Email already exists"}), 409

    if users.find_one({"contact": contact}):
        return jsonify({"status": "error", "message": "Contact already exists"}), 409

    users.insert_one({
        "username": username,
        "email": email,
        "password": password,
        "contact": contact
    })
    return jsonify({"status": "success", "message": "User registered successfully"}), 201

# ------------------------- User Login (by email, username or contact) -------------------------
@app.route("/login", methods=["POST"])
def login():
    data = request.get_json()
    identifier = data.get("identifier")  # could be email, username or contact
    password   = data.get("password")

    if not identifier or not password:
        return jsonify({"status": "error", "message": "Identifier and password required"}), 400

    user = users.find_one({
        "$or": [
            {"email": identifier},
            {"username": identifier},
            {"contact": identifier}
        ]
    })

    if user and user.get("password") == password:
        return jsonify({"status": "success", "message": "Login successful"}), 200

    return jsonify({"status": "error", "message": "Invalid credentials"}), 401

# ------------------------- Create Group -------------------------
@app.route("/create_group", methods=["POST"])
def create_group():
    try:
        data = request.get_json()
        name    = data.get("group_name")
        members = data.get("members")
        if not name or not members or not isinstance(members, list):
            return jsonify({"error": "Group name and member list required"}), 400

        group_id = str(uuid.uuid4())[:8]
        group = {"group_id": group_id, "name": name, "members": members}
        groups.insert_one(group)
        return jsonify({"message": "Group created", "group_id": group_id}), 200

    except Exception:
        traceback.print_exc()
        return jsonify({"error": "Internal Server Error"}), 500

# ------------------------- Join Group -------------------------
@app.route("/join_group", methods=["POST"])
def join_group():
    try:
        data     = request.get_json()
        identifier = data.get("identifier")  # could be email or contact
        group_id   = data.get("group_id")

        if not identifier or not group_id:
            return jsonify({"error": "Identifier and group_id required"}), 400

        result = groups.update_one(
            {"group_id": group_id},
            {"$addToSet": {"members": identifier}}
        )

        if result.matched_count == 0:
            return jsonify({"error": "Group not found"}), 404

        return jsonify({"message": "Joined group"}), 200

    except Exception:
        traceback.print_exc()
        return jsonify({"error": "Internal Server Error"}), 500

# ------------------------- Get User's Groups -------------------------
@app.route("/user_groups", methods=["GET"])
def get_user_groups():
    identifier = request.args.get("identifier")  # could be email or contact
    if not identifier:
        return jsonify({"error": "Identifier is required"}), 400

    user_groups = groups.find({"members": identifier})
    payload = [{"group_id": g["group_id"], "name": g.get("name", "")} for g in user_groups]
    return jsonify({"groups": payload}), 200

# ------------------------- Get All Users -------------------------
@app.route("/all_users", methods=["GET"])
def all_users():
    try:
        contacts = [u["contact"] for u in users.find({}, {"contact": 1, "_id": 0})]
        return jsonify({"users": contacts}), 200
    except Exception:
        traceback.print_exc()
        return jsonify({"error": "Internal Server Error"}), 500

# ------------------------- Check if User Exists -------------------------
@app.route("/check_user_exists", methods=["POST"])
def check_user_exists():
    data = request.get_json()
    identifier = data.get("identifier")

    if not identifier:
        return jsonify({"status": "error", "message": "Identifier is required"}), 400

    user = users.find_one({
        "$or": [
            {"email": identifier},
            {"username": identifier},
            {"contact": identifier}
        ]
    })

    if user:
        return jsonify({"status": "success", "message": "User exists"}), 200
    else:
        return jsonify({"status": "error", "message": "User not found"}), 404

# ------------------------- Get Group Members -------------------------
@app.route("/get_group_members", methods=["POST"])
def get_group_members():
    data = request.get_json()
    group_id = data.get("group_id")

    group = groups.find_one({"group_id": group_id})
    if group and "members" in group:
        return jsonify({"members": group["members"]})
    else:
        return jsonify({"members": []}), 404

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)
