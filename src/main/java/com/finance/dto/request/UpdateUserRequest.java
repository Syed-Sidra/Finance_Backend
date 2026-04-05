package com.finance.dto.request;

import com.finance.enums.Role;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    private Role role;

    private Boolean active;
}
