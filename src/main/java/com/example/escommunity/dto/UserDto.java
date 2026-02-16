package com.example.escommunity.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserDto {
    private String email;
    private String password;
    private String role;
}
