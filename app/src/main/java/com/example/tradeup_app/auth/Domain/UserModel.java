package com.example.tradeup_app.auth.Domain;



public class UserModel {
    private String uid;
    private String email;
    private String username;
    private String profilePic;
    private String bio;
    private String contact;
    private double rating;
    private boolean isAdmin;
    private boolean deactivated;
    private boolean isFlagged;
    private String flaggedReason;

    // Constructor rỗng cho Firebase
    public UserModel() {}


    // Constructor đầy đủ
    public UserModel(String uid, String email, String username, String profilePic, String bio, String contact, double rating) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.profilePic = profilePic;
        this.bio = bio;
        this.contact = contact;
        this.rating = rating;
        this.isAdmin = false;
        this.deactivated = false;
        this.isFlagged = false;
        this.flaggedReason = "";
    }

    // Getter và Setter
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }




    public double getRating() { return rating; }

    public void setRating(Object ratingObj) {
        if (ratingObj == null) {
            this.rating = 0.0;
        } else if (ratingObj instanceof String) {
            try {
                this.rating = Double.parseDouble((String) ratingObj);
            } catch (NumberFormatException e) {
                this.rating = 0.0;
            }
        } else if (ratingObj instanceof Number) {
            this.rating = ((Number) ratingObj).doubleValue();
        } else {
            this.rating = 0.0;
        }
    }






    public boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(boolean isAdmin) { this.isAdmin = isAdmin; }

    public boolean isDeactivated() { return deactivated; }
    public void setDeactivated(boolean deactivated) { this.deactivated = deactivated; }

    public boolean isFlagged() { return isFlagged; }
    public void setFlagged(boolean flagged) { this.isFlagged = flagged; }
    public String getFlaggedReason() { return flaggedReason; }
    public void setFlaggedReason(String flaggedReason) { this.flaggedReason = flaggedReason; }
}