package com.example.ioweyou;

import androidx.annotation.NonNull;

/**
 * Model class representing a Group with an ID and a name.
 */
public class Group {
    private final String groupId;
    private final String name;

    /**
     * Constructs a new Group.
     *
     * @param groupId The unique identifier for the group.
     * @param name    The display name of the group.
     */
    public Group(String groupId, String name) {
        this.groupId = groupId;
        this.name = name;
    }

    /**
     * @return The unique identifier of this group.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @return The display name of this group.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of the group for debugging.
     */
    @NonNull
    @Override
    public String toString() {
        return "Group{" +
                "groupId='" + groupId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
