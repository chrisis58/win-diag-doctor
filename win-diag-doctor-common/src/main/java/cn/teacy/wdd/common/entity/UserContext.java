package cn.teacy.wdd.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserContext {
    private boolean isAdmin;
    private boolean isReader;

    public static UserContext UNKNOWN = new UserContext(false, false);
}