package antifraud.enums;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum Role {
    ADMINISTRATOR, MERCHANT, SUPPORT;

    public static Role getRole(String input) {
        for (Role role : Role.values()) {
            if (role.name().equals(input) && !input.equals("ADMINISTRATOR"))
                return role;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    public String getRoleWithPrefix() {
        return "ROLE_" + this.name();
    }

}
