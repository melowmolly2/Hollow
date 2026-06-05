package dto.admin;

public class UnbanUserRequest {
    public String username;
    public String password;

    public UnbanUserRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
