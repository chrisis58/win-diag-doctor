package cn.teacy.wdd.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserContext {
    private Boolean isAdmin;
    private Boolean isReader;

    public static UserContext UNKNOWN = new UserContext(false, false);
}