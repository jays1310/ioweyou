from flask import Flask, request, jsonify
from flask_cors import CORS
from pymongo import MongoClient
import uuid

app = Flask(__name__)
CORS(app)

# MongoDB Setup
client = MongoClient("your_mongodb_connection_string")
db = client['iou_app']
users_collection = db['users']
groups_collection = db['groups']

@app.route('/create_group', methods=['POST'])
def create_group():
    data = request.json
    group_id = str(uuid.uuid4())[:8]  # 8-char unique ID
    group_name = data.get('group_name')
    members = data.get('members')  # Should be a list of usernames or user IDs

    if not group_name or not members:
        return jsonify({'success': False, 'message': 'Missing data'})

    group = {
        'group_id': group_id,
        'group_name': group_name,
        'members': members,
        'transactions': []
    }
    groups_collection.insert_one(group)

    return jsonify({'success': True, 'group_id': group_id})

@app.route('/join_group', methods=['POST'])
def join_group():
    data = request.json
    group_id = data.get('group_id')
    username = data.get('username')

    group = groups_collection.find_one({'group_id': group_id})
    if not group:
        return jsonify({'success': False, 'message': 'Group not found'})

    if username not in group['members']:
        groups_collection.update_one({'group_id': group_id}, {'$push': {'members': username}})

    return jsonify({'success': True, 'message': 'Joined group successfully'})

@app.route('/get_users', methods=['GET'])
def get_users():
    users = list(users_collection.find({}, {'_id': 0, 'username': 1, 'email': 1}))
    return jsonify({'success': True, 'users': users})

if __name__ == '__main__':
    app.run(debug=True)
