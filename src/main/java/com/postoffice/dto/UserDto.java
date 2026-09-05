package com.postoffice.dto;

import com.postoffice.model.User;

import java.math.BigDecimal;

public class UserDto {
    private Long id;
    private String name;
    private String email;
    private BigDecimal balance;

    public static UserDto from(User user) {
        UserDto dto = new UserDto();
        dto.id = user.getId();
        dto.name = user.getName();
        dto.email = user.getEmail();
        dto.balance = user.getBalance();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public BigDecimal getBalance() { return balance; }
}
