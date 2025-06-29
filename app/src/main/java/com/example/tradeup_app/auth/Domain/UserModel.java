package com.example.tradeup_app.auth.Domain;



public class UserModel {
    public String uid;
    public String email;
    public String username;
    public String profilePic;
    public String bio;
    public String contact;
    public int rating;
    private boolean deactivated;
    public UserModel() {}
    public UserModel(String uid, String email, String username, String photoUrl, String bio, String contact, String s) {} // Firebase cần constructor rỗng

    public UserModel(String uid, String email, String username, String profilePic, String bio, String contact, int rating) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.profilePic = profilePic;
        this.bio = bio;
        this.contact = contact;
        this.rating = rating;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = (int) rating; }

    public boolean isDeactivated() { return deactivated; }
    public void setDeactivated(boolean deactivated) { this.deactivated = deactivated; }

    // Add admin functionality
    private boolean isAdmin = false;

    public boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(boolean isAdmin) { this.isAdmin = isAdmin; }
}
