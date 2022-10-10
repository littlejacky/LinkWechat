package com.linkwechat.factory.impl.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linkwechat.common.constant.Constants;
import com.linkwechat.common.enums.TrackState;
import com.linkwechat.domain.WeCustomer;
import com.linkwechat.domain.wecom.callback.WeBackBaseVo;
import com.linkwechat.domain.wecom.callback.WeBackCustomerVo;
import com.linkwechat.factory.WeEventStrategy;
import com.linkwechat.service.IWeCustomerService;
import com.linkwechat.service.IWeCustomerTrajectoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author danmo
 * @description 删除企业客户事件
 * @date 2021/1/20 23:33
 **/
@Slf4j
@Component("del_external_contact")
public class WeCallBackDelExternalContactImpl extends WeEventStrategy {

    @Autowired
    private IWeCustomerService weCustomerService;

    @Autowired
    private IWeCustomerTrajectoryService iWeCustomerTrajectoryService;

    @Override
    public void eventHandle(WeBackBaseVo message) {
        WeBackCustomerVo customerInfo = (WeBackCustomerVo) message;

        weCustomerService.remove(new LambdaQueryWrapper<WeCustomer>()
                .eq(WeCustomer::getExternalUserid,customerInfo.getExternalUserID())
                .eq(WeCustomer::getAddUserId,customerInfo.getUserID()));
        //添加跟进动态
        iWeCustomerTrajectoryService.createAddOrRemoveTrajectory(customerInfo.getExternalUserID(),customerInfo.getUserID(),false,
                false);
    }
}