package com.wflow.org;

import com.wflow.workflow.bean.process.OrgUser;
import com.wflow.workflow.service.UserDeptOrLeaderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * @author : willian fu
 * @date : 2022/9/25
 */
@Slf4j
@SpringBootTest
public class UserLeaderTest {

    @Autowired
    private UserDeptOrLeaderService leaderService;

    //获取用户所在部门的指定级别部门leader
    @Test
    public void getUserDeptLeader(){
        //最上层公司
        //String leader1 = leaderService.getUserLeaderByLevel("3243678", "35453", 1, false);
        //研发部
        List<String> leader2 = leaderService.getUserLeadersByLevel("3243678", "35453", 2, false);
        Map<String, OrgUser> userMapByIds = leaderService.getUserMapByIds(leader2);
        System.out.println();
    }
}
